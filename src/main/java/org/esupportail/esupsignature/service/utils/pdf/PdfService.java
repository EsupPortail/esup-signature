package org.esupportail.esupsignature.service.utils.pdf;

import org.apache.commons.lang3.SystemUtils;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
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
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
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
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    @Resource
    private PdfConfig pdfConfig;

    @Resource
    private FileService fileService;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private LogService logService;

    public InputStream stampImage(InputStream inputStream, SignRequest signRequest, SignRequestParams signRequestParams, User user) {
        SignType signType = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        PdfParameters pdfParameters;
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdfParameters = getPdfParameters(pdDocument);
            PDPage pdPage = pdDocument.getPage(signRequestParams.getSignPageNumber() - 1);

            Date newDate = new Date();
            DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);
            InputStream signImage;
            if (signType.equals(SignType.visa)) {
                signImage = fileService.addTextToImage(PdfService.class.getResourceAsStream("/static/images/sceau.png"), signRequestParams);
            } else if (signRequestParams.getAddExtra()) {
                signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), signRequestParams);
                File fileWithWatermark = fileService.getTempFile("sign_with_mark.png");
                fileService.addImageWatermark(PdfService.class.getResourceAsStream("/static/images/watermark.png"), signImage, fileWithWatermark, new Color(141, 198, 64));
                signImage = new FileInputStream(fileWithWatermark);
            } else {
                if(signRequestParams.getSignImageNumber() == user.getSignImages().size()) {
                    signImage = SignRequestService.class.getResourceAsStream("/static/images/check.png");
                } else {
                    signImage = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
                    File fileWithWatermark = fileService.getTempFile("sign_with_mark.png");
                    fileService.addImageWatermark(PdfService.class.getResourceAsStream("/static/images/watermark.png"), signImage, fileWithWatermark, new Color(141, 198, 64));
                    signImage = new FileInputStream(fileWithWatermark);
                }
            }
            BufferedImage bufferedSignImage = ImageIO.read(signImage);
            fileService.changeColor(bufferedSignImage, 0, 0, 0, signRequestParams.getRed(), signRequestParams.getGreen(), signRequestParams.getBlue());
            ByteArrayOutputStream signImageByteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedSignImage, "png", signImageByteArrayOutputStream);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, signImageByteArrayOutputStream.toByteArray(), "sign.png");

            float tx = 0;
            float ty = 0;
            float xAdjusted = signRequestParams.getxPos();
            float yAdjusted;
            int widthAdjusted = Math.round((float) signRequestParams.getSignWidth());
            int heightAdjusted = Math.round((float) signRequestParams.getSignHeight());

            if(pdfParameters.getRotation() == 0 || pdfParameters.getRotation() == 180) {
                yAdjusted = pdfParameters.getHeight() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + pdPage.getCropBox().getLowerLeftY();
            } else {
                yAdjusted = pdfParameters.getWidth() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + pdPage.getCropBox().getLowerLeftY();
            }
            if (pdfParameters.isLandScape()) {
                tx = pdfParameters.getWidth();
            } else {
                ty = pdfParameters.getHeight();
            }
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            String signatureInfos =
                    "Signature calligraphique" + pdfTextStripper.getLineSeparator() +
                    "De : " + user.getFirstname() + " " + user.getName() + pdfTextStripper.getLineSeparator() +
                    "Le : " +  dateFormat.format(newDate) + pdfTextStripper.getLineSeparator() +
                    "Depuis : " + logService.getIp() + pdfTextStripper.getLineSeparator() +
                    "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                    globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();

            PDAnnotationLink pdAnnotationLink = new PDAnnotationLink();
            PDRectangle position = new PDRectangle(xAdjusted, yAdjusted, signRequestParams.getSignWidth(), heightAdjusted);
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

