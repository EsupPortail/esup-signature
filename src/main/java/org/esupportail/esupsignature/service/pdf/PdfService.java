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
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.xmlgraphics.xmp.Metadata;
import org.apache.xmlgraphics.xmp.schemas.pdf.PDFXAdapter;
import org.apache.xmlgraphics.xmp.schemas.pdf.PDFXXMPSchema;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.Attribute;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.esupportail.esupsignature.config.pdf.PdfConfig;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
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

    public InputStream stampImage(InputStream inputStream, SignType signType, SignRequestParams signRequestParams, User user, boolean addDate) {
        PdfParameters pdfParameters;
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            pdfParameters = getPdfParameters(pdDocument);
            PDPage pdPage = pdDocument.getPage(signRequestParams.getSignPageNumber() - 1);
            PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, AppendMode.APPEND, true, true);
            DateFormat dateFormat = new SimpleDateFormat("dd MMMM YYYY HH:mm:ss", Locale.FRENCH);
            InputStream signImage = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
            String text = "";
            if (signType.equals(SignType.visa)) {
                try {
                    text += "Visé par " + user.getFirstname() + " " + user.getName() + "\n";
                    if (addDate) {
                        text +="Le " + dateFormat.format(new Date());
                    }
                    signImage = fileService.addTextToImage(PdfService.class.getResourceAsStream("/sceau.png"), text, signRequestParams.getSignWidth(), signRequestParams.getSignHeight());
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
//                TODO add nom prenom ?
//                if(addName) {
//
//                }
                if (addDate) {
                    text +="Le " + dateFormat.format(new Date());
                }
                signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), text, signRequestParams.getSignWidth(), signRequestParams.getSignHeight());
            }
            BufferedImage bufferedImage = ImageIO.read(signImage);
            ByteArrayOutputStream signImageByteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", signImageByteArrayOutputStream);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(pdDocument, signImageByteArrayOutputStream.toByteArray(), "sign.png");
            float tx = 0;
            float ty = 0;
            float x_adjusted = signRequestParams.getxPos();
            float y_adjusted = signRequestParams.getyPos();

            if(pdfParameters.getRotation() == 0 || pdfParameters.getRotation() == 180) {
                y_adjusted = pdfParameters.getHeight() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + pdPage.getCropBox().getLowerLeftY();
            } else {
                y_adjusted = pdfParameters.getWidth() - signRequestParams.getyPos() - signRequestParams.getSignHeight() + pdPage.getCropBox().getLowerLeftY();
            }
            if (pdfParameters.isLandScape()) {
                tx = pdfParameters.getWidth();
            } else {
                ty = pdfParameters.getHeight();
            }
            if (pdfParameters.getRotation() != 0 && pdfParameters.getRotation() != 360 ) {
                contentStream.transform(Matrix.getRotateInstance(Math.toRadians(pdfParameters.getRotation()), tx, ty));
            }
            contentStream.drawImage(pdImage, x_adjusted, y_adjusted, signRequestParams.getSignWidth(), signRequestParams.getSignHeight());
            contentStream.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            pdDocument.setAllSecurityToBeRemoved(true);
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
            info.setCreator("GhostScript");
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

            PDFAIdentificationSchema pdfaid = xmpMetadata.createAndAddPFAIdentificationSchema();
            pdfaid.setConformance("B");
            pdfaid.setPart(pdfConfig.getPdfProperties().getPdfALevel());
            pdfaid.setAboutAsSimple("");

            XmpSerializer serializer = new XmpSerializer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serializer.serialize(xmpMetadata, baos, true);

            PDMetadata metadata = new PDMetadata(pdDocument);
            metadata.importXMPMetadata(baos.toByteArray());
            cat.setMetadata(metadata);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
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
            String cmd = pdfConfig.getPdfProperties().getPathToGS() + " -dPDFA=" + pdfConfig.getPdfProperties().getPdfALevel() + " -dBATCH -dNOPAUSE -dNOSAFER -sColorConversionStrategy=RGB -sDEVICE=pdfwrite -dPDFACompatibilityPolicy=1 -sOutputFile='" + targetFile.getAbsolutePath() + "' '" + defFile + "' '" + file.getAbsolutePath() + "'";
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
                    logger.debug(output.toString());
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
                                pdField.setValue(datas.get(filedName));
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
