package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SignBookSigningService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookSigningService.class);

    private final SignBookService signBookService;
    private final ObjectMapper objectMapper;

    public SignBookSigningService(SignBookService signBookService, ObjectMapper objectMapper) {
        this.signBookService = signBookService;
        this.objectMapper = objectMapper;
    }

    public StepStatus initSign(Long signRequestId, String signRequestParamsJsonString, String comment,
                               String formData, String password, String signWith, String sealCertificat,
                               Long userShareId, String userEppn, String authUserEppn, boolean keepSignFields)
            throws IOException, EsupSignatureRuntimeException {
        StepStatus stepStatus = signBookService.initSign(signRequestId, signRequestParamsJsonString, comment,
                formData, password, signWith, sealCertificat, userShareId, userEppn, authUserEppn, keepSignFields);
        if(StepStatus.completed.equals(stepStatus)) {
            pendingAutoSign(signRequestId, userEppn, authUserEppn);
        }
        return stepStatus;
    }

    public String initMassSign(String userEppn, String authUserEppn, String ids, HttpSession httpSession,
                               String password, String signWith, String sealCertificat) throws IOException {
        String error = signBookService.initMassSign(userEppn, authUserEppn, ids, httpSession, password, signWith, sealCertificat);
        List<String> signRequestIds = objectMapper.readValue(ids, new TypeReference<>(){});
        for(String signRequestId : signRequestIds) {
            pendingAutoSign(Long.parseLong(signRequestId), userEppn, authUserEppn);
        }
        return error;
    }

    private void pendingAutoSign(Long signRequestId, String userEppn, String authUserEppn) {
        try {
            signBookService.pendingAutoSignAfterUserSignature(signRequestId, userEppn, authUserEppn);
        } catch (RuntimeException e) {
            logger.error("Unable to process automatic signature after sign request {}", signRequestId, e);
        }
    }
}
