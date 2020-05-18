package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.DssUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    @Resource
    private CertificateVerifier certificateVerifier;

    @Resource
    private org.springframework.core.io.Resource defaultPolicy;

    public Reports validate(InputStream inputStream) {
        try {
            SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(Objects.requireNonNull(DssUtils.toDSSDocument(inputStream)));
            logger.info("validate with : " + documentValidator.getClass());
            documentValidator.setCertificateVerifier(certificateVerifier);
            documentValidator.setLocale(Locale.FRENCH);
            documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
            Reports reports = null;
            try (InputStream is = defaultPolicy.getInputStream()) {
                reports = documentValidator.validateDocument(is);
            } catch (IOException e) {
                logger.error("Unable to parse policy : " + e.getMessage(), e);
            }
            return reports;
        } catch (DSSException e) {
            logger.error("Unable to read document : " + e.getMessage(), e);
        }
        return null;
    }

}
