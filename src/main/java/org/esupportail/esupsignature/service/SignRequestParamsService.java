package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SignRequestParamsService {

    private static final Logger logger = LoggerFactory.getLogger(SignRequestParamsService.class);


    @Resource
    private  SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private PdfService pdfService;

    @Resource
    private ObjectMapper objectMapper;

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
        List<SignRequestParams> signRequestParamses = pdfService.scanSignatureFields(inputStream);
        if(signRequestParamses.size() == 0) {
            SignRequestParams signRequestParams = SignRequest.getEmptySignRequestParams();
            signRequestParamses.add(signRequestParams);
        }
        for(SignRequestParams signRequestParams : signRequestParamses) {
            signRequestParamsRepository.save(signRequestParams);
        }
        return signRequestParamses;
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
