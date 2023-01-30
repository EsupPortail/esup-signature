package org.esupportail.esupsignature.service.utils.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDNamedDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.Attribute;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpSerializer;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.pdf.PdfConfig;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;


@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    @Resource
    private PdfConfig pdfConfig;

    @Resource
    private FileService fileService;

    private final GlobalProperties globalProperties;

    @Resource
    private LogService logService;

    @Resource
    private ValidationService validationService;

    public PdfService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public byte[] stampImage(byte[] inputStream, SignRequest signRequest, SignRequestParams signRequestParams, int j, User user, Date date) {
        double fixFactor = .75;
        SignType signType = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        PdfParameters pdfParameters;
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdfParameters = getPdfParameters(pdDocument, signRequestParams.getSignPageNumber());
            if(signRequestParams.getAllPages() != null && signRequestParams.getAllPages()) {
                int i = 1;
                for(PDPage pdPage : pdDocument.getPages()) {
                    if(i != signRequestParams.getSignPageNumber() || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.pdfImageStamp)) {
                        stampImageToPage(signRequest, signRequestParams, user, fixFactor, signType, pdfParameters, pdDocument, pdPage, i, date);
                    }
                    i++;
                }
            } else {
                if(j > 0) {
                    PDPage pdPage = pdDocument.getPage(signRequestParams.getSignPageNumber() - 1);
                    stampImageToPage(signRequest, signRequestParams, user, fixFactor, signType, pdfParameters, pdDocument, pdPage, signRequestParams.getSignPageNumber(), date);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.save(out);
            byte[] in = out.toByteArray();
            pdDocument.close();
            return in;
        } catch (IOException e) {
            logger.error("error to add image", e);
        }
        return null;
    }

    private void stampImageToPage(SignRequest signRequest, SignRequestParams signRequestParams, User user, double fixFactor, SignType signType, PdfParameters pdfParameters, PDDocument pdDocument, PDPage pdPage, int pageNumber, Date newDate) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);
        InputStream signImage = null;
        if (signRequestParams.getSignImageNumber() < 0) {
            signImage = fileService.getFaImageByIndex(signRequestParams.getSignImageNumber());
        } else {
            if (signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) || !signRequestParams.getAddImage()) {
                signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname()), signRequestParams, signType, user, newDate, fixFactor);
            } else if (signRequestParams.getAddExtra()) {
                if(signRequestParams.getSignImageNumber() == null || signRequestParams.getSignImageNumber() >= user.getSignImages().size()) {
                    signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname()), signRequestParams, signType, user, newDate, fixFactor);
                } else {
                    signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), signRequestParams, signType, user, newDate, fixFactor);
                }
            } else if (signRequestParams.getTextPart() == null) {
                if(user.getSignImages().size() >= signRequestParams.getSignImageNumber() + 1) {
                    signImage = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
                } else {
                    signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname()), signRequestParams, signType, user, newDate, fixFactor);
                }
            }
            if (signRequestParams.getAddWatermark()) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                fileService.addImageWatermark(new ClassPathResource("/static/images/watermark.png").getInputStream(), signImage, outputStream, new Color(137, 137, 137), signRequestParams.getExtraOnTop());
                signImage = new ByteArrayInputStream(outputStream.toByteArray());
            }
        }

        float tx = 0;
        float ty = 0;
        float xAdjusted = (float) (signRequestParams.getxPos() * fixFactor);
        float yAdjusted;

        if (pdfParameters.getRotation() == 0 || pdfParameters.getRotation() == 180) {
            yAdjusted = (float) (pdfParameters.getHeight() - signRequestParams.getyPos() * fixFactor - signRequestParams.getSignHeight() * fixFactor + pdPage.getCropBox().getLowerLeftY());
        } else {
            yAdjusted = (float) (pdfParameters.getWidth() - signRequestParams.getyPos() * fixFactor - signRequestParams.getSignHeight() * fixFactor + pdPage.getCropBox().getLowerLeftY());
        }

        if (pdfParameters.isLandScape()) {
            tx = pdfParameters.getWidth();
        } else {
            ty = pdfParameters.getHeight();
        }

        PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);

        if (pdfParameters.getRotation() != 0 && pdfParameters.getRotation() != 360) {
            contentStream.transform(Matrix.getRotateInstance(Math.toRadians(pdfParameters.getRotation()), tx, ty));
        }
        if (signImage != null) {
            logger.info("stamp image to " + Math.round(xAdjusted) + ", " + Math.round(yAdjusted) + " on page : " + pageNumber);
            BufferedImage bufferedSignImage = ImageIO.read(signImage);
            fileService.changeColor(bufferedSignImage, 0, 0, 0, signRequestParams.getRed(), signRequestParams.getGreen(), signRequestParams.getBlue());
            ByteArrayOutputStream signImageByteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedSignImage, "png", signImageByteArrayOutputStream);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, signImageByteArrayOutputStream.toByteArray(), "sign.png");
            contentStream.drawImage(pdImage, xAdjusted, yAdjusted, (float) (signRequestParams.getSignWidth() * fixFactor), (float) (signRequestParams.getSignHeight() * fixFactor));
            if (signRequestParams.getSignImageNumber() >= 0) {
                addLink(signRequest, signRequestParams, user, fixFactor, pdDocument, pdPage, newDate, dateFormat, xAdjusted, yAdjusted);
            }
        } else if (StringUtils.hasText(signRequestParams.getTextPart())) {
            int fontSize = (int) (signRequestParams.getFontSize() * signRequestParams.getSignScale() * .75);
            PDFont pdFont = PDTrueTypeFont.load(pdDocument, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream(), WinAnsiEncoding.INSTANCE);
            contentStream.beginText();
            contentStream.setFont(pdFont, fontSize);
            contentStream.newLineAtOffset(xAdjusted, (float) (yAdjusted + signRequestParams.getSignHeight() * .75 - fontSize));
            String[] lines = signRequestParams.getTextPart().split("\n");
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -fontSize / .75f);
            }
            contentStream.endText();
        }
        contentStream.close();
    }

    private void addLink(SignRequest signRequest, SignRequestParams signRequestParams, User user, double fixFactor, PDDocument pdDocument, PDPage pdPage, Date newDate, DateFormat dateFormat, float xAdjusted, float yAdjusted) throws IOException {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        String signatureInfos =
                "Signature calligraphique" + pdfTextStripper.getLineSeparator() +
                        "De : " + user.getFirstname() + " " + user.getName() + pdfTextStripper.getLineSeparator() +
                        "Le : " +  dateFormat.format(newDate) + pdfTextStripper.getLineSeparator() +
                        "Depuis : " + logService.getIp() + pdfTextStripper.getLineSeparator() +
                        "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                        globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();

        PDAnnotationLink pdAnnotationLink = new PDAnnotationLink();
        PDRectangle position = new PDRectangle(xAdjusted, yAdjusted, (float) (signRequestParams.getSignWidth() * fixFactor), (float) (signRequestParams.getSignHeight() * fixFactor));
        pdAnnotationLink.setRectangle(position);
        PDBorderStyleDictionary pdBorderStyleDictionary = new PDBorderStyleDictionary();
        pdBorderStyleDictionary.setStyle(PDBorderStyleDictionary.STYLE_INSET);
        pdAnnotationLink.setBorderStyle(pdBorderStyleDictionary);
        Color color = new Color(255, 255, 255);
        float[] components = new float[] {
                color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f };
        PDColor pdColor = new PDColor(components, PDDeviceRGB.INSTANCE);
        pdAnnotationLink.setColor(pdColor);
        PDActionURI action = new PDActionURI();
        action.setURI(globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken());
        pdAnnotationLink.setAction(action);
        pdAnnotationLink.setPage(pdPage);
        pdAnnotationLink.setQuadPoints(new float[0]);
        pdAnnotationLink.setContents(signatureInfos);
        pdPage.getAnnotations().add(pdAnnotationLink);

        if(pdDocument.getDocumentCatalog().getDocumentOutline() == null) {
            PDDocumentOutline outline = new PDDocumentOutline();
            pdDocument.getDocumentCatalog().setDocumentOutline(outline);
        }
        PDOutlineItem pdOutlineItem = new PDOutlineItem();
        pdOutlineItem.setDestination(pdAnnotationLink.getDestination());
        pdOutlineItem.setTitle(signatureInfos);
        pdDocument.getDocumentCatalog().getDocumentOutline().addLast(pdOutlineItem);
    }

    public ByteArrayOutputStream createQR(String data) throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                new String(data.getBytes("UTF-8"), "UTF-8"),
                BarcodeFormat.DATA_MATRIX, 500, 500);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "png", outputStream);
        return outputStream;
    }

    public InputStream addQrCode(SignRequest signRequest, InputStream inputStream) throws IOException, WriterException {
        PDDocument pdDocument = PDDocument.load(inputStream);
        for(int i = 0; i < pdDocument.getNumberOfPages(); i++) {
            PDPage pdPage = pdDocument.getPage(i);
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            String signatureInfos = "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                    globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();
            PDAnnotationLink pdAnnotationLink = new PDAnnotationLink();
            PDRectangle position = new PDRectangle(pdPage.getMediaBox().getWidth() - 80, 30, 30, 30);
            pdAnnotationLink.setRectangle(position);
            PDBorderStyleDictionary pdBorderStyleDictionary = new PDBorderStyleDictionary();
            pdBorderStyleDictionary.setStyle(PDBorderStyleDictionary.STYLE_INSET);
            pdAnnotationLink.setBorderStyle(pdBorderStyleDictionary);
            Color color = new Color(255, 255, 255);
            float[] components = new float[]{
                    color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f};
            PDColor pdColor = new PDColor(components, PDDeviceRGB.INSTANCE);
            pdAnnotationLink.setColor(pdColor);
            PDActionURI action = new PDActionURI();
            String url = globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();
            action.setURI(url);
            pdAnnotationLink.setAction(action);
            pdAnnotationLink.setPage(pdPage);
            pdAnnotationLink.setQuadPoints(new float[0]);
            pdAnnotationLink.setContents(signatureInfos);
            pdPage.getAnnotations().add(pdAnnotationLink);

            if (pdDocument.getDocumentCatalog().getDocumentOutline() == null) {
                PDDocumentOutline outline = new PDDocumentOutline();
                pdDocument.getDocumentCatalog().setDocumentOutline(outline);
            }
            PDOutlineItem pdOutlineItem = new PDOutlineItem();
            pdOutlineItem.setDestination(pdAnnotationLink.getDestination());
            pdOutlineItem.setTitle(signatureInfos);
            pdDocument.getDocumentCatalog().getDocumentOutline().addLast(pdOutlineItem);
            ByteArrayOutputStream outputStream = createQR(url);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, outputStream.toByteArray(), "QRCode check");
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);
            contentStream.drawImage(pdImage, pdPage.getMediaBox().getWidth() - 50, 30, 30, 30);
            contentStream.close();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdDocument.save(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        pdDocument.close();
        return in;
    }

    public byte[] addOutLine(SignRequest signRequest, byte[] inputStream, User user, Date newDate, DateFormat dateFormat) throws IOException {
        PDDocument pdDocument = PDDocument.load(inputStream);
        if(pdDocument.getDocumentCatalog().getDocumentOutline() == null) {
            PDDocumentOutline outline = new PDDocumentOutline();
            pdDocument.getDocumentCatalog().setDocumentOutline(outline);
        }
        PDFTextStripper pdfTextStripper = new PDFTextStripper();
        String signatureInfos =
                "Signature calligraphique" + pdfTextStripper.getLineSeparator() +
                        "De : " + user.getFirstname() + " " + user.getName() + pdfTextStripper.getLineSeparator() +
                        "Le : " +  dateFormat.format(newDate) + pdfTextStripper.getLineSeparator() +
                        "Depuis : " + logService.getIp() + pdfTextStripper.getLineSeparator() +
                        "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                        globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();
        PDOutlineItem pdOutlineItem = new PDOutlineItem();
        pdOutlineItem.setTitle(signatureInfos);
        PDNamedDestination dest = new PDNamedDestination();
        dest.setNamedDestination(globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken());
        pdOutlineItem.setDestination(dest);
        pdDocument.getDocumentCatalog().getDocumentOutline().addLast(pdOutlineItem);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdDocument.save(out);
        byte[] in = out.toByteArray();
        pdDocument.close();
        return in;
    }


    public Map<String, String> readMetadatas(InputStream inputStream) {
        Map<String, String> metadatas = new HashMap<>();
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            PDDocumentInformation info = pdDocument.getDocumentInformation();
            if(checkMetadataKeys(info.getMetadataKeys())) {
                for (String metaName : info.getMetadataKeys()) {
                    metadatas.put(metaName, info.getPropertyStringValue(metaName).toString());
                }
            } else {
                try {
                    PDMetadata metadata = new PDMetadata(pdDocument);
                    DomXmpParser domXmpParser = new DomXmpParser();
                    XMPMetadata xmpMetadata = domXmpParser.parse(metadata.exportXMPMetadata());
                    if(checkMetadataKeys(xmpMetadata.getDublinCoreSchema().getAllAttributes())) {
                        setMetadatasAttributes(metadatas, xmpMetadata);
                    }
                } catch (Exception e) {
                    logger.warn("error on search metadatas : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("error on write metadatas", e);
        }
        return metadatas;
    }

    private void setMetadatasAttributes(Map<String, String> metadatas, XMPMetadata xmpMetadata) {
        for(Attribute attribute : xmpMetadata.getDublinCoreSchema().getAllAttributes()) {
            metadatas.put(attribute.getName(), attribute.getValue());
        }
    }

    public boolean checkMetadataKeys(Set<String> keys) {
        for(String key : keys) {
            if(key.startsWith("sign_")) {
                return true;
            }
        }
        return false;
    }

    public boolean checkMetadataKeys(List<Attribute> attributes) {
        for(Attribute attribute : attributes) {
            if(attribute.getName().startsWith("sign_")) {
                return true;
            }
        }
        return false;
    }

    public byte[] writeMetadatas(byte[] inputStream, String fileName, SignRequest signRequest, List<Log> additionnalLogs) {

        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.setVersion(1.7f);
            COSDictionary trailer = pdDocument.getDocument().getTrailer();
            COSDictionary rootDictionary = trailer.getCOSDictionary(COSName.ROOT);
            rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
            rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.7"));

            String producer = "esup-signature v." + globalProperties.getVersion();

            PDDocumentInformation info = pdDocument.getDocumentInformation();
            info.setTitle(signRequest.getTitle() + globalProperties.getSignedSuffix());
            info.setSubject(fileName);
            info.setCreator(signRequest.getCreateBy().getEppn());
            info.setProducer(producer);
            if (info.getCreationDate() == null) {
                info.setCreationDate(Calendar.getInstance());
            }
            info.setModificationDate(Calendar.getInstance());

            PDDocumentCatalog cat = pdDocument.getDocumentCatalog();

            XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();

            AdobePDFSchema pdfSchema = xmpMetadata.createAndAddAdobePDFSchema();
            pdfSchema.setProducer(info.getProducer());

            DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
            dublinCoreSchema.setTitle(info.getTitle());
            dublinCoreSchema.setDescription(info.getSubject());
            dublinCoreSchema.addCreator(info.getCreator());

            XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
            xmpBasicSchema.setCreatorTool(info.getProducer());
            xmpBasicSchema.setCreateDate(info.getCreationDate());
            xmpBasicSchema.setModifyDate(info.getModificationDate());
            xmpBasicSchema.addIdentifier(signRequest.getToken());
            xmpBasicSchema.addIdentifier(globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken());

            PDFAIdentificationSchema pdfaIdentificationSchema = xmpMetadata.createAndAddPFAIdentificationSchema();
            pdfaIdentificationSchema.setConformance("B");
            pdfaIdentificationSchema.setPart(2);

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);

            List<Log> logs = new ArrayList<>();
            logs.addAll(additionnalLogs);
            logs.addAll(logService.getSignLogs(signRequest.getId()));
            int i = 0;
            for(Log log : logs) {
                i++;
                String signatureInfos =
                    pdfTextStripper.getLineSeparator() + log.getAction() + pdfTextStripper.getLineSeparator() +
                    "De : " + log.getUser().getFirstname() + " " + log.getUser().getName() + pdfTextStripper.getLineSeparator() +
                    "Le : " + dateFormat.format(log.getLogDate()) + pdfTextStripper.getLineSeparator() +
                    "Depuis : " + log.getIp() + pdfTextStripper.getLineSeparator() +
                    "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                    globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();
                info.setKeywords(info.getKeywords() + ", " + signatureInfos);
                info.setCustomMetadataValue("Signature_1" + i, signatureInfos);
                pdfaIdentificationSchema.setTextPropertyValue("Signature_" + i, signatureInfos);
                xmpBasicSchema.setTextPropertyValue("Signature_" + i, signatureInfos);
                dublinCoreSchema.setTextPropertyValue("Signature_" + i, signatureInfos);
                pdfSchema.setTextPropertyValue("Signature_" + i, signatureInfos);

            }

            if(info.getKeywords() != null) {
                pdfSchema.setKeywords(info.getKeywords());
            }

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmpMetadata, baos, true);

            PDMetadata metadata = new PDMetadata(pdDocument);
            metadata.importXMPMetadata(baos.toByteArray());
            cat.setMetadata(metadata);
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            for(PDPage pdPage : pdDocument.getPages()) {
                for(PDAnnotation pdAnnotation : pdPage.getAnnotations()) {
                    pdAnnotation.setPrinted(true);
                }
            }
            pdDocument.setDocumentInformation(info);
            pdDocument.save(out);
            pdDocument.close();
            return out.toByteArray();
        } catch (IOException | BadFieldValueException | TransformerException e) {
            logger.error("error on write metadatas", e);
        }
        return inputStream;
    }

    public byte[] convertGS(byte[] originalBytes) throws IOException, EsupSignatureException {
        if (!isPdfAComplient(originalBytes) && pdfConfig.getPdfProperties().isConvertToPdfA()) {
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -sstdout=%stderr -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dNOPAUSE -dNOSAFER -dBATCH -sFONTPATH=" + pdfConfig.getPdfProperties().getPathToFonts() + " " + pdfConfig.getPdfProperties().getGsCommandParams() + " -sOutputFile=- '" + pdfConfig.getPdfADefPath() + "' - 2>/dev/null";
            logger.info("GhostScript PDF/A convertion : " + cmd);
            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }
            ByteArrayOutputStream convertedOutputStream = new ByteArrayOutputStream();
            byte[] result;
            try {
                Process process = processBuilder.start();
                IOUtils.copy(new ByteArrayInputStream(originalBytes), process.getOutputStream());
                process.getOutputStream().flush();
                process.getOutputStream().close();
                process.getInputStream().transferTo(convertedOutputStream);
                result = convertedOutputStream.toByteArray();
                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.info("Convert success");
                } else {
                    logger.warn("Convert fail");
                    logger.warn(cmd);
                    StringBuilder output = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    logger.warn(output.toString());
                    logger.warn("PDF/A convertion failure : document will be signed without convertion");
                    process.destroy();
                    return originalBytes;
                }
            } catch (InterruptedException e) {
                logger.error("GhostScript launch error : check installation or path", e);
                throw new EsupSignatureSignException("GhostScript launch error");
            }
            return result;
        } else {
            return originalBytes;
        }
    }

    public byte[] normalizeGS(byte[] originalBytes) throws IOException, EsupSignatureException {
        Reports reports = validationService.validate(new ByteArrayInputStream(originalBytes), null);
        if (!isAcroForm(new ByteArrayInputStream(originalBytes)) && (reports == null || reports.getSimpleReport() == null || reports.getSimpleReport().getSignatureIdList().size() == 0)) {
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -sstdout=%stderr -dBATCH -dNOPAUSE -dPassThroughJPEGImages=true -dNOSAFER -sDEVICE=pdfwrite -d -sOutputFile=- - 2>/dev/null";
            logger.info("GhostScript PDF/A conversion : " + cmd);
            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }
            byte[] result;
            try {
                Process process = processBuilder.start();
                process.getOutputStream().write(originalBytes);
                process.getOutputStream().flush();
                process.getOutputStream().close();
                result = process.getInputStream().readAllBytes();
                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.info("Convert success");
                } else {
                    logger.warn("Convert fail");
                    logger.warn(cmd);
                    StringBuilder output = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(result)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line + "\n");
                    }
                    logger.warn(output.toString());
                    throw new EsupSignatureSignException("PDF/A convertion failure");
                }
            } catch (InterruptedException e) {
                logger.error("GhostScript launcs error : check installation or path", e);
                throw new EsupSignatureSignException("GhostScript launch error");
            }
            return result;
        } else {
            return originalBytes;
        }
    }

    public boolean isPdfAComplient(byte[] pdfFile) throws EsupSignatureException {
        List<String> result = checkPDFA(pdfFile, false);
        if (result.size() > 0 && "success".equals(result.get(0))) {
            return true;
        }
        return false;
    }

    public List<String> checkPDFA(byte[] pdfFile, boolean fillResults) throws EsupSignatureException {
        List<String> result = new ArrayList<>();
        VeraGreenfieldFoundryProvider.initialise();
        try {
            PDFAParser parser = Foundries.defaultInstance().createParser(new ByteArrayInputStream(pdfFile));
            PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
            ValidationResult validationResult = validator.validate(parser);
            if (validationResult.isCompliant()) {
                result.add("success");
                result.add("File is compliant with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
            } else {
                result.add("danger");
                result.add("File is not compliant with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
                if (fillResults) {
                    for (TestAssertion test : validationResult.getTestAssertions()) {
                        result.add(test.getRuleId().getClause() + " : " + test.getMessage());
                    }
                }
            }
            validator.close();
            parser.close();
        } catch (Exception e) {
            logger.warn("check error " + e.getMessage());
            logger.debug("check error", e);
        }
        return result;
    }

    public byte[] fill(InputStream pdfFile, Map<String, String> datas, boolean isLastStep) {
        try {
            PDDocument pdDocument = PDDocument.load(pdfFile);
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                byte[] ttfBytes = new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream().readAllBytes();
                PDFont pdFont = PDTrueTypeFont.load(pdDocument, new ByteArrayInputStream(ttfBytes), WinAnsiEncoding.INSTANCE);
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("LiberationSans"), pdFont);
                pdAcroForm.setDefaultResources(resources);
                pdAcroForm.setDefaultAppearance("/LiberationSans 10 Tf 0 g");
                List<PDField> fields = pdAcroForm.getFields();
                for(PDField pdField : fields) {
                    for(PDAnnotationWidget pdAnnotationWidget : pdField.getWidgets()) {
                        pdAnnotationWidget.getCOSObject().setString(COSName.DA, "/LiberationSans 10 Tf 0 g");
                    }
                    String filedName = pdField.getPartialName().split("\\$|#|!")[0];
                    if(datas.containsKey(filedName)) {
                        if (pdField instanceof PDCheckBox) {
                            if (datas.get(filedName) != null && datas.get(filedName).equals("on")) {
                                ((PDCheckBox) pdField).check();
                            }
                        } else if (pdField instanceof PDRadioButton) {
                            PDRadioButton pdRadioButton = (PDRadioButton) pdField;
                            try {
                                String value = datas.get(filedName);
                                if(value.isEmpty()) {
                                    value = "Off";
                                }
                                pdRadioButton.setValue(value);
                            } catch (NullPointerException | IllegalArgumentException e) {
                                logger.debug("radio value error", e);
                            }
                        } else if (pdField instanceof PDListBox) {
                            String value = datas.get(filedName);
                            PDListBox pdListBox = (PDListBox) pdField;
                            pdListBox.setValue(value);
                            if(isLastStep) {
                                PDTextField pdTextField = new PDTextField(pdAcroForm);
                                pdTextField.setPartialName(pdListBox.getPartialName());
                                PDAnnotationWidget pdAnnotationWidget = new PDAnnotationWidget();
                                PDAnnotationWidget oldWidget = pdListBox.getWidgets().get(0);
                                pdAnnotationWidget.setAppearance(oldWidget.getAppearance());
                                pdAnnotationWidget.setPage(oldWidget.getPage());
                                pdAnnotationWidget.setAppearanceCharacteristics(oldWidget.getAppearanceCharacteristics());
                                pdAnnotationWidget.setRectangle(oldWidget.getRectangle());
                                pdAnnotationWidget.setPrinted(true);
                                pdTextField.setWidgets(Collections.singletonList(pdAnnotationWidget));
                                pdTextField.getCOSObject().setNeedToBeUpdated(true);
                                pdTextField.getCOSObject().removeItem(COSName.AA);
                                pdTextField.getCOSObject().removeItem(COSName.AP);
                                pdTextField.getCOSObject().setString(COSName.DA, "/LiberationSans 10 Tf 0 g");
                                pdTextField.setValue(value);
                                pdAcroForm.getFields().add(pdTextField);
                                pdAcroForm.getFields().remove(pdListBox);
                                Map<String, Integer> pageNrByAnnotDict = getPageNumberByAnnotDict(pdDocument);
                                int page = pageNrByAnnotDict.get(pdField.getPartialName());
                                int countPage = 1;
                                for (PDPage pdPage : pdDocument.getPages()) {
                                    pdPage.getAnnotations().removeAll(pdListBox.getWidgets());
                                    if(countPage == page) {
                                        pdPage.getAnnotations().add(pdAnnotationWidget);
                                    }
                                    countPage++;
                                }
                            }
                        } else {
                            if (!(pdField instanceof PDSignatureField)) {
                                String value = datas.get(filedName);
                                pdField.getCOSObject().setNeedToBeUpdated(true);
                                pdField.getCOSObject().removeItem(COSName.AA);
                                pdField.getCOSObject().removeItem(COSName.AP);
                                pdField.getCOSObject().setString(COSName.DA, "/LiberationSans 10 Tf 0 g");
                                pdField.setValue(value);
                            }
                        }
                    }
                    if (!pdField.isReadOnly()) {
                        pdField.setReadOnly(true);
                    } else {
                        pdField.setReadOnly(false);
                        pdField.setRequired(true);
                    }

                }
                if(isLastStep) {
                    pdAcroForm.flatten();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.save(out);
            pdDocument.close();
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("file read error", e);
        }
        return null;
    }

    public PDFieldTree getFields(PDDocument pdDocument) throws EsupSignatureException {
        try {
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                PDFieldTree fields = new PDFieldTree(pdAcroForm);
                pdDocument.close();
                return fields;
            }
        } catch (IOException e) {
            logger.error("file read error", e);
        }
        return null;
    }

    public InputStream removeSignField(InputStream pdfFile) {
        try {
            PDDocument pdDocument = PDDocument.load(pdfFile);
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                PDFont pdFont = PDTrueTypeFont.load(pdDocument, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream(), WinAnsiEncoding.INSTANCE);
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("LiberationSans"), pdFont);
                pdAcroForm.setDefaultResources(resources);
                List<PDField> fields = pdAcroForm.getFields();
                for(PDField pdField : fields) {
                    if(pdField instanceof PDSignatureField) {
                        List<PDAnnotationWidget> widgets = pdField.getWidgets();
                        for (PDAnnotationWidget widget : widgets) {
                            for(PDPage page : pdDocument.getPages()) {
                                List<PDAnnotation> annotations = page.getAnnotations();
                                boolean removed = false;
                                for (PDAnnotation annotation : annotations) {
                                    if (annotation.getCOSObject().equals(widget.getCOSObject())) {
                                        removed = annotations.remove(annotation);
                                        break;
                                    }
                                }
                                if (!removed)
                                    System.out.println("Inconsistent annotation definition: Page annotations do not include the target widget.");
                            }
                        }
                        PDSignatureField pdSignatureField = (PDSignatureField) pdField;
                        pdAcroForm.getFields().remove(pdSignatureField);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.save(out);
            pdDocument.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            logger.error("file read error", e);
        }
        return null;
    }

    public PdfParameters getPdfParameters(InputStream pdfFile, int pageNumber) {
        PDDocument pdDocument = null;
        try {
            pdDocument = PDDocument.load(pdfFile);
            return getPdfParameters(pdDocument, pageNumber);
        } catch (Exception e) {
            logger.error("error on get pdf parameters", e);
        } finally {
            if (pdDocument != null) {
                try {
                    pdDocument.close();
                } catch (IOException e) {
                    logger.error("unable to close document", e);
                }
            }
        }
        return null;
    }

    public PdfParameters getPdfParameters(PDDocument pdDocument, int pageNumber) {
        PDPage pdPage = pdDocument.getPage(pageNumber - 1);
        PdfParameters pdfParameters = new PdfParameters(
                (int) pdPage.getMediaBox().getWidth(),
                (int) pdPage.getMediaBox().getHeight(),
                pdPage.getRotation(),
                pdDocument.getNumberOfPages());
        return pdfParameters;
    }

    public BufferedImage pageAsBufferedImage(InputStream pdfFile, int page) {
        BufferedImage bufferedImage = null;
        PDDocument pdDocument = null;
        try {
            pdDocument = PDDocument.load(pdfFile);
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            bufferedImage = pdfRenderer.renderImageWithDPI(page, pdfConfig.getPdfProperties().getPdfToImageDpi(), ImageType.RGB);
        } catch (IOException e) {
            logger.error("error on convert page to image", e);
        } finally {
            if (pdDocument != null) {
                try {
                    pdDocument.close();
                } catch (IOException e) {
                    logger.error("unable to close document", e);
                }
            }
        }
        return bufferedImage;
    }

    private InputStream bufferedImageToInputStream(BufferedImage image, String type) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, type, os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public InputStream pageAsInputStream(InputStream pdfFile, int page) throws Exception {
        BufferedImage bufferedImage = pageAsBufferedImage(pdfFile, page);
        InputStream inputStream = bufferedImageToInputStream(bufferedImage, "png");
        bufferedImage.flush();
        return inputStream;
    }

    public InputStream jpegToPdf(InputStream inputStream, String name) throws IOException {
        PDDocument pdDocument = new PDDocument();
        byte[] imageBytes = inputStream.readAllBytes();
        BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(imageBytes));
        float width = bimg.getWidth();
        float height = bimg.getHeight();
        PDPage page = new PDPage(new PDRectangle(width, height));
        pdDocument.addPage(page);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, imageBytes, name);
        PDPageContentStream contentStream = new PDPageContentStream(pdDocument, page);
        contentStream.drawImage(pdImage, 0, 0);
        contentStream.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        pdDocument.save(out);
        pdDocument.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    int determineSafe(PDDocument document, PDAnnotationWidget widget) throws IOException {
        COSDictionary widgetObject = widget.getCOSObject();
        PDPageTree pages = document.getDocumentCatalog().getPages();
        for (int i = 0; i < pages.getCount(); i++) {
            for (PDAnnotation annotation : pages.get(i).getAnnotations()) {
                COSDictionary annotationObject = annotation.getCOSObject();
                if (annotationObject.equals(widgetObject)) {
                    return i;
                }
            }
        }
        return -1;
    }

    int determineFast(PDDocument document, PDAnnotationWidget widget)
    {
        PDPage page = widget.getPage();
        return page != null ? document.getPages().indexOf(page) : -1;
    }

    public Map<String, Integer> getPageNumberByAnnotDict(PDDocument pdDocument) throws IOException {
        Iterator<PDPage> pages = pdDocument.getDocumentCatalog().getPages().iterator();
        PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
        Map<String, Integer> pageNrByAnnotDict = new HashMap<>();
        for (PDField field : pdAcroForm.getFieldTree()) {
            for (PDAnnotationWidget widget : field.getWidgets()) {
                int pageNb = determineFast(pdDocument, widget);
                if(pageNb == -1) {
                    pageNb = determineSafe(pdDocument, widget);
                }
                if(pageNb > -1) {
                    pageNrByAnnotDict.put(field.getPartialName(), pageNb);
                }
            }
        }
//
//        Map<COSDictionary, Integer> pageNrByAnnotDict = new HashMap<>();
//        int i = 0;
//        for (Iterator<PDPage> it = pages; it.hasNext(); ) {
//            PDPage pdPage = it.next();
//            for (PDAnnotation annotation : pdPage.getAnnotations()) {
//                pageNrByAnnotDict.put(annotation.getCOSObject(), i + 1);
//            }
//            i++;
//        }
        return pageNrByAnnotDict;
    }

    public boolean isAcroForm(ByteArrayInputStream byteArrayInputStream) throws IOException {
        PDDocument pdDocument = PDDocument.load(byteArrayInputStream);
        PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
        if(pdAcroForm != null && pdAcroForm.getFields().size() > 0) {
            return true;
        }
        return false;
    }

//    public InputStream convertDocToPDF(InputStream doc) {
//        try {
//            Docx2PDFViaDocx4jConverter docx2PDFViaDocx4jConverter = Docx2PDFViaDocx4jConverter.getInstance();
//            docx2PDFViaDocx4jConverter.toPdfSettings(Options.getFrom( "DOCX" ));
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            docx2PDFViaDocx4jConverter.convert(doc, byteArrayOutputStream, Options.getFrom( "DOCX" ));
//            return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
//        } catch (XDocConverterException e) {
//            logger.error(e.getMessage(), e);
//        }
//        return doc;
//    }

}