//            PDDocumentInformation info = pdDocument.getDocumentInformation();
//            info.setKeywords(signatureInfos);
//            info.setCustomMetadataValue("signatureInfos_" + signRequest.getSignedDocuments().size() + 1, signatureInfos);
//            pdDocument.setDocumentInformation(info);

            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);

            if (pdfParameters.getRotation() != 0 && pdfParameters.getRotation() != 360 ) {
                contentStream.transform(Matrix.getRotateInstance(Math.toRadians(pdfParameters.getRotation()), tx, ty));
            }
            logger.info("stamp image to " + Math.round(xAdjusted) +", " + Math.round(yAdjusted));
            contentStream.drawImage(pdImage, xAdjusted, yAdjusted, widthAdjusted, heightAdjusted);
            contentStream.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.save(out);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            pdDocument.close();
            return in;
        } catch (IOException e) {
            logger.error("error to add image", e);
        }
        return null;
    }

    public List<String> getSignatureStrings(User user, SignType signType, Date newDate, DateFormat dateFormat) throws IOException {
        List<String> addText = new ArrayList<>();
        if(signType.equals(SignType.visa)) {
            addText.add("Visé par " + user.getFirstname() + " " + user.getName() + "\n");
        } else {
            addText.add("Signé par " + user.getFirstname() + " " + user.getName() + "\n");
        }
        addText.add("Le " + dateFormat.format(newDate));
        return addText;
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
                    logger.error("error on search metadatas", e);
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

    public InputStream writeMetadatas(InputStream inputStream, String fileName, SignRequest signRequest, List<Log> additionnalLogs) {

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
            info.setTitle(fileName);
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
            pdfaIdentificationSchema.setPart(pdfConfig.getPdfProperties().getPdfALevel());
            pdfaIdentificationSchema.setAboutAsSimple("");

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

            pdfSchema.setKeywords(info.getKeywords());

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
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException | BadFieldValueException | TransformerException e) {
            logger.error("error on write metadatas", e);
        }
        return inputStream;
    }

    public InputStream convertGS(InputStream inputStream, String UUID) throws IOException, EsupSignatureException {
        File file = fileService.inputStreamToTempFile(inputStream, "temp.pdf");
        if (!isPdfAComplient(file) && pdfConfig.getPdfProperties().isConvertToPdfA()) {
            File targetFile = fileService.getTempFile("afterconvert_tmp.pdf");
            String defFile = PdfService.class.getResource("/PDFA_def.ps").getFile();
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dBATCH -dNOPAUSE -dSubsetFonts=true -dPreserveAnnots=true -dShowAnnots=true -dPrinted=false -dNOSAFER -sColorConversionStrategy=UseDeviceIndependentColor -sDEVICE=pdfwrite -dPDFACompatibilityPolicy=1 -dCompatibilityLevel=1.7 -sDocumentUUID=" + UUID + " -d -sOutputFile='" + targetFile.getAbsolutePath() + "' '" + defFile + "' '" + file.getAbsolutePath() + "'";
            //String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dBATCH -dNOPAUSE -dPreserveAnnots=true -dShowAnnots=true -dPrinted=false -dDOPDFMARKS -dNOSAFER -sColorConversionStrategy=RGB -sDEVICE=pdfwrite -sOutputFile='" + targetFile.getAbsolutePath() + "' -c '/PreserveAnnotTypes [/Text /UnderLine /Link /Stamp /FreeText /Squiggly /Underline] def' -f '" + file.getAbsolutePath() + "'";
            logger.info("GhostScript PDF/A convertion : " + cmd);

            ProcessBuilder processBuilder = new ProcessBuilder();
            if(SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/C", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }
            //processBuilder.directory(new File("/tmp"));
            try {
                Process process = processBuilder.start();
                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line + "\n");
                }

                int exitVal = process.waitFor();
                if (exitVal == 0) {
                    logger.info("Convert success");
                    logger.debug(output.toString());
                } else {
                    logger.warn("Convert fail");
                    logger.warn(cmd);
                    logger.warn(output.toString());
                    throw new EsupSignatureSignException("PDF/A convertion failure");
                }
            } catch (InterruptedException e) {
                logger.error("GhostScript launcs error : check installation or path", e);
                throw new EsupSignatureSignException("GhostScript launch error");
            }
            InputStream convertedInputStream = new FileInputStream(targetFile);
            file.delete();
            targetFile.delete();
            return convertedInputStream;
        } else {
            FileInputStream fileInputStream = new FileInputStream(file);
            file.delete();
            return fileInputStream;
        }
    }

    public boolean isPdfAComplient(File pdfFile) throws EsupSignatureException {
        if ("success".equals(checkPDFA(pdfFile, false).get(0))) {
            return true;
        }
        return false;
    }

    public List<String> checkPDFA(InputStream inputStream, boolean fillResults) throws IOException, EsupSignatureException {
        File file = fileService.inputStreamToTempFile(inputStream, "tmp.pdf");
        List<String> checkResult = checkPDFA(file, fillResults);
        file.delete();
        return checkResult;
    }

    public List<String> checkPDFA(File pdfFile, boolean fillResults) throws EsupSignatureException {
        List<String> result = new ArrayList<>();
        VeraGreenfieldFoundryProvider.initialise();
        try {
            PDFAParser parser = Foundries.defaultInstance().createParser(pdfFile);
            PDFAValidator validator = Foundries.defaultInstance().createValidator(parser.getFlavour(), false);
            ValidationResult validationResult = validator.validate(parser);
            if (validationResult.isCompliant()) {
                result.add("success");
                result.add("File is complient with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
            } else {
                result.add("danger");
                result.add("File is not complient with PDF/A-" + validationResult.getPDFAFlavour().getId() + " !");
                if (fillResults) {
                    for (TestAssertion test : validationResult.getTestAssertions()) {
                        result.add(test.getRuleId().getClause() + " : " + test.getMessage());
                    }
                }
            }
            validator.close();
            parser.close();
        } catch (Exception e) {
            logger.error("check error", e);
            throw new EsupSignatureException("check pdf error", e);
        }
        return result;
    }

    public InputStream generatePdfFromData(Data data) throws IOException {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(doc, page);
        contents.beginText();
        PDFont font = PDType1Font.HELVETICA_BOLD;
        contents.setFont(font, 15);
        contents.setLeading(15f);
        contents.newLineAtOffset(30, 700);
        contents.newLine();
        for (Map.Entry<String, String> entry : data.getDatas().entrySet()) {
            contents.newLine();
            contents.showText(entry.getKey() + " : " + entry.getValue());
        }
        contents.endText();
        contents.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return  new ByteArrayInputStream(out.toByteArray());
    }

    public PDDocument addNewPage(PDDocument pdDocument, String template, int position) {
        try {
            PDDocument targetPDDocument = new PDDocument();
            PDPage newPage = null;
            if (template != null) {
                PDDocument defaultNewPageTemplate = PDDocument.load(new ClassPathResource(template, PdfService.class).getFile());
                if (defaultNewPageTemplate != null) {
                    newPage = defaultNewPageTemplate.getPage(0);
                }
            } else {
                PDDocument defaultNewPageTemplate = PDDocument.load(new ClassPathResource("/templates/pdf/defaultnewpage.pdf", PdfService.class).getFile());
                if (defaultNewPageTemplate != null) {
                    newPage = defaultNewPageTemplate.getPage(0);
                } else {
                    newPage = new PDPage(PDRectangle.A4);
                }
            }
            if (position == 0) {
                targetPDDocument.addPage(newPage);
            }

            for (PDPage page : pdDocument.getPages()) {
                targetPDDocument.addPage(page);
            }

            if (position == -1) {
                targetPDDocument.addPage(newPage);
            }
            return targetPDDocument;
        } catch (IOException e) {
            logger.error("error to add blank page", e);
        }
        return null;
    }

    public Map<COSDictionary, Integer> getPageNrByAnnotDict(PDDocumentCatalog docCatalog) throws IOException {
        Iterator<PDPage> pages = docCatalog.getPages().iterator();
        Map<COSDictionary, Integer> pageNrByAnnotDict = new HashMap<>();
        int i = 0;
        for (Iterator<PDPage> it = pages; it.hasNext(); ) {
            PDPage pdPage = it.next();
            for (PDAnnotation annotation : pdPage.getAnnotations()) {
                pageNrByAnnotDict.put(annotation.getCOSObject(), i + 1);
            }
            i++;
        }
        return pageNrByAnnotDict;
    }

    public InputStream fill(InputStream pdfFile, Map<String, String> datas) {
        try {
            PDDocument pdDocument = PDDocument.load(pdfFile);
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                PDFont pdFont = PDTrueTypeFont.load(pdDocument, new ClassPathResource("fonts/LiberationSans-Regular.ttf").getFile(), WinAnsiEncoding.INSTANCE);
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("Helv"), pdFont);
                resources.put(COSName.getPDFName("Helvetica"), pdFont);
                pdAcroForm.setDefaultResources(resources);
                List<PDField> fields = pdAcroForm.getFields();
                for(PDField pdField : fields) {
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
                            } catch (NullPointerException e) {
                                logger.debug("radio buton is null");
                            }
                        } else {
                            if (!(pdField instanceof PDSignatureField)) {
                                String value = datas.get(filedName);
                                pdField.getCOSObject().setNeedToBeUpdated(true);
                                pdField.getCOSObject().removeItem(COSName.AA);
                                pdField.getCOSObject().removeItem(COSName.AP);
                                pdField.getCOSObject().setString(COSName.DA, "/Helv 10 Tf 0 g");
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

    public PDFieldTree getFields(PDDocument pdDocument) {
        try {
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            PDFieldTree fields = new PDFieldTree(pdAcroForm);
            pdDocument.close();
            return fields;
        } catch (IOException e) {
            logger.error("file read error", e);
        }
        return null;
    }

    public PdfParameters getPdfParameters(InputStream pdfFile) {
        PDDocument pdDocument = null;
        try {
            pdDocument = PDDocument.load(pdfFile);
            return getPdfParameters(pdDocument);
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

    public PdfParameters getPdfParameters(PDDocument pdDocument) {
        PDPage pdPage = pdDocument.getPage(0);
        PdfParameters pdfParameters = new PdfParameters((int) pdPage.getMediaBox().getWidth(), (int) pdPage.getMediaBox().getHeight(), pdPage.getRotation(), pdDocument.getNumberOfPages());
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

}