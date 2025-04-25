package org.esupportail.esupsignature.service.utils.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.exception.ProtectedDocumentException;
import eu.europa.esig.dss.pdf.PdfPermissionsChecker;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxDocumentReader;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.Resource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
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
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

    public byte[] stampImage(byte[] inputStream, SignRequest signRequest, SignRequestParams signRequestParams, int j, User user, Date date, Boolean otp, Boolean endingWithCert) {
        SignType signType = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        PdfParameters pdfParameters;
        try {
            PDDocument pdDocument = Loader.loadPDF(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdfParameters = getPdfParameters(pdDocument, signRequestParams.getSignPageNumber());
            if(signRequestParams.getAllPages() != null && signRequestParams.getAllPages() && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign()) {
                int i = 1;
                for(PDPage pdPage : pdDocument.getPages()) {
                    if(i != signRequestParams.getSignPageNumber() || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.pdfImageStamp)) {
                        stampImageToPage(signRequest, signRequestParams, user, signType, pdfParameters, pdDocument, pdPage, i, date, otp, endingWithCert);
                    }
                    i++;
                }
            } else {
                if(j > 0) {
                    PDPage pdPage = pdDocument.getPage(signRequestParams.getSignPageNumber() - 1);
                    stampImageToPage(signRequest, signRequestParams, user, signType, pdfParameters, pdDocument, pdPage, signRequestParams.getSignPageNumber(), date, otp, endingWithCert);
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

    private void stampImageToPage(SignRequest signRequest, SignRequestParams signRequestParams, User user, SignType signType, PdfParameters pdfParameters, PDDocument pdDocument, PDPage pdPage, int pageNumber, Date newDate, Boolean otp, Boolean endingWithCert) throws IOException {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);
        InputStream signImage = null;
        if (signRequestParams.getSignImageNumber() < 0) {
            signImage = fileService.getFaImageByIndex(signRequestParams.getSignImageNumber());
        } else {
            if ((signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) || !signRequestParams.getAddImage())
                    && (!StringUtils.hasText(signRequestParams.getTextPart()) || signRequestParams.getAddExtra()) ) {
                signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname(), user.getEmail(), true), signRequestParams, signType, user, newDate, otp);
            } else if (signRequestParams.getAddExtra()) {
                if(signRequestParams.getSignImageNumber() == null || signRequestParams.getSignImageNumber() >= user.getSignImages().size()) {
                    if(signRequestParams.getSignImageNumber() >= user.getSignImages().size() + 1 && signRequestParams.getSignImageNumber() != 999998) {
                        signImage = fileService.addTextToImage(fileService.getDefaultParaphe(user.getName(), user.getFirstname(), user.getEmail(), true), signRequestParams, signType, user, newDate, otp);
                    } else {
                        signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname(), user.getEmail(), true), signRequestParams, signType, user, newDate, otp);
                    }
                } else {
                    signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), signRequestParams, signType, user, newDate, otp);
                }
            } else if (signRequestParams.getTextPart() == null) {
                if(user.getSignImages().size() >= signRequestParams.getSignImageNumber() + 1) {
                    signImage = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
                } else {
                    if(signRequestParams.getSignImageNumber() >= user.getSignImages().size() + 1 && signRequestParams.getSignImageNumber() != 999998) {
                        signImage = fileService.addTextToImage(fileService.getDefaultParaphe(user.getName(), user.getFirstname(), user.getEmail(), true), signRequestParams, signType, user, newDate, otp);
                    } else {
                        signImage = fileService.addTextToImage(fileService.getDefaultImage(user.getName(), user.getFirstname(), user.getEmail(), true), signRequestParams, signType, user, newDate, otp);
                    }
                }
            }
            if (BooleanUtils.isTrue(signRequestParams.getAddWatermark())) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                fileService.addImageWatermark(new ClassPathResource("/static/images/watermark.png").getInputStream(), signImage, outputStream, signRequestParams.getExtraOnTop());
                signImage = new ByteArrayInputStream(outputStream.toByteArray());
            }
        }

        float tx = 0;
        float ty = 0;
        Float fixFactor = globalProperties.getFixFactor();
        float xAdjusted = signRequestParams.getxPos() * fixFactor;
        float yAdjusted;

        if (pdfParameters.getRotation() == 0 || pdfParameters.getRotation() == 180) {
            yAdjusted = pdfParameters.getHeight() - signRequestParams.getyPos() * fixFactor - signRequestParams.getSignHeight() * fixFactor + pdPage.getCropBox().getLowerLeftY();
            if (pdfParameters.isLandScape()) {
                tx = pdfParameters.getWidth();
            } else {
                ty = pdfParameters.getHeight();
            }
        } else {
            yAdjusted = pdfParameters.getWidth() - signRequestParams.getyPos() * fixFactor - signRequestParams.getSignHeight() * fixFactor + pdPage.getCropBox().getLowerLeftY();
            if (pdfParameters.isLandScape()) {
                ty = pdfParameters.getHeight();
            } else {
                tx = pdfParameters.getWidth();
            }
        }

        PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);
        Matrix rotation = null;
        if (pdfParameters.getRotation() != 0 && pdfParameters.getRotation() != 360) {
            rotation = Matrix.getRotateInstance(Math.toRadians(pdfParameters.getRotation()), tx, ty);
            contentStream.transform(rotation);
        }
        if (signImage != null) {
            logger.info("stamp image to " + Math.round(xAdjusted) + ", " + Math.round(yAdjusted) + " on page : " + pageNumber);
            BufferedImage bufferedSignImage = ImageIO.read(signImage);
            fileService.changeColor(bufferedSignImage, 0, 0, 0, signRequestParams.getRed(), signRequestParams.getGreen(), signRequestParams.getBlue());
            ByteArrayOutputStream signImageByteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedSignImage, "png", signImageByteArrayOutputStream);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, signImageByteArrayOutputStream.toByteArray(), "sign.png");
            contentStream.drawImage(pdImage, xAdjusted, yAdjusted, signRequestParams.getSignWidth() * fixFactor, signRequestParams.getSignHeight() * fixFactor);
            if (signRequestParams.getSignImageNumber() >= 0 && !endingWithCert) {
                addLink(signRequest, signRequestParams, user, fixFactor, pdDocument, pdPage, newDate, dateFormat, xAdjusted, yAdjusted, rotation);
            }
        } else if (StringUtils.hasText(signRequestParams.getTextPart())) {
            float fontSize = signRequestParams.getFontSize() * fixFactor;
            PDFont pdFont = PDType0Font.load(pdDocument, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream(), true);
            contentStream.beginText();
            contentStream.setFont(pdFont, fontSize);
            String[] lines = signRequestParams.getTextPart().split("\n");
            PDFontDescriptor descriptor = pdFont.getFontDescriptor();
            float lineHeight = descriptor.getCapHeight() / 1000 * signRequestParams.getFontSize() / fixFactor;
            yAdjusted = yAdjusted + (lineHeight * (lines.length - 1));
            contentStream.newLineAtOffset(xAdjusted + 1, yAdjusted + lineHeight * fixFactor / 2);
            for (String line : lines) {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -lineHeight);
            }
            contentStream.endText();
        }
        contentStream.close();
    }

    private void addLink(SignRequest signRequest, SignRequestParams signRequestParams, User user, double fixFactor, PDDocument pdDocument, PDPage pdPage, Date newDate, DateFormat dateFormat, float xAdjusted, float yAdjusted, Matrix rotation) throws IOException {
        PDFTextStripper pdfTextStripper = new PDFTextStripper();

        String signatureInfos =
                "Signature calligraphique" + pdfTextStripper.getLineSeparator() +
                        "De : " + user.getFirstname() + " " + user.getName() + pdfTextStripper.getLineSeparator() +
                        "Le : " +  dateFormat.format(newDate) + pdfTextStripper.getLineSeparator() +
                        "Depuis : " + logService.getIp() + pdfTextStripper.getLineSeparator() +
                        "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                        globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();

        PDAnnotationLink pdAnnotationLink = new PDAnnotationLink();
        float width = (float) (signRequestParams.getSignWidth() * fixFactor);
        float height = (float) (signRequestParams.getSignHeight() * fixFactor);
        PDRectangle position = new PDRectangle(xAdjusted, yAdjusted, width, height);
        if(rotation!= null) {
            float x0 = position.getLowerLeftX();
            float y0 = position.getLowerLeftY();
            float x1 = position.getUpperRightX();
            float y1 = position.getUpperRightY();
            Point2D.Float p0 = rotation.transformPoint(x0, y0);
            Point2D.Float p1 = rotation.transformPoint(x1, y1);
            position = new PDRectangle(
                    Math.min(p0.x, p1.x),
                    Math.min(p0.y, p1.y),
                    Math.abs(p1.x - p0.x),
                    Math.abs(p1.y - p0.y)
            );
        }
        pdAnnotationLink.setRectangle(position);
        pdAnnotationLink.setQuadPoints(new float[0]);
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
                new String(data.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8),
                BarcodeFormat.DATA_MATRIX, 500, 500);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "png", outputStream);
        return outputStream;
    }

    public InputStream addQrCode(SignRequest signRequest, InputStream inputStream) throws IOException, WriterException {
        PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes());
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

    public Map<String, String> readMetadatas(InputStream inputStream) {
        Map<String, String> metadatas = new HashMap<>();
        try {
            PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes());
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
            PDDocument pdDocument = Loader.loadPDF(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.setVersion(1.7f);
            COSDictionary trailer = pdDocument.getDocument().getTrailer();
            COSDictionary rootDictionary = trailer.getCOSDictionary(COSName.ROOT);
            rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
            rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.7"));

            String producer = "esup-signature v." + globalProperties.getVersion();

            PDDocumentInformation info = pdDocument.getDocumentInformation();
            if(!StringUtils.hasText(info.getTitle())) {
                info.setTitle(signRequest.getTitle() + globalProperties.getSignedSuffix());
            }
            if(!StringUtils.hasText(info.getSubject())) {
                info.setSubject(fileName);
            }
            if(!StringUtils.hasText(info.getCreator())) {
                info.setCreator(signRequest.getCreateBy().getEppn());
            }
            if(!StringUtils.hasText(info.getProducer())) {
                info.setProducer(producer);
            }
            if (info.getCreationDate() == null) {
                info.setCreationDate(Calendar.getInstance());
            }
            if (info.getModificationDate() == null) {
                info.setModificationDate(Calendar.getInstance());
            }

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

            PDFAIdentificationSchema pdfaIdentificationSchema = xmpMetadata.createAndAddPDFAIdentificationSchema();
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

    public byte[] convertToPDFA(byte[] originalBytes) throws EsupSignatureRuntimeException {
        if (!isPdfAComplient(originalBytes) && pdfConfig.getPdfProperties().isConvertToPdfA()) {
            String params = pdfConfig.getPdfProperties().getGsCommandParams();
            if(!pdfConfig.getPdfProperties().isAutoRotate()) {
                params = params + " -dAutoRotatePages=/None";
            }
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -sstdout=%stderr -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dNOPAUSE -dNOSAFER -dBATCH -sFONTPATH=" + pdfConfig.getPdfProperties().getPathToFonts() + " " + params + " -sOutputFile=- '" + pdfConfig.getPdfADefPath() + "' - 2>/dev/null";
            logger.info("GhostScript PDF/A conversion : " + cmd);
            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }
            ByteArrayOutputStream convertedOutputStream = new ByteArrayOutputStream();
            byte[] result;
            Process process = null;
            try {
                process = processBuilder.start();
                IOUtils.copy(new ByteArrayInputStream(originalBytes), process.getOutputStream());
                process.getOutputStream().flush();
                process.getOutputStream().close();
                process.getInputStream().transferTo(convertedOutputStream);
                result = convertedOutputStream.toByteArray();
                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.info("Convert success");
                } else {
                    logger.warn("PDF/A conversion failure : document will be signed without conversion");
                    logger.warn("Convert command fail : " + cmd);
                    return originalBytes;
                }
            } catch (IOException | InterruptedException e) {
                logger.error("GhostScript error", e);
                return originalBytes;
            } finally {
                if(process != null) {
                    process.destroy();
                }
            }
            return result;
        } else {
            return originalBytes;
        }
    }

    public byte[] normalizePDF(byte[] originalBytes) throws IOException, EsupSignatureRuntimeException {
        ByteArrayOutputStream repairedOriginalBytes = new ByteArrayOutputStream();
        PDDocument pdDocument = Loader.loadPDF(originalBytes);
        for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {
            List<PDAnnotation> annotations = pdDocument.getPage(i).getAnnotations();
            for (PDAnnotation annotation : annotations) {
                if (annotation.getPage() == null) {
                        annotation.setPage(pdDocument.getPage(i));
                }
            }
        }
        pdDocument.save(repairedOriginalBytes);
        pdDocument.close();
        originalBytes = repairedOriginalBytes.toByteArray();
        Reports reports = validationService.validate(new ByteArrayInputStream(originalBytes), null);
        if (!isAcroForm(new ByteArrayInputStream(originalBytes)) && (reports == null || reports.getSimpleReport() == null || reports.getSimpleReport().getSignatureIdList().isEmpty())) {
            String params = "";
            if(!pdfConfig.getPdfProperties().isAutoRotate()) {
                params = params + " -dAutoRotatePages=/None";
            }
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -dPDFSTOPONERROR  -sstdout=%stderr -dBATCH -dNOPAUSE -dPassThroughJPEGImages=true -dNOSAFER -sDEVICE=pdfwrite" + params + " -d -sOutputFile=- - 2>/dev/null";
            logger.info("GhostScript normalize : " + cmd);
            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }
            byte[] result;
            Process process = null;
            try {
                process = processBuilder.start();
                process.getOutputStream().write(originalBytes);
                process.getOutputStream().flush();
                process.getOutputStream().close();
                result = process.getInputStream().readAllBytes();
                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.info("Convert success");
                } else {
                    logger.warn("PDF/A conversion failure : document will be added without conversion");
                    logger.warn("Convert command fail : " + cmd);
                    return originalBytes;
                }
            } catch (IOException | InterruptedException e) {
                logger.error("GhostScript error", e);
                return originalBytes;
            } finally {
                if(process != null) {
                    process.destroy();
                }
            }
            return result;
        } else {
            return originalBytes;
        }
    }

    public boolean isPdfAComplient(byte[] pdfFile) throws EsupSignatureRuntimeException {
        List<String> result = checkPDFA(pdfFile, false);
        if (!result.isEmpty() && "success".equals(result.get(0))) {
            return true;
        }
        return false;
    }

    public List<String> checkPDFA(byte[] pdfFile, boolean fillResults) throws EsupSignatureRuntimeException {
        List<String> result = new ArrayList<>();
        VeraGreenfieldFoundryProvider.initialise();
        try {
            PDFAParser parser = Foundries.defaultInstance().createParser(new ByteArrayInputStream(pdfFile));
            PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
            ValidationResult validationResult = validator.validate(parser);
            if (validationResult.isCompliant()) {
                result.add("success");
                result.add("Le document est conforme PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
            } else {
                result.add("danger");
                result.add("Le document n'est pas conforme PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
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

    public byte[] fill(InputStream pdfFile, Map<String, String> datas, boolean isLastStep, boolean isForm) {
        ByteArrayOutputStream interimOut = new ByteArrayOutputStream();
        try {
            PDDocument pdDocument = Loader.loadPDF(pdfFile.readAllBytes());
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                Map<String, Integer> pageNrByAnnotDict = getPageNumberByAnnotDict(pdDocument);
                PDType0Font pdFont = PDType0Font.load(pdDocument, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream(), false);
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("LiberationSans"), pdFont);
                pdAcroForm.setDefaultResources(resources);
                pdAcroForm.setDefaultAppearance("/LiberationSans 10 Tf 0 g");
                List<PDField> fields = pdAcroForm.getFields();
                for(PDField pdField : fields) {
                    if(pdField instanceof PDSignatureField) {
                        for(PDAnnotationWidget pdAnnotationWidget : pdField.getWidgets()) {
                            pdAnnotationWidget.setPage(pdDocument.getPage(0));
                        }
                        continue;
                    }
                    if(pageNrByAnnotDict.containsKey(pdField.getPartialName())) {
                        for (PDAnnotationWidget pdAnnotationWidget : pdField.getWidgets()) {
                            pdAnnotationWidget.getCOSObject().setString(COSName.DA, "/LiberationSans 10 Tf 0 g");
                            pdAnnotationWidget.setPage(pdDocument.getPage(pageNrByAnnotDict.get(pdField.getPartialName())));
                        }
                    }
                    String filedName = pdField.getPartialName().split("\\$|#|!")[0];
                    if(datas.containsKey(filedName)) {
                        if (pdField instanceof PDCheckBox) {
                            if (datas.get(filedName) != null && datas.get(filedName).equals("on")) {
                                ((PDCheckBox) pdField).check();
                            }
                        } else if (pdField instanceof PDRadioButton pdRadioButton) {
                            try {
                                String value = datas.get(filedName);
                                if(value.isEmpty()) {
                                    value = "Off";
                                }
                                pdRadioButton.setValue(value);
                            } catch (NullPointerException | IllegalArgumentException e) {
                                logger.debug("radio value error", e);
                            }
                        } else if (pdField instanceof PDListBox pdListBox) {
                            String value = datas.get(filedName);
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
                                int countPage = 1;
                                for (PDPage pdPage : pdDocument.getPages()) {
                                    pdPage.getAnnotations().removeAll(pdListBox.getWidgets());
                                    if(pageNrByAnnotDict.containsKey(pdField.getPartialName()) && countPage == pageNrByAnnotDict.get(pdField.getPartialName())) {
                                        pdPage.getAnnotations().add(pdAnnotationWidget);
                                    }
                                    countPage++;
                                }
                            }
                        } else {
                            String value = datas.get(filedName);
                            pdField.getCOSObject().setNeedToBeUpdated(true);
                            pdField.getCOSObject().removeItem(COSName.AA);
                            pdField.getCOSObject().removeItem(COSName.AP);
                            pdField.getCOSObject().setString(COSName.DA, "/LiberationSans 10 Tf 0 g");
                            try {
                                pdField.setValue(value);
                            } catch (Exception e) {
                                logger.warn("error on set value " + filedName + ", cause : " +e.getMessage());
                            }
                        }
                    }
                    if (!pdField.isReadOnly() && !isForm) {
                        pdField.setReadOnly(true);
                    }
                }
            }
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.save(interimOut);
            pdDocument.close();

            ByteArrayInputStream interimInput = new ByteArrayInputStream(interimOut.toByteArray());
            PDDocument pdDocument2 = Loader.loadPDF(interimInput.readAllBytes());
            PDAcroForm pdAcroForm2 = pdDocument2.getDocumentCatalog().getAcroForm();
            if (isLastStep && pdAcroForm2 != null) {
                pdDocument.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
                pdAcroForm2.flatten();
            }
            ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
            pdDocument2.setAllSecurityToBeRemoved(true);
            pdDocument2.save(finalOut);
            pdDocument2.close();
            return finalOut.toByteArray();
        } catch (IOException e) {
            logger.error("file read error", e);
        }
        return null;
    }

    public PDFieldTree getFields(PDDocument pdDocument) throws EsupSignatureRuntimeException {
        PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
        if(pdAcroForm != null) {
            return new PDFieldTree(pdAcroForm);
        }
        return null;
    }

    public InputStream removeSignField(InputStream pdfFile, Workflow workflow) {
        try {
            PDDocument pdDocument = Loader.loadPDF(pdfFile.readAllBytes());
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                PDFont pdFont = PDType0Font.load(pdDocument, new ClassPathResource("/static/fonts/LiberationSans-Regular.ttf").getInputStream(), true);
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("LiberationSans"), pdFont);
                pdAcroForm.setDefaultResources(resources);
                List<PDField> fields = pdAcroForm.getFields();
                for(PDField pdField : fields) {
                    if(pdField instanceof PDSignatureField) {
                        removeField(pdField, pdDocument, pdAcroForm);
                    } else if(workflow != null && StringUtils.hasText(workflow.getSignRequestParamsDetectionPattern())) {
                        String className = "org.apache.pdfbox.pdmodel.interactive.form.PD" + extractTextInBrackets(workflow.getSignRequestParamsDetectionPattern());
                        try {
                            Class<?> pdFieldClass = Class.forName(className);
                            if (pdFieldClass.isInstance(pdField)) {
                                Method getPartialNameMethod = pdFieldClass.getMethod("getPartialName");
                                String signFieldName = (String) getPartialNameMethod.invoke(pdField);
                                Pattern pattern = Pattern.compile(workflow.getSignRequestParamsDetectionPattern().split("]")[1], Pattern.CASE_INSENSITIVE);
                                if (pattern.matcher(signFieldName).find()) {
                                    removeField(pdField, pdDocument, pdAcroForm);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            logger.warn("error on remove sign field", e);
                        }
                    }
                }
            }
            if(workflow != null && StringUtils.hasText(workflow.getSignRequestParamsDetectionPattern()) && workflow.getSignRequestParamsDetectionPattern().contains("AnnotationLink")) {
                try {
                    PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
                    PDPageTree pdPages = docCatalog.getPages();
                    for (PDPage pdPage : pdPages) {
                        List<PDAnnotation> annotationsToRemove = new ArrayList<>();
                        List<PDAnnotation> pdAnnotations = pdPage.getAnnotations();
                        for (PDAnnotation pdAnnotation : pdAnnotations) {
                            if (pdAnnotation instanceof PDAnnotationLink pdAnnotationLink) {
                                String signFieldName = ((PDActionURI) pdAnnotationLink.getAction()).getURI();
                                Pattern pattern = Pattern.compile(workflow.getSignRequestParamsDetectionPattern().split("]")[1], Pattern.CASE_INSENSITIVE);
                                if(pattern.matcher(signFieldName).find()) {
                                    annotationsToRemove.add(pdAnnotation);
                                    PDRectangle linkPosition = pdAnnotationLink.getRectangle();
                                    Rectangle2D.Float rect = new Rectangle2D.Float(
                                            linkPosition.getLowerLeftX(),
                                            linkPosition.getLowerLeftY(),
                                            linkPosition.getWidth(),
                                            linkPosition.getHeight()
                                    );
                                    try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true)) {
                                        contentStream.setNonStrokingColor(1f, 1f, 1f);
                                        contentStream.addRect(rect.x-1, rect.y-1, rect.width+1, rect.height+1);
                                        contentStream.fill();
                                    }
                                }
                            }
                        }
                        pdAnnotations.removeAll(annotationsToRemove);
                    }
                } catch (IOException e) {
                    logger.warn("error on remove sign field fake link", e);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.save(out);
            pdDocument.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            logger.error("file read error", e);
        }
        return null;
    }

    private static void removeField(PDField pdField, PDDocument pdDocument, PDAcroForm pdAcroForm) throws IOException {
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
                    logger.debug("Inconsistent annotation definition: Page annotations do not include the target widget.");
            }
        }
        pdAcroForm.getFields().remove(pdField);
    }

    private String extractTextInBrackets(String input) {
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public PdfParameters getPdfParameters(InputStream pdfFile, int pageNumber) {
        PDDocument pdDocument = null;
        try {
            pdDocument = Loader.loadPDF(pdfFile.readAllBytes());
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
        return new PdfParameters((int) pdPage.getMediaBox().getWidth(), (int) pdPage.getMediaBox().getHeight(), pdPage.getRotation(), pdDocument.getNumberOfPages());
    }

    public BufferedImage pageAsBufferedImage(InputStream pdfFile, int page) {
        BufferedImage bufferedImage = null;
        PDDocument pdDocument = null;
        try {
            pdDocument = Loader.loadPDF(pdfFile.readAllBytes());
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

    private InputStream bufferedImageToInputStream(BufferedImage image) throws IOException {
        return fileService.bufferedImageToInputStream(image, "png");
    }

    public InputStream pageAsInputStream(InputStream pdfFile, int page) throws Exception {
        BufferedImage bufferedImage = pageAsBufferedImage(pdfFile, page);
        InputStream inputStream = bufferedImageToInputStream(bufferedImage);
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
        return pageNrByAnnotDict;
    }

    public boolean isAcroForm(ByteArrayInputStream byteArrayInputStream) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(byteArrayInputStream.readAllBytes())) {
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            return pdAcroForm != null && !pdAcroForm.getFields().isEmpty();
        }
    }

    public void checkPdfPermitions(MultipartFile multipartFile) throws EsupSignatureRuntimeException {
        if(!Objects.equals(multipartFile.getContentType(), "application/pdf")) {
            return;
        }
        try {
            PdfPermissionsChecker pdfPermissionsChecker = new PdfPermissionsChecker();
            pdfPermissionsChecker.checkSignatureRestrictionDictionaries(new PdfBoxDocumentReader(Loader.loadPDF(multipartFile.getBytes())), new SignatureFieldParameters());
            pdfPermissionsChecker.checkDocumentPermissions(new PdfBoxDocumentReader(Loader.loadPDF(multipartFile.getBytes())), new SignatureFieldParameters());
        } catch (IOException e) {
            logger.error("error on check pdf permitions", e);
            throw new EsupSignatureRuntimeException("error on check pdf permitions", e);
        } catch (ProtectedDocumentException e) {
            logger.warn(multipartFile.getOriginalFilename() + " : " + e.getMessage());
            throw new EsupSignatureRuntimeException("La création de nouvelles signatures n'est pas autorisée dans le document actuel. Raison : Le dictionnaire des autorisations PDF n'autorise pas la modification ou la création de champs de formulaire interactifs, y compris les champs de signature, lorsque le document est ouvert avec un accès utilisateur.");
        }
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
