package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;

@Service
public class SignRequestParamsService {

    @Resource
    private  SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private PdfService pdfService;

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
