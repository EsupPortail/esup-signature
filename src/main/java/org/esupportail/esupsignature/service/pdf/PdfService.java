package org.esupportail.esupsignature.service.pdf;

import org.apache.commons.lang3.SystemUtils;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
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
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.file.FileService;
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
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    @Resource
    private PdfConfig pdfConfig;

    @Resource
    private FileService fileService;

    @Resource
    private GlobalProperties globalProperties;

    public InputStream stampImage(InputStream inputStream, SignRequest signRequest, SignRequestParams signRequestParams, User user) {
        SignType signType = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType();
        PdfParameters pdfParameters;
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdfParameters = getPdfParameters(pdDocument);
            PDPage pdPage = pdDocument.getPage(signRequestParams.getSignPageNumber() - 1);

            DateFormat dateFormat = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss", Locale.FRENCH);
            Date newDate = new Date();
            String addText = "";
            int lineNumber = 0;
            if(signRequestParams.isAddName()) {
                if(signType.equals(SignType.visa)) {
                    addText += "Visé par " + user.getFirstname() + " " + user.getName() + "\n";
                } else {
                    addText += "Signé par " + user.getFirstname() + " " + user.getName() + "\n";
                }
                lineNumber++;
            }
            if (signRequestParams.isAddDate()) {
                addText +="Le " + dateFormat.format(newDate);
                lineNumber++;
            }
            InputStream signImage;
            if (signType.equals(SignType.visa)) {
                signImage = fileService.addTextToImage(PdfService.class.getResourceAsStream("/sceau.png"), addText, lineNumber);
            } else {
                signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), addText, lineNumber);
            }
            BufferedImage bufferedSignImage = ImageIO.read(signImage);
            ByteArrayOutputStream signImageByteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedSignImage, "png", signImageByteArrayOutputStream);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, signImageByteArrayOutputStream.toByteArray(), "sign.png");

            float tx = 0;
            float ty = 0;
            float xAdjusted = signRequestParams.getxPos();
            float yAdjusted;
            float width =  bufferedSignImage.getWidth();
            float height = bufferedSignImage.getHeight();
            int heightAdjusted = Math.round(((float) signRequestParams.getSignWidth() / width) * height);
            if(pdfParameters.getRotation() == 0 || pdfParameters.getRotation() == 180) {
                yAdjusted = pdfParameters.getHeight() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + (heightAdjusted - signRequestParams.getSignHeight()) + pdPage.getCropBox().getLowerLeftY();
            } else {
                yAdjusted = pdfParameters.getWidth() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + (heightAdjusted - signRequestParams.getSignHeight()) + pdPage.getCropBox().getLowerLeftY();
            }
            if (pdfParameters.isLandScape()) {
                tx = pdfParameters.getWidth();
            } else {
                ty = pdfParameters.getHeight();
            }
            PDFTextStripper pdfTextStripper = new PDFTextStripper();

            String signatureInfos =
                    "Signature calligraphique" + pdfTextStripper.getLineSeparator() +
                    "De : " + user.getEppn() + pdfTextStripper.getLineSeparator() +
                    "Le : " +  dateFormat.format(newDate) + pdfTextStripper.getLineSeparator() +
                    "Liens de contrôle : " + pdfTextStripper.getLineSeparator() +
                    globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken();

            PDAnnotationLink pdAnnotationLink = new PDAnnotationLink();
            PDRectangle position = new PDRectangle(xAdjusted, yAdjusted, signRequestParams.getSignWidth(), heightAdjusted);
            pdAnnotationLink.setRectangle(position);
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

            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);

            if (pdfParameters.getRotation() != 0 && pdfParameters.getRotation() != 360 ) {
                contentStream.transform(Matrix.getRotateInstance(Math.toRadians(pdfParameters.getRotation()), tx, ty));
            }
            contentStream.drawImage(pdImage, xAdjusted, yAdjusted, signRequestParams.getSignWidth(), heightAdjusted);
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

