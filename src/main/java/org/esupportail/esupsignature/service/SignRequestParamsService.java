package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignRequestParamsService {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestParamsService.class);


    @Resource
    private  SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private PdfService pdfService;

    @Resource
    private ObjectMapper objectMapper;

    public SignRequestParams getById(Long id) {
        return signRequestParamsRepository.findById(id).get();
    }

    public SignRequestParams createFromPdf(PDSignatureField pdSignatureField, List<Integer> annotationPages, PDPage pdPage) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(pdSignatureField.getPartialName());
//        int xPosCentered = (int) ((int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX() + ((int) pdSignatureField.getWidgets().get(0).getRectangle().getWidth() / 2) - (100 * 0.75));
        signRequestParams.setxPos(Math.round(pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX() / 0.75f));
        signRequestParams.setyPos(Math.round((pdPage.getBBox().getHeight() - pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - pdSignatureField.getWidgets().get(0).getRectangle().getHeight()) / .75f));
        signRequestParams.setSignPageNumber(annotationPages.get(0));
        signRequestParams.setSignWidth(Math.round(pdSignatureField.getWidgets().get(0).getRectangle().getWidth()));
        signRequestParams.setSignHeight(Math.round(pdSignatureField.getWidgets().get(0).getRectangle().getHeight()));
        signRequestParamsRepository.save(signRequestParams);
        return signRequestParams;
    }

    public List<SignRequestParams> getSignRequestParamsFromJson(String signRequestParamsJsonString) {
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        try {
            signRequestParamses = Arrays.asList(objectMapper.readValue(signRequestParamsJsonString, SignRequestParams[].class));
        } catch (JsonProcessingException e) {
            logger.warn("no signRequestParams returned");
        }
        return signRequestParamses;
    }

    public List<SignRequestParams> scanSignatureFields(InputStream inputStream, int docNumber) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            List<SignRequestParams> signRequestParamses = getSignRequestParamsFromPdf(pdDocument);
            for(SignRequestParams signRequestParams : signRequestParamses) {
                signRequestParams.setSignDocumentNumber(docNumber);
                signRequestParamsRepository.save(signRequestParams);
            }

            return signRequestParamses;
        } catch (IOException e) {
            throw new EsupSignatureIOException("unable to open pdf document");
        }
    }

    public List<SignRequestParams> getSignRequestParamsFromPdf(PDDocument pdDocument) {
        List<SignRequestParams> signRequestParamsList = new ArrayList<>();
        try {
            PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
            Map<COSDictionary, Integer> pageNrByAnnotDict = pdfService.getPageNrByAnnotDict(docCatalog);
            PDAcroForm acroForm = docCatalog.getAcroForm();
            if(acroForm != null) {
                for (PDField pdField : acroForm.getFields()) {
                    if (pdField instanceof PDSignatureField) {
                        PDSignatureField pdSignatureField = (PDSignatureField) pdField;
                        PDSignature pdSignature = pdSignatureField.getSignature();
                        if(pdSignature != null) { continue; }
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
                        SignRequestParams signRequestParams = createFromPdf(pdSignatureField, annotationPages, pdPage);
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

    public void copySignRequestParams(SignRequest signRequest, List<SignRequestParams> signRequestParamses) {
        List<SignRequestParams> liveWfSignRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
        for (int i = 0 ; i < signRequestParamses.size() ; i++) {
            if (liveWfSignRequestParams.size() < i + 1) {
                SignRequestParams signRequestParams = createSignRequestParams(signRequestParamses.get(i).getSignPageNumber(), signRequestParamses.get(i).getxPos(), signRequestParamses.get(i).getyPos());
                signRequestParams.setSignImageNumber(signRequestParamses.get(i).getSignImageNumber());
                signRequestParams.setSignPageNumber(signRequestParamses.get(i).getSignPageNumber());
                signRequestParams.setxPos(signRequestParamses.get(i).getxPos());
                signRequestParams.setyPos(signRequestParamses.get(i).getyPos());
                signRequestParams.setSignWidth(signRequestParamses.get(i).getSignWidth());
                signRequestParams.setSignHeight(signRequestParamses.get(i).getSignHeight());
                signRequestParams.setExtraWidth(signRequestParamses.get(i).getExtraWidth());
                signRequestParams.setExtraHeight(signRequestParamses.get(i).getExtraHeight());
                signRequestParams.setExtraType(signRequestParamses.get(i).getExtraType());
                signRequestParams.setExtraName(signRequestParamses.get(i).getExtraName());
                signRequestParams.setExtraDate(signRequestParamses.get(i).getExtraDate());
                signRequestParams.setExtraText(signRequestParamses.get(i).getExtraText());
                signRequestParams.setVisual(signRequestParamses.get(i).getVisual());
                signRequestParams.setAddExtra(signRequestParamses.get(i).getAddExtra());
                signRequestParams.setAddWatermark(signRequestParamses.get(i).getAddWatermark());
                signRequestParams.setAllPages(signRequestParamses.get(i).getAllPages());
                signRequestParams.setExtraOnTop(signRequestParamses.get(i).getExtraOnTop());
                liveWfSignRequestParams.add(signRequestParams);
            } else {
                liveWfSignRequestParams.get(i).setSignImageNumber(signRequestParamses.get(i).getSignImageNumber());
                liveWfSignRequestParams.get(i).setSignPageNumber(signRequestParamses.get(i).getSignPageNumber());
                liveWfSignRequestParams.get(i).setxPos(signRequestParamses.get(i).getxPos());
                liveWfSignRequestParams.get(i).setyPos(signRequestParamses.get(i).getyPos());
                liveWfSignRequestParams.get(i).setSignWidth(signRequestParamses.get(i).getSignWidth());
                liveWfSignRequestParams.get(i).setSignHeight(signRequestParamses.get(i).getSignHeight());
                liveWfSignRequestParams.get(i).setExtraWidth(signRequestParamses.get(i).getExtraWidth());
                liveWfSignRequestParams.get(i).setExtraHeight(signRequestParamses.get(i).getExtraHeight());
                liveWfSignRequestParams.get(i).setExtraType(signRequestParamses.get(i).getExtraType());
                liveWfSignRequestParams.get(i).setExtraName(signRequestParamses.get(i).getExtraName());
                liveWfSignRequestParams.get(i).setExtraDate(signRequestParamses.get(i).getExtraDate());
                liveWfSignRequestParams.get(i).setExtraText(signRequestParamses.get(i).getExtraText());
                liveWfSignRequestParams.get(i).setVisual(signRequestParamses.get(i).getVisual());
                liveWfSignRequestParams.get(i).setAddExtra(signRequestParamses.get(i).getAddExtra());
                liveWfSignRequestParams.get(i).setAddWatermark(signRequestParamses.get(i).getAddWatermark());
                liveWfSignRequestParams.get(i).setAllPages(signRequestParamses.get(i).getAllPages());
                liveWfSignRequestParams.get(i).setExtraOnTop(signRequestParamses.get(i).getExtraOnTop());
            }
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

}
