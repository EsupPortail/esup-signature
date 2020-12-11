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
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.pdf.PdfService;
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

    public SignRequestParams createFromPdf(PDSignatureField pdSignatureField, List<Integer> annotationPages, PDPage pdPage) {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignImageNumber(0);
        signRequestParams.setPdSignatureFieldName(pdSignatureField.getPartialName());
        signRequestParams.setxPos((int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftX());
        signRequestParams.setyPos((int) pdPage.getBBox().getHeight() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getLowerLeftY() - (int) pdSignatureField.getWidgets().get(0).getRectangle().getHeight());
        signRequestParams.setSignPageNumber(annotationPages.get(0));
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

    public List<SignRequestParams> scanSignatureFields(InputStream inputStream) throws EsupSignatureIOException {
        try {
            PDDocument pdDocument = PDDocument.load(inputStream);
            List<SignRequestParams> signRequestParamses = pdSignatureFieldsToSignRequestParams(pdDocument);
            if(signRequestParamses.size() == 0) {
                SignRequestParams signRequestParams = SignRequest.getEmptySignRequestParams();
                signRequestParamses.add(signRequestParams);
            }
            for(SignRequestParams signRequestParams : signRequestParamses) {
                signRequestParamsRepository.save(signRequestParams);
            }

            return signRequestParamses;
        } catch (IOException e) {
            throw new EsupSignatureIOException("unable to open pdf document");
        }
    }

    public List<SignRequestParams> pdSignatureFieldsToSignRequestParams(PDDocument pdDocument) {
        List<SignRequestParams> signRequestParamsList = new ArrayList<>();
        try {
            PDDocumentCatalog docCatalog = pdDocument.getDocumentCatalog();
            Map<COSDictionary, Integer> pageNrByAnnotDict = pdfService.getPageNrByAnnotDict(docCatalog);
            PDAcroForm acroForm = docCatalog.getAcroForm();
            if(acroForm != null) {
                for (PDField pdField : acroForm.getFields()) {
                    if (pdField instanceof PDSignatureField) {
                        PDSignatureField pdSignatureField = (PDSignatureField) pdField;
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
        SignRequestParams signRequestParams = signRequest.getCurrentSignRequestParams();
        signRequestParams.setSignPageNumber(signRequestParamses.get(0).getSignPageNumber());
        signRequestParams.setxPos(signRequestParamses.get(0).getxPos());
        signRequestParams.setyPos(signRequestParamses.get(0).getyPos());
        signRequestParams.setSignWidth(signRequestParamses.get(0).getSignWidth());
        signRequestParams.setSignHeight(signRequestParamses.get(0).getSignHeight());
        signRequestParams.setAddExtra(signRequestParamses.get(0).isAddExtra());
    }
}
