package org.esupportail.esupsignature.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.util.Matrix;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service gérant les paramètres de requêtes de signature sur des documents PDF.
 * Permet de récupérer, créer, et manipuler les paramètres des champs de signature.
 * Les fonctionnalités incluent la détection de champs de signature, leur gestion,
 * ainsi que le stockage ou la suppression des paramètres associés.
 *
 * Utilise principalement Apache PDFBox pour les manipulations des fichiers PDF.
 *
 * @author David Lemaignent
 */
@Service
public class SignRequestParamsService {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestParamsService.class);
    private static final int NO_MCID = Integer.MIN_VALUE;

    private final SignRequestParamsRepository signRequestParamsRepository;
    private final PdfService pdfService;
    private final GlobalProperties globalProperties;
    private final SignRequestRepository signRequestRepository;

    public SignRequestParamsService(SignRequestParamsRepository signRequestParamsRepository, PdfService pdfService, GlobalProperties globalProperties, SignRequestRepository signRequestRepository) {
        this.signRequestParamsRepository = signRequestParamsRepository;
        this.pdfService = pdfService;
        this.globalProperties = globalProperties;
        this.signRequestRepository = signRequestRepository;
    }

    /**
     * Récupère un SignRequestParams par son identifiant unique.
     *
     * @param id L'identifiant du paramètre de requête de signature
     * @return L'instance SignRequestParams correspondante
     * @throws RuntimeException Si aucun paramètre avec cet ID n'est trouvé
     */
    public SignRequestParams getById(Long id) {
        return signRequestParamsRepository.findById(id).orElseThrow();
    }

    /**
     * Récupère un champ de signature spécifique d'un fichier PDF.
     *
     * @param documentToSign Le fichier PDF (en tant que MultipartFile)
     * @param pdSignatureFieldName Le nom du champ de signature à rechercher
     * @return L'objet SignRequestParams correspondant au champ de signature
     */
    public SignRequestParams getSignatureField(MultipartFile documentToSign, String pdSignatureFieldName) {
        try (PDDocument pdDocument = Loader.loadPDF(documentToSign.getBytes())) {
            Map<String, Integer> pageNrByAnnotDict = pdfService.getPageNumberByAnnotDict(pdDocument);
            PDAcroForm pdAcroForm = pdDocument.getDocumentCatalog().getAcroForm();
            PDPageTree pdPages = pdDocument.getDocumentCatalog().getPages();
            if (pdAcroForm != null) {
                for (PDField pdField : pdAcroForm.getFields()) {
                    if (pdField instanceof PDSignatureField && pageNrByAnnotDict.get(pdSignatureFieldName) != null) {
                        if (pdField.getPartialName().equals(pdSignatureFieldName)) {
                            PDRectangle pdRectangle = pdField.getWidgets().get(0).getRectangle();
                            int pageNum = pageNrByAnnotDict.get(pdSignatureFieldName);
                            PDPage pdPage = pdPages.get(pageNum);
                            return createFromPdf(pdSignatureFieldName, pdRectangle, pageNum, pdPage, false);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("error on get signature field", e);
        }
        return null;
    }

    /**
     * Crée une instance de SignRequestParams à partir des données d'un champ PDF.
     *
     * @param name Le nom du champ
     * @param pdRectangle Les coordonnées du rectangle du champ
     * @param signPageNumber Le numéro de la page contenant le champ
     * @param pdPage La page PDF associée
     * @return Une instance de SignRequestParams configurée
     */
    public SignRequestParams createFromPdf(String name, PDRectangle pdRectangle, int signPageNumber, PDPage pdPage, boolean scaleDimensions) {
        SignRequestParams signRequestParams = new SignRequestParams();
        PDRectangle pageBox = pdPage.getCropBox();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(name);
        signRequestParams.setxPos(Math.round((pdRectangle.getLowerLeftX() - pageBox.getLowerLeftX()) / globalProperties.getFixFactor()));
        signRequestParams.setyPos(Math.round((pageBox.getLowerLeftY() + pageBox.getHeight() - pdRectangle.getLowerLeftY() - pdRectangle.getHeight()) / globalProperties.getFixFactor()));
        float scaleWidth = pdRectangle.getWidth() / 200f;
        float scaleHeight = pdRectangle.getHeight() / 100f;
        float scale = Math.min(scaleWidth, scaleHeight);
        signRequestParams.setSignScale(scale);
        if(scaleDimensions) {
            signRequestParams.setSignWidth(Math.round(pdRectangle.getWidth()));
            signRequestParams.setSignHeight(Math.round(pdRectangle.getHeight()));
        }
        signRequestParams.setSignPageNumber(signPageNumber);
        return signRequestParams;
    }

    /**
     * Analyse un document PDF pour détecter les champs de signature et retourne les paramètres associés.
     *
     * @param inputStream Le flux d'entrée contenant les données du document PDF
     * @param docNumber Le numéro du document
     * @param signRequestParamsDetectionPattern Pattern de détection des champs de signature
     * @param persist Indique si les paramètres doivent être enregistrés dans la base de données
     * @return Une liste de SignRequestParams détectés
     * @throws EsupSignatureIOException Si une erreur intervient lors de l'ouverture ou l'analyse du PDF
     */
    public List<SignRequestParams> scanSignatureFields(InputStream inputStream, OutputStream outputStream, int docNumber, String signRequestParamsDetectionPattern, boolean persist, boolean orderByName) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes());
            List<SignRequestParams> signRequestParamses = getSignRequestParamsFromPdf(pdDocument, signRequestParamsDetectionPattern, orderByName);
            for(SignRequestParams signRequestParams : signRequestParamses) {
                signRequestParams.setSignDocumentNumber(docNumber);
                if(persist) {
                    signRequestParamsRepository.save(signRequestParams);
                }
            }
            if(outputStream != null) {
                pdDocument.save(outputStream);
            }
            pdDocument.close();
            return signRequestParamses;
        } catch (IOException e) {
            throw new EsupSignatureIOException("unable to open pdf document");
        }
    }

    /**
     * Extrait tous les champs de signature d'un document PDF.
     *
     * @param pdDocument L'objet PDDocument représentant le PDF
     * @param signRequestParamsDetectionPattern Pattern de détéction des champs de signature
     * @return Une liste de SignRequestParams correspondants aux champs de signature
     * @throws EsupSignatureIOException Si des erreurs de lecture ou d'accès surviennent
     */
    public List<SignRequestParams> getSignRequestParamsFromPdf(PDDocument pdDocument, String signRequestParamsDetectionPattern, boolean orderByName) throws EsupSignatureIOException {
        List<SignRequestParams> signRequestParamsList = new ArrayList<>();
        try {
            PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
            PDPageTree pdPages = docCatalog.getPages();
            PDAcroForm acroForm = docCatalog.getAcroForm();
            if(acroForm != null) {
                Map<String, Integer> pageNrByAnnotDict = pdfService.getPageNumberByAnnotDict(pdDocument);
                for (PDField pdField : acroForm.getFields()) {
                    if (pdField instanceof PDSignatureField pdSignatureField) {
                        PDSignature pdSignature = pdSignatureField.getSignature();
                        if(pdSignature != null) {
                            continue;
                        }
                        String signFieldName = pdSignatureField.getPartialName();
                        if(pageNrByAnnotDict.containsKey(signFieldName) && pageNrByAnnotDict.get(signFieldName) != null) {
                            int pageNum = pageNrByAnnotDict.get(signFieldName);
                            PDPage pdPage = pdPages.get(pageNum);
                            PDRectangle signRect = pdSignatureField.getWidgets().get(0).getRectangle();
                            boolean isOverlapping = false;
                            for (PDAnnotation annotation : pdPage.getAnnotations()) {
                                if ("Sig".equals(annotation.getCOSObject().getNameAsString(COSName.FT))) {
                                    continue;
                                }
                                PDRectangle annotRect = annotation.getRectangle();
                                if (rectanglesOverlap(signRect, annotRect)) {
                                    isOverlapping = true;
                                    break;
                                }
                            }
                            if (!isOverlapping) {
                                SignRequestParams signRequestParams = createFromPdf(signFieldName, signRect, pageNrByAnnotDict.get(signFieldName) + 1, pdPage, true);
                                signRequestParamsList.add(signRequestParams);
                            } else {
                                logger.warn("Signature field " + signFieldName + " is overlapping with another annotation");
                            }
                        }
                    }
                    if(StringUtils.hasText(signRequestParamsDetectionPattern)) {
                        String className = "org.apache.pdfbox.pdmodel.interactive.form.PD" + extractTextInBrackets(signRequestParamsDetectionPattern);
                        try {
                            Class<?> pdFieldClass = Class.forName(className);
                            if (pdFieldClass.isInstance(pdField)) {
                                Method getPartialNameMethod = pdFieldClass.getMethod("getPartialName");
                                String signFieldName = (String) getPartialNameMethod.invoke(pdField);
                                Pattern pattern = Pattern.compile(signRequestParamsDetectionPattern.split("]")[1], Pattern.CASE_INSENSITIVE);
                                if (pattern.matcher(signFieldName).find()) {
                                    int pageNum = pageNrByAnnotDict.get(signFieldName);
                                    PDPage pdPage = pdPages.get(pageNum);
                                    Method getWidgetsMethod = pdFieldClass.getMethod("getWidgets");
                                    List<?> widgets = (List<?>) getWidgetsMethod.invoke(pdField);
                                    Method getRectangleMethod = widgets.get(0).getClass().getMethod("getRectangle");
                                    Object rectangle = getRectangleMethod.invoke(widgets.get(0));
                                    SignRequestParams signRequestParams = createFromPdf(signFieldName, (PDRectangle) rectangle, pageNrByAnnotDict.get(signFieldName) + 1, pdPage, false);
                                    signRequestParamsList.add(signRequestParams);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            logger.debug("error on get sign fields");
                        }
                    }
                }
            }
            if(StringUtils.hasText(signRequestParamsDetectionPattern) && signRequestParamsDetectionPattern.contains("AnnotationLink")) {
                int i = 1;
                for(PDPage pdPage : pdPages) {
                    List<PDAnnotation> pdAnnotations = pdPage.getAnnotations();
                    for (PDAnnotation pdAnnotation : pdAnnotations) {
                        if (pdAnnotation instanceof PDAnnotationLink pdAnnotationLink) {
                            if (pdAnnotationLink.getAction() instanceof PDActionURI pdActionURI) {
                                String signFieldName = pdActionURI.getURI();
                                Pattern pattern = Pattern.compile(signRequestParamsDetectionPattern.split("]")[1], Pattern.CASE_INSENSITIVE);
                                if (pattern.matcher(signFieldName).find()) {
                                    PDRectangle originalPdRectangle = pdAnnotationLink.getRectangle();
                                    PDRectangle pdRectangle = new PDRectangle(originalPdRectangle.getUpperRightX() - originalPdRectangle.getWidth(), originalPdRectangle.getUpperRightY() - 75, 150, 75);
                                    SignRequestParams signRequestParams = createFromPdf(signFieldName, pdRectangle, i, pdPage, true);
                                    signRequestParamsList.add(signRequestParams);
                                }
                            }
                        }
                    }
                    i++;
                }
            }

            if(StringUtils.hasText(signRequestParamsDetectionPattern) && signRequestParamsDetectionPattern.contains("Image")) {
                Map<Integer, String> mcidToAltText = new HashMap<>();
                PDStructureTreeRoot structureTreeRoot = docCatalog.getStructureTreeRoot();
                if(structureTreeRoot != null) {
                    List<?> rootKids = structureTreeRoot.getKids();
                    if(rootKids != null) {
                        for(Object kid : rootKids) {
                            if(kid instanceof PDStructureElement) {
                                buildMcidToAltTextMap((PDStructureElement) kid, mcidToAltText);
                            }
                        }
                    }
                }

                logger.info("MCID to AltText map: " + mcidToAltText);

                Pattern pattern = Pattern.compile(signRequestParamsDetectionPattern.split("]")[1], Pattern.CASE_INSENSITIVE);
                int i = 1;

                for(PDPage pdPage : pdPages) {
                    try {
                        PDResources resources = pdPage.getResources();
                        if(resources != null) {
                            List<ImageOccurrence> imageOccurrences = findImageOccurrences(pdPage);
                            Set<String> imagesToRemove = new HashSet<>();
                            Set<Integer> imageTokenIndexesToRemove = new HashSet<>();

                            for(ImageOccurrence imageOccurrence : imageOccurrences) {
                                if(imageOccurrence.getMcid() == null) {
                                    continue;
                                }
                                String altText = mcidToAltText.get(imageOccurrence.getMcid());
                                if(altText != null && pattern.matcher(altText).find()) {
                                    SignRequestParams signRequestParams = createFromPdf(imageOccurrence.getImageName(), imageOccurrence.getRectangle(), i, pdPage, true);
                                    signRequestParamsList.add(signRequestParams);
                                    imagesToRemove.add(imageOccurrence.getImageName());
                                    imageTokenIndexesToRemove.add(imageOccurrence.getDoTokenIndex());
                                    break;
                                }
                            }

                            if(!imageTokenIndexesToRemove.isEmpty()) {
                                try {
                                    PDFStreamParser parser = new PDFStreamParser(pdPage);
                                    List<?> tokens = parser.parse();
                                    List<Object> newTokens = new ArrayList<>();

                                    for(int t = 0; t < tokens.size(); t++) {
                                        Object token = tokens.get(t);
                                        boolean skip = false;

                                        if(token instanceof Operator && "Do".equals(((Operator) token).getName()) && imageTokenIndexesToRemove.contains(t)) {
                                            if(!newTokens.isEmpty() && newTokens.get(newTokens.size() - 1) instanceof COSName) {
                                                newTokens.remove(newTokens.size() - 1);
                                            }
                                            skip = true;
                                        }

                                        if(!skip) {
                                            newTokens.add(token);
                                        }
                                    }

                                    PDStream newStream = new PDStream(pdDocument);
                                    try(OutputStream os = newStream.createOutputStream()) {
                                        ContentStreamWriter writer = new ContentStreamWriter(os);
                                        writer.writeTokens(newTokens);
                                    }
                                    pdPage.setContents(newStream);

                                    // Supprimer les ressources
                                    COSDictionary resDictionary = resources.getCOSObject();
                                    COSDictionary xObjects = (COSDictionary) resDictionary.getDictionaryObject(COSName.XOBJECT);
                                    if(xObjects != null) {
                                        for(String imageName : imagesToRemove) {
                                            if(!hasImageReference(newTokens, imageName)) {
                                                xObjects.removeItem(COSName.getPDFName(imageName));
                                            }
                                        }
                                    }
                                } catch(IOException e) {
                                    logger.warn("Error removing images from stream", e);
                                }
                            }

                        }
                    } catch (IOException e) {
                        logger.warn("Error processing images on page " + i, e);
                    }
                    i++;
                }
            }

//            pdDocument.close();
        } catch (Exception e) {
            logger.error("error on get sign fields", e);
            throw new EsupSignatureIOException(e.getMessage(), e);
        }
        if(orderByName) {
            return signRequestParamsList.stream()
                    .sorted(Comparator
                            .comparing(SignRequestParams::getPdSignatureFieldName,
                                    Comparator.nullsLast(String::compareTo))
                            .thenComparingInt(SignRequestParams::getxPos)
                            .thenComparingInt(SignRequestParams::getyPos)
                            .thenComparingInt(SignRequestParams::getSignPageNumber))
                    .collect(Collectors.toList());
        } else {
            return signRequestParamsList.stream().sorted(Comparator.comparingInt(SignRequestParams::getxPos)).sorted(Comparator.comparingInt(SignRequestParams::getyPos)).sorted(Comparator.comparingInt(SignRequestParams::getSignPageNumber)).collect(Collectors.toList());
        }
    }

    private List<ImageOccurrence> findImageOccurrences(PDPage pdPage) throws IOException {
        PDFStreamParser parser = new PDFStreamParser(pdPage);
        List<?> tokens = parser.parse();
        List<ImageOccurrence> imageOccurrences = new ArrayList<>();
        List<COSBase> arguments = new ArrayList<>();
        Deque<Matrix> matrixStack = new ArrayDeque<>();
        Deque<Integer> mcidStack = new ArrayDeque<>();
        Matrix currentMatrix = new Matrix();

        for(int t = 0; t < tokens.size(); t++) {
            Object token = tokens.get(t);
            if(token instanceof COSBase) {
                arguments.add((COSBase) token);
                continue;
            }
            if(!(token instanceof Operator operator)) {
                arguments.clear();
                continue;
            }

            switch(operator.getName()) {
                case "q":
                    matrixStack.push(currentMatrix.clone());
                    break;
                case "Q":
                    currentMatrix = matrixStack.isEmpty() ? new Matrix() : matrixStack.pop();
                    break;
                case "cm":
                    if(arguments.size() == 6 && arguments.stream().allMatch(COSNumber.class::isInstance)) {
                        currentMatrix.concatenate(new Matrix(
                                ((COSNumber) arguments.get(0)).floatValue(),
                                ((COSNumber) arguments.get(1)).floatValue(),
                                ((COSNumber) arguments.get(2)).floatValue(),
                                ((COSNumber) arguments.get(3)).floatValue(),
                                ((COSNumber) arguments.get(4)).floatValue(),
                                ((COSNumber) arguments.get(5)).floatValue()
                        ));
                    }
                    break;
                case "BMC":
                    mcidStack.push(NO_MCID);
                    break;
                case "BDC":
                    Integer mcid = extractMcid(arguments);
                    mcidStack.push(mcid != null ? mcid : NO_MCID);
                    break;
                case "EMC":
                    if(!mcidStack.isEmpty()) {
                        mcidStack.pop();
                    }
                    break;
                case "Do":
                    if(arguments.size() == 1 && arguments.get(0) instanceof COSName xObjectName) {
                        PDXObject xObject = pdPage.getResources().getXObject(xObjectName);
                        if(xObject instanceof PDImageXObject) {
                            Integer currentMcid = getCurrentMcid(mcidStack);
                            PDRectangle pdRectangle = getRectangleFromMatrix(currentMatrix);
                            imageOccurrences.add(new ImageOccurrence(xObjectName.getName(), currentMcid, pdRectangle, t));
                            logger.info("Found image {} with MCID {} at {}", xObjectName.getName(), currentMcid, pdRectangle);
                        }
                    }
                    break;
                default:
                    break;
            }

            arguments.clear();
        }
        return imageOccurrences;
    }

    private void buildMcidToAltTextMap(PDStructureElement elem, Map<Integer, String> map) {
        String altText = elem.getAlternateDescription();
        List<?> kids = elem.getKids();
        if(kids != null && altText != null) {
            for(Object kid : kids) {
                if(kid instanceof Integer) {
                    int mcid = (Integer) kid;
                    map.put(mcid, altText);
                    logger.info("Mapped MCID " + mcid + " -> " + altText);
                }
            }
        }
        if(kids != null) {
            for(Object kid : kids) {
                if(kid instanceof PDStructureElement) {
                    buildMcidToAltTextMap((PDStructureElement) kid, map);
                }
            }
        }
    }

    private void extractAltTextFromStructure(COSBase kBase, Map<Integer, String> mcidToAltText) {
        if(kBase instanceof COSArray) {
            COSArray array = (COSArray) kBase;
            for(int i = 0; i < array.size(); i++) {
                COSBase item = array.get(i);
                if(item instanceof COSDictionary) {
                    procesStructElement((COSDictionary) item, mcidToAltText);
                }
            }
        } else if(kBase instanceof COSDictionary) {
            procesStructElement((COSDictionary) kBase, mcidToAltText);
        }
    }

    private void procesStructElement(COSDictionary elemDict, Map<Integer, String> mcidToAltText) {
        COSBase altBase = elemDict.getDictionaryObject(COSName.ALT);
        String altText = null;
        if(altBase instanceof COSString) {
            altText = ((COSString) altBase).getString();
        }
        COSBase kBase = elemDict.getDictionaryObject(COSName.K);
        if(kBase instanceof COSArray && altText != null) {
            COSArray kArray = (COSArray) kBase;
            for(int i = 0; i < kArray.size(); i++) {
                COSBase item = kArray.get(i);
                if(item instanceof COSInteger) {
                    mcidToAltText.put(((COSInteger) item).intValue(), altText);
                }
            }
        }
        COSBase childrenBase = elemDict.getDictionaryObject(COSName.K);
        if(childrenBase instanceof COSArray) {
            extractAltTextFromStructure(childrenBase, mcidToAltText);
        }
    }

    private Integer extractMcid(List<COSBase> arguments) {
        if(arguments.size() < 2 || !(arguments.get(1) instanceof COSDictionary dict)) {
            return null;
        }
        COSBase mcidValue = dict.getDictionaryObject(COSName.MCID);
        if(mcidValue instanceof COSInteger) {
            return ((COSInteger) mcidValue).intValue();
        }
        return null;
    }

    private Integer getCurrentMcid(Deque<Integer> mcidStack) {
        for(Integer mcid : mcidStack) {
            if(mcid != null && mcid != NO_MCID) {
                return mcid;
            }
        }
        return null;
    }

    private PDRectangle getRectangleFromMatrix(Matrix matrix) {
        Point2D.Float p1 = matrix.transformPoint(0, 0);
        Point2D.Float p2 = matrix.transformPoint(1, 0);
        Point2D.Float p3 = matrix.transformPoint(0, 1);
        Point2D.Float p4 = matrix.transformPoint(1, 1);

        float minX = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        float maxX = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
        float minY = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        float maxY = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));

        return new PDRectangle(minX, minY, maxX - minX, maxY - minY);
    }

    private boolean hasImageReference(List<Object> tokens, String imageName) {
        for(int t = 1; t < tokens.size(); t++) {
            Object token = tokens.get(t);
            if(token instanceof Operator && "Do".equals(((Operator) token).getName())) {
                Object previous = tokens.get(t - 1);
                if(previous instanceof COSName && imageName.equals(((COSName) previous).getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final class ImageOccurrence {
        private final String imageName;
        private final Integer mcid;
        private final PDRectangle rectangle;
        private final int doTokenIndex;

        private ImageOccurrence(String imageName, Integer mcid, PDRectangle rectangle, int doTokenIndex) {
            this.imageName = imageName;
            this.mcid = mcid;
            this.rectangle = rectangle;
            this.doTokenIndex = doTokenIndex;
        }

        private String getImageName() {
            return imageName;
        }

        private Integer getMcid() {
            return mcid;
        }

        private PDRectangle getRectangle() {
            return rectangle;
        }

        private int getDoTokenIndex() {
            return doTokenIndex;
        }
    }

    private boolean rectanglesOverlap(PDRectangle rect1, PDRectangle rect2) {
        return rect1.getLowerLeftX() < rect2.getUpperRightX() &&
                rect1.getUpperRightX() > rect2.getLowerLeftX() &&
                rect1.getLowerLeftY() < rect2.getUpperRightY() &&
                rect1.getUpperRightY() > rect2.getLowerLeftY();
    }

    private String extractTextInBrackets(String input) {
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Copie les paramètres de requête de signature d'une liste vers une signRequest.
     *
     * @param signRequestId La requête de signature cible
     * @param signRequestParamses La liste des paramètres de requête de signature à copier
     */
    @Transactional
    public void copySignRequestParams(Long signRequestId, List<SignRequestParams> signRequestParamses) {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        signRequest.getSignRequestParams().clear();
        for (SignRequestParams requestParams : signRequestParamses) {
            SignRequestParams signRequestParams = createSignRequestParams(requestParams.getSignPageNumber(), requestParams.getxPos(), requestParams.getyPos());
            signRequestParams.setSignImageNumber(requestParams.getSignImageNumber());
            signRequestParams.setPdSignatureFieldName(requestParams.getPdSignatureFieldName());
            signRequestParams.setRotate(requestParams.getRotate());
            signRequestParams.setSignScale(requestParams.getSignScale());
            signRequestParams.setSignWidth(requestParams.getSignWidth());
            signRequestParams.setSignHeight(requestParams.getSignHeight());
            signRequestParams.setExtraType(requestParams.getExtraType());
            signRequestParams.setExtraName(requestParams.getExtraName());
            signRequestParams.setExtraDate(requestParams.getExtraDate());
            signRequestParams.setExtraText(requestParams.getExtraText());
            signRequestParams.setIsExtraText(requestParams.getIsExtraText());
            signRequestParams.setAddExtra(requestParams.getAddExtra());
            signRequestParams.setTextPart(requestParams.getTextPart());
            signRequestParams.setAddWatermark(requestParams.getAddWatermark());
            signRequestParams.setAllPages(requestParams.getAllPages());
            signRequestParams.setExtraOnTop(requestParams.getExtraOnTop());
            signRequestParams.setFontSize(requestParams.getFontSize());
            signRequestParams.setRecipient(requestParams.getRecipient());
            signRequest.getSignRequestParams().add(signRequestParams);
        }
    }

    /**
     * Crée et sauvegarde une nouvelle instance de SignRequestParams.
     *
     * @param signPageNumber Le numéro de page de la signature
     * @param xPos La position X du champ de signature
     * @param yPos La position Y du champ de signature
     * @return L'instance nouvellement créée de SignRequestParams
     */
    public SignRequestParams createSignRequestParams(Integer signPageNumber, Integer xPos, Integer yPos) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignPageNumber(signPageNumber);
        if(xPos != null && yPos != null) {
            signRequestParams.setxPos(xPos);
            signRequestParams.setyPos(yPos);
        }
        signRequestParamsRepository.save(signRequestParams);
        return signRequestParams;
    }

    /**
     * Supprime un SignRequestParams de la base de données.
     *
     * @param id L'identifiant du SignRequestParams à supprimer
     */
    @Transactional
    public void delete(Long id) {
        SignRequestParams signRequestParams = getById(id);
        signRequestParamsRepository.delete(signRequestParams);
    }

}
