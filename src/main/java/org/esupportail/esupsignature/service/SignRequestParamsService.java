package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public SignRequestParams createFromPdf(PDTerminalField pdSignatureField, int signPageNumber, PDPage pdPage) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(pdSignatureField.getPartialName());
//        int xPosCentered = (int) ((int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX() + ((int) pdSignatureField.getWidgets().get(0).getRectangle().getWidth() / 2) - (100 * 0.75));
        signRequestParams.setxPos(Math.round(pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX() / 0.75f));
        signRequestParams.setyPos(Math.round((pdPage.getBBox().getHeight() - pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - pdSignatureField.getWidgets().get(0).getRectangle().getHeight()) / .75f));
        signRequestParams.setSignPageNumber(signPageNumber);
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
            logger.warn("no signRequestParams returned", e);
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

    public List<SignRequestParams> getSignRequestParamsFromPdf(PDDocument pdDocument) throws EsupSignatureIOException {
        List<SignRequestParams> signRequestParamsList = new ArrayList<>();
        try {
            PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
            PDPageTree pdPages = docCatalog.getPages();
            PDAcroForm acroForm = docCatalog.getAcroForm();
            if(acroForm != null) {
                Map<String, Integer> pageNrByAnnotDict = pdfService.getPageNumberByAnnotDict(pdDocument);
                for (PDField pdField : acroForm.getFields()) {
                    if (pdField instanceof PDSignatureField) {
                        PDSignatureField pdSignatureField = (PDSignatureField) pdField;
                        PDSignature pdSignature = pdSignatureField.getSignature();
                        if(pdSignature != null) {
                            continue;
                        }
                        String signFieldName = pdSignatureField.getPartialName();
                        int pageNum = pageNrByAnnotDict.get(signFieldName);
                        PDPage pdPage = pdPages.get(pageNum);
                        SignRequestParams signRequestParams = createFromPdf(pdSignatureField, pageNrByAnnotDict.get(signFieldName) + 1, pdPage);
                        signRequestParamsList.add(signRequestParams);
                    }
                    if(pdField instanceof PDPushButton) {
                        PDPushButton pdSignatureField = (PDPushButton) pdField;
                        String signFieldName = pdSignatureField.getPartialName();
                        if(signFieldName.toLowerCase(Locale.ROOT).startsWith("signature")) {
                            int pageNum = pageNrByAnnotDict.get(signFieldName);
                            PDPage pdPage = pdPages.get(pageNum);
                            SignRequestParams signRequestParams = createFromPdf(pdSignatureField, pageNrByAnnotDict.get(signFieldName) + 1, pdPage);
                            signRequestParamsList.add(signRequestParams);
                        }
                    }
                }
            }
            pdDocument.close();
        } catch (Exception e) {
            logger.error("error on get sign fields", e);
            throw new EsupSignatureIOException(e.getMessage());
        }
        return signRequestParamsList.stream().sorted(Comparator.comparingInt(SignRequestParams::getxPos)).sorted(Comparator.comparingInt(SignRequestParams::getyPos)).sorted(Comparator.comparingInt(SignRequestParams::getSignPageNumber)).collect(Collectors.toList());
    }

    public void copySignRequestParams(SignRequest signRequest, List<SignRequestParams> signRequestParamses) {
        List<SignRequestParams> liveWfSignRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
        for (int i = 0 ; i < signRequestParamses.size() ; i++) {
            if (liveWfSignRequestParams.size() < i + 1) {
                SignRequestParams signRequestParams = createSignRequestParams(signRequestParamses.get(i).getSignPageNumber(), signRequestParamses.get(i).getxPos(), signRequestParamses.get(i).getyPos());
                signRequestParams.setSignImageNumber(signRequestParamses.get(i).getSignImageNumber());
                signRequestParams.setSignPageNumber(signRequestParamses.get(i).getSignPageNumber());
                signRequestParams.setSignScale(signRequestParamses.get(i).getSignScale());
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

    @Transactional
    public void delete(Long id) {
        SignRequestParams signRequestParams = getById(id);
        signRequestParamsRepository.delete(signRequestParams);
    }

}
