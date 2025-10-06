package org.esupportail.esupsignature.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
                    if (pdField instanceof PDSignatureField) {
                        if (pdField.getPartialName().equals(pdSignatureFieldName)) {
                            PDRectangle pdRectangle = pdField.getWidgets().get(0).getRectangle();
                            int pageNum = pageNrByAnnotDict.get(pdSignatureFieldName);
                            PDPage pdPage = pdPages.get(pageNum);
                            return createFromPdf(pdSignatureFieldName, pdRectangle, pageNum, pdPage);
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
    public SignRequestParams createFromPdf(String name, PDRectangle pdRectangle, int signPageNumber, PDPage pdPage) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(name);
        signRequestParams.setxPos(Math.round(pdRectangle.getLowerLeftX() / globalProperties.getFixFactor()));
        signRequestParams.setyPos(Math.round((pdPage.getBBox().getHeight() - pdRectangle.getLowerLeftY() - pdRectangle.getHeight()) / globalProperties.getFixFactor()));
        signRequestParams.setSignWidth(Math.round(pdRectangle.getWidth()));
        signRequestParams.setSignHeight(Math.round(pdRectangle.getHeight()));
        signRequestParams.setSignPageNumber(signPageNumber);
        return signRequestParams;
    }

    /**
     * Analyse un document PDF pour détecter les champs de signature et retourne les paramètres associés.
     *
     * @param inputStream Le flux d'entrée contenant les données du document PDF
     * @param docNumber Le numéro du document
     * @param workflow Le workflow associé pour les validations
     * @param persist Indique si les paramètres doivent être enregistrés dans la base de données
     * @return Une liste de SignRequestParams détectés
     * @throws EsupSignatureIOException Si une erreur intervient lors de l'ouverture ou l'analyse du PDF
     */
    public List<SignRequestParams> scanSignatureFields(InputStream inputStream, int docNumber, Workflow workflow, boolean persist) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes());
            List<SignRequestParams> signRequestParamses = getSignRequestParamsFromPdf(pdDocument, workflow);
            for(SignRequestParams signRequestParams : signRequestParamses) {
                signRequestParams.setSignDocumentNumber(docNumber);
                if(persist) {
                    signRequestParamsRepository.save(signRequestParams);
                }
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
     * @param workflow Le workflow à utiliser pour certaines validations
     * @return Une liste de SignRequestParams correspondants aux champs de signature
     * @throws EsupSignatureIOException Si des erreurs de lecture ou d'accès surviennent
     */
    public List<SignRequestParams> getSignRequestParamsFromPdf(PDDocument pdDocument, Workflow workflow) throws EsupSignatureIOException {
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
                                SignRequestParams signRequestParams = createFromPdf(
                                        signFieldName, signRect, pageNrByAnnotDict.get(signFieldName) + 1, pdPage
                                );
                                signRequestParamsList.add(signRequestParams);
                            } else {
                                logger.warn("Signature field " + signFieldName + " is overlapping with another annotation");
                            }
                        }
                    }
                    if(workflow != null && StringUtils.hasText(workflow.getSignRequestParamsDetectionPattern())) {
                        String className = "org.apache.pdfbox.pdmodel.interactive.form.PD" + extractTextInBrackets(workflow.getSignRequestParamsDetectionPattern());
                        try {
                            Class<?> pdFieldClass = Class.forName(className);
                            if (pdFieldClass.isInstance(pdField)) {
                                Method getPartialNameMethod = pdFieldClass.getMethod("getPartialName");
                                String signFieldName = (String) getPartialNameMethod.invoke(pdField);
                                Pattern pattern = Pattern.compile(workflow.getSignRequestParamsDetectionPattern().split("]")[1], Pattern.CASE_INSENSITIVE);
                                if (pattern.matcher(signFieldName).find()) {
                                    int pageNum = pageNrByAnnotDict.get(signFieldName);
                                    PDPage pdPage = pdPages.get(pageNum);
                                    Method getWidgetsMethod = pdFieldClass.getMethod("getWidgets");
                                    List<?> widgets = (List<?>) getWidgetsMethod.invoke(pdField);
                                    Method getRectangleMethod = widgets.get(0).getClass().getMethod("getRectangle");
                                    Object rectangle = getRectangleMethod.invoke(widgets.get(0));
                                    SignRequestParams signRequestParams = createFromPdf(signFieldName, (PDRectangle) rectangle, pageNrByAnnotDict.get(signFieldName) + 1, pdPage);
                                    signRequestParamsList.add(signRequestParams);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            logger.debug("error on get sign fields");
                        }
                    }
                }
            }
            if(workflow != null && StringUtils.hasText(workflow.getSignRequestParamsDetectionPattern()) && workflow.getSignRequestParamsDetectionPattern().contains("AnnotationLink")) {
                int i = 1;
                for(PDPage pdPage : pdPages) {
                    List<PDAnnotation> pdAnnotations = pdPage.getAnnotations();
                    for (PDAnnotation pdAnnotation : pdAnnotations) {
                        if (pdAnnotation instanceof PDAnnotationLink pdAnnotationLink) {
                            if (pdAnnotationLink.getAction() instanceof PDActionURI pdActionURI) {
                                String signFieldName = pdActionURI.getURI();
                                Pattern pattern = Pattern.compile(workflow.getSignRequestParamsDetectionPattern().split("]")[1], Pattern.CASE_INSENSITIVE);
                                if (pattern.matcher(signFieldName).find()) {
                                    PDRectangle originalPdRectangle = pdAnnotationLink.getRectangle();
                                    PDRectangle pdRectangle = new PDRectangle(originalPdRectangle.getUpperRightX() - originalPdRectangle.getWidth(), originalPdRectangle.getUpperRightY() - 50, 100, 50);
                                    SignRequestParams signRequestParams = createFromPdf(signFieldName, pdRectangle, i, pdPage);
                                    signRequestParamsList.add(signRequestParams);
                                }
                            }
                        }
                    }
                    i++;
                }
            }
            pdDocument.close();
        } catch (Exception e) {
            logger.error("error on get sign fields", e);
            throw new EsupSignatureIOException(e.getMessage(), e);
        }
        return signRequestParamsList.stream().sorted(Comparator.comparingInt(SignRequestParams::getxPos)).sorted(Comparator.comparingInt(SignRequestParams::getyPos)).sorted(Comparator.comparingInt(SignRequestParams::getSignPageNumber)).collect(Collectors.toList());
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
            signRequestParams.setSignScale(requestParams.getSignScale());
            signRequestParams.setSignWidth(requestParams.getSignWidth());
            signRequestParams.setSignHeight(requestParams.getSignHeight());
            signRequestParams.setExtraType(requestParams.getExtraType());
            signRequestParams.setExtraName(requestParams.getExtraName());
            signRequestParams.setExtraDate(requestParams.getExtraDate());
            signRequestParams.setExtraText(requestParams.getExtraText());
            signRequestParams.setAddExtra(requestParams.getAddExtra());
            signRequestParams.setTextPart(requestParams.getTextPart());
            signRequestParams.setAddWatermark(requestParams.getAddWatermark());
            signRequestParams.setAllPages(requestParams.getAllPages());
            signRequestParams.setExtraOnTop(requestParams.getExtraOnTop());
            signRequestParams.setFontSize(requestParams.getFontSize());
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