//    public InputStream stampText(Document document, String text, int xPos, int yPos, int pageNumber) {
//        //signRequestService.setStep("Apposition de la signature");
//        PdfParameters pdfParameters;
//        try {
//            PDDocument pdDocument = PDDocument.load(document.getInputStream());
//            pdfParameters = getPdfParameters(pdDocument);
//            PDPage pdPage = pdDocument.getPage(pageNumber - 1);
//            PDImageXObject pdImage;
//
//            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);
//            float height = pdPage.getMediaBox().getHeight();
//            float width = pdPage.getMediaBox().getWidth();
//            File signImage = fileService.addTextToImage(PdfService.class.getResourceAsStream("/sceau.png"), text, text.length() * 10, 20);
//            BufferedImage bufferedImage = ImageIO.read(signImage);
//            if (pdfParameters.getRotation() == 0) {
//                AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
//                tx.translate(0, -bufferedImage.getHeight(null));
//                AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//                bufferedImage = op.filter(bufferedImage, null);
//                ByteArrayOutputStream flipedSignImage = new ByteArrayOutputStream();
//                ImageIO.write(bufferedImage, "png", flipedSignImage);
//                pdImage = PDImageXObject.createFromByteArray(pdDocument, flipedSignImage.toByteArray(), "sign.png");
//                contentStream.transform(new Matrix(new java.awt.geom.AffineTransform(1, 0, 0, -1, 0, height)));
//                contentStream.drawImage(pdImage, xPos, yPos, bufferedImage.getWidth(), bufferedImage.getHeight());
//            } else {
//                AffineTransform at = new java.awt.geom.AffineTransform(0, 1, -1, 0, width, 0);
//                contentStream.transform(new Matrix(at));
//                ByteArrayOutputStream flipedSignImage = new ByteArrayOutputStream();
//                ImageIO.write(bufferedImage, "png", flipedSignImage);
//                pdImage = PDImageXObject.createFromByteArray(pdDocument, flipedSignImage.toByteArray(), "sign.png");
//                contentStream.drawImage(pdImage, xPos, yPos - 37, bufferedImage.getWidth(), bufferedImage.getWidth());
//            }
//            contentStream.close();
//            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            pdDocument.setAllSecurityToBeRemoved(true);
//            pdDocument.save(out);
//            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//            pdDocument.close();
//            signImage.delete();
//            return in;
//        } catch (IOException e) {
//            logger.error("error to add image", e);
//        }
//        return null;
//    }

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
                    PDDocumentCatalog catalog = pdDocument.getDocumentCatalog();
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

    public InputStream writeMetadatas(InputStream inputStream, String fileName, SignRequest signRequest) {

        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdDocument.setAllSecurityToBeRemoved(true);
            pdDocument.setVersion(1.7f);

            COSDictionary trailer = pdDocument.getDocument().getTrailer();
            COSDictionary rootDictionary = trailer.getCOSDictionary(COSName.ROOT);
            rootDictionary.setItem(COSName.TYPE, COSName.CATALOG);
            rootDictionary.setItem(COSName.VERSION, COSName.getPDFName("1.7"));

            PDDocumentInformation info = pdDocument.getDocumentInformation();
            info.setTitle(fileName);
            info.setSubject(fileName);
            info.setAuthor(signRequest.getCreateBy().getEppn());
            info.setCreator(signRequest.getCreateBy().getEppn());
            info.setProducer("esup-signature");
            info.setKeywords("pdf, signed, " + fileName);
            if (info.getCreationDate() == null) {
                info.setCreationDate(Calendar.getInstance());
            }
            info.setModificationDate(Calendar.getInstance());
            pdDocument.setDocumentInformation(info);

            PDDocumentCatalog cat = pdDocument.getDocumentCatalog();

            XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();

            AdobePDFSchema pdfSchema = xmpMetadata.createAndAddAdobePDFSchema();
            pdfSchema.setKeywords(info.getKeywords());
            pdfSchema.setProducer(info.getProducer());

            DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
            dublinCoreSchema.setTitle(info.getTitle());
            dublinCoreSchema.setDescription(info.getSubject());
            dublinCoreSchema.addCreator(info.getAuthor());

            XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
            xmpBasicSchema.setCreatorTool(info.getCreator());
            xmpBasicSchema.setCreateDate(info.getCreationDate());
            xmpBasicSchema.setModifyDate(info.getModificationDate());
            xmpBasicSchema.addIdentifier(signRequest.getToken());
            xmpBasicSchema.addIdentifier(globalProperties.getRootUrl() + "/public/control/" + signRequest.getToken());

            PDFAIdentificationSchema pdfaid = xmpMetadata.createAndAddPFAIdentificationSchema();
            pdfaid.setConformance("B");
            pdfaid.setPart(pdfConfig.getPdfProperties().getPdfALevel());
            pdfaid.setAboutAsSimple(null);

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
            pdDocument.save(out);
            pdDocument.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException | BadFieldValueException | TransformerException e) {
            logger.error("error on write metadatas", e);
        }
        return inputStream;
    }

    public InputStream convertGS(InputStream inputStream) throws IOException, EsupSignatureException {
        File file = fileService.inputStreamToTempFile(inputStream, "temp.pdf");
        if (!isPdfAComplient(file) && pdfConfig.getPdfProperties().isConvertToPdfA()) {
            File targetFile = fileService.getTempFile("afterconvert_tmp.pdf");
            String defFile = PdfService.class.getResource("/PDFA_def.ps").getFile();
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dBATCH -dNOPAUSE -dPreserveAnnots=true -dShowAnnots=true -dPrinted=false -dNOSAFER -sColorConversionStrategy=UseDeviceIndependentColor -sDEVICE=pdfwrite -dPDFACompatibilityPolicy=1 -dCompatibilityLevel=1.7 -d -sOutputFile='" + targetFile.getAbsolutePath() + "' '" + defFile + "' '" + file.getAbsolutePath() + "'";
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

    public void addText(PDPageContentStream contentStream, String text, int xPos, int yPos, PDFont font) throws IOException {
        int fontSize = 8;
        contentStream.beginText();
        contentStream.newLineAtOffset(xPos, 830 - yPos);
        contentStream.setFont(font, fontSize);
        contentStream.showText(text);
        contentStream.endText();
    }

    public int[] getSignSize(BufferedImage bimg) {
        int signWidth;
        int signHeight;
        if (bimg.getWidth() <= pdfConfig.getPdfProperties().getSignWidthThreshold() * 2) {
            signWidth = bimg.getWidth();
            signHeight = bimg.getHeight();
        } else {
            signWidth = pdfConfig.getPdfProperties().getSignWidthThreshold();
            double percent = ((double) pdfConfig.getPdfProperties().getSignWidthThreshold() / (double) bimg.getWidth());
            signHeight = (int) (percent * bimg.getHeight());
        }
        return new int[]{signWidth, signHeight};
    }

    public int[] getSignSize(InputStream signFile) throws IOException {
        BufferedImage bimg = ImageIO.read(signFile);
        return getSignSize(bimg);
    }

    private Map<COSDictionary, Integer> getPageNrByAnnotDict(PDDocumentCatalog docCatalog) throws IOException {
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

    public List<SignRequestParams> pdSignatureFieldsToSignRequestParams(PDDocument pdDocument) {
        List<SignRequestParams> signRequestParamsList = new ArrayList<>();
		try {
			PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
			Map<COSDictionary, Integer> pageNrByAnnotDict = getPageNrByAnnotDict(docCatalog);
			PDAcroForm acroForm = docCatalog.getAcroForm();
			if(acroForm != null) {
                for (PDField pdField : acroForm.getFields()) {
                    if (pdField instanceof PDSignatureField) {
                        PDSignatureField pdSignatureField = (PDSignatureField) pdField;
                        SignRequestParams signRequestParams = new SignRequestParams();
                        List<Integer> annotationPages = new ArrayList<>();
                        List<PDAnnotationWidget> kids = pdField.getWidgets();
                        if (kids != null) {
                            for (COSObjectable kid : kids) {
                                COSBase kidObject = kid.getCOSObject();
                                if (kidObject instanceof COSDictionary)
                                    annotationPages.add(pageNrByAnnotDict.get(kidObject));
                            }
                        }
                        PDPage pdPage = pdDocument.getPage(annotationPages.get(0) - 1);
                        signRequestParams.setSignImageNumber(0);
                        signRequestParams.setPdSignatureFieldName(pdSignatureField.getPartialName());
                        signRequestParams.setxPos((int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX());
                        signRequestParams.setyPos((int) pdPage.getBBox().getHeight() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getHeight());
                        signRequestParams.setSignPageNumber(annotationPages.get(0));
                        signRequestParamsList.add(signRequestParams);
                    }
                }
            }
			pdDocument.close();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return signRequestParamsList.stream().sorted(Comparator.comparingInt(value -> value.getxPos())).sorted(Comparator.comparingInt(value -> value.getyPos())).sorted(Comparator.comparingInt(SignRequestParams::getSignPageNumber)).collect(Collectors.toList());
    }

    public InputStream fill(InputStream pdfFile, Map<String, String> datas) {
        try {
            PDDocument pdDocument = PDDocument.load(pdfFile);
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            if(pdAcroForm != null) {
                PDFont font = PDType1Font.HELVETICA;
                PDResources resources = pdAcroForm.getDefaultResources();
                resources.put(COSName.getPDFName("Helv"), font);
                resources.put(COSName.getPDFName("Helvetica"), font);
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
                                pdRadioButton.setValue(datas.get(filedName));
                            } catch (NullPointerException e) {
                                logger.debug("radio buton is null");
                            }
                        } else {
                            if (!(pdField instanceof PDSignatureField)) {
                                PDAnnotationWidget ww = pdField.getWidgets().get(0);
                                pdField.setValue(datas.get(filedName));
                                pdField.getCOSObject().setNeedToBeUpdated(true);
                                pdField.getCOSObject().removeItem(COSName.AA);
                                pdField.getCOSObject().removeItem(COSName.AP);
                                pdField.getCOSObject().setString(COSName.DA, "/Helv 11 Tf 0 g");
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

    public List<SignRequestParams> scanSignatureFields(InputStream inputStream) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            return pdSignatureFieldsToSignRequestParams(pdDocument);
        } catch (IOException e) {
            throw new EsupSignatureIOException("unable to open pdf document");
        }
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