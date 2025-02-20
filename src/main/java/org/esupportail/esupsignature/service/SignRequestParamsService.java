package org.esupportail.esupsignature.service;

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
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

@Service
public class SignRequestParamsService {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestParamsService.class);

    private final SignRequestParamsRepository signRequestParamsRepository;

    private final PdfService pdfService;

    private final GlobalProperties globalProperties;

    public SignRequestParamsService(SignRequestParamsRepository signRequestParamsRepository, PdfService pdfService, GlobalProperties globalProperties) {
        this.signRequestParamsRepository = signRequestParamsRepository;
        this.pdfService = pdfService;
        this.globalProperties = globalProperties;
    }

    public SignRequestParams getById(Long id) {
        return signRequestParamsRepository.findById(id).orElseThrow();
    }

    public SignRequestParams createFromPdf(String name, PDRectangle pdRectangle, int signPageNumber, PDPage pdPage) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(name);
        signRequestParams.setxPos(Math.round(pdRectangle.getLowerLeftX() / globalProperties.getFixFactor()));
        signRequestParams.setyPos(Math.round((pdPage.getBBox().getHeight() - pdRectangle.getLowerLeftY() - pdRectangle.getHeight()) / globalProperties.getFixFactor()));
        signRequestParams.setSignPageNumber(signPageNumber);
        return signRequestParams;
    }

    public List<SignRequestParams> scanSignatureFields(InputStream inputStream, int docNumber, Workflow workflow, boolean persist) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
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
                            logger.warn("error on get sign fields");
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
                                    SignRequestParams signRequestParams = createFromPdf(signFieldName, pdAnnotationLink.getRectangle(), i, pdPage);
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

    public void copySignRequestParams(SignRequest signRequest, List<SignRequestParams> signRequestParamses) {
        for (int i = 0 ; i < signRequestParamses.size() ; i++) {
            SignRequestParams signRequestParams;
            if (signRequest.getSignRequestParams().size() >= i + 1) {
                signRequestParams = signRequest.getSignRequestParams().get(i);
            } else {
                signRequestParams = createSignRequestParams(signRequestParamses.get(i).getSignPageNumber(), signRequestParamses.get(i).getxPos(), signRequestParamses.get(i).getyPos());
            }
            signRequestParams.setSignImageNumber(signRequestParamses.get(i).getSignImageNumber());
            signRequestParams.setSignPageNumber(signRequestParamses.get(i).getSignPageNumber());
            signRequestParams.setSignScale(signRequestParamses.get(i).getSignScale());
            signRequestParams.setxPos(signRequestParamses.get(i).getxPos());
            signRequestParams.setyPos(signRequestParamses.get(i).getyPos());
            signRequestParams.setSignWidth(signRequestParamses.get(i).getSignWidth());
            signRequestParams.setSignHeight(signRequestParamses.get(i).getSignHeight());
            signRequestParams.setExtraType(signRequestParamses.get(i).getExtraType());
            signRequestParams.setExtraName(signRequestParamses.get(i).getExtraName());
            signRequestParams.setExtraDate(signRequestParamses.get(i).getExtraDate());
            signRequestParams.setExtraText(signRequestParamses.get(i).getExtraText());
            signRequestParams.setAddExtra(signRequestParamses.get(i).getAddExtra());
            signRequestParams.setTextPart(signRequestParamses.get(i).getTextPart());
            signRequestParams.setAddWatermark(signRequestParamses.get(i).getAddWatermark());
            signRequestParams.setAllPages(signRequestParamses.get(i).getAllPages());
            signRequestParams.setExtraOnTop(signRequestParamses.get(i).getExtraOnTop());
            if (signRequest.getSignRequestParams().size() < i + 1) {
                signRequest.getSignRequestParams().add(signRequestParams);
            }
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().size() >= i + 1) {
                signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().remove(i);
            }
            signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().add(signRequestParams);
        }
    }

    public SignRequestParams createSignRequestParams(Integer signPageNumber, Integer xPos, Integer yPos) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignPageNumber(signPageNumber);
        signRequestParams.setxPos(xPos);
        signRequestParams.setyPos(yPos);
        signRequestParamsRepository.save(signRequestParams);
        return signRequestParams;
    }

    @Transactional
    public void delete(Long id) {
        SignRequestParams signRequestParams = getById(id);
        signRequestParamsRepository.delete(signRequestParams);
    }

}
