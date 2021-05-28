package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.DssUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnBean(CertificateVerifier.class)
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    @Autowired
    private CertificateVerifier certificateVerifier;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private org.springframework.core.io.Resource defaultPolicy;

    @Transactional
    public Reports validate(long signRequestId) throws IOException {
        byte[] bytes = signRequestService.getToSignDocuments(signRequestId).get(0).getInputStream().readAllBytes();
        return validate(new ByteArrayInputStream(bytes), null);
    }

    public Reports validate(InputStream docInputStream, InputStream signInputStream) {
        try {
            List<DSSDocument> detachedContents = new ArrayList<>();
            SignedDocumentValidator documentValidator;
            if(signInputStream != null && signInputStream.available() > 0) {
                detachedContents.add(DssUtils.toDSSDocument(docInputStream));
                documentValidator = SignedDocumentValidator.fromDocument(DssUtils.toDSSDocument(signInputStream));
                documentValidator.setDetachedContents(detachedContents);
            } else {
                documentValidator = SignedDocumentValidator.fromDocument(DssUtils.toDSSDocument(docInputStream));
            }
            logger.info("validate with : " + documentValidator.getClass());
            documentValidator.setCertificateVerifier(certificateVerifier);
            documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.NONE);
            documentValidator.setLocale(Locale.FRENCH);
            documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
            Reports reports = null;
            try (InputStream is = defaultPolicy.getInputStream()) {
                reports = documentValidator.validateDocument(is);
            } catch (IOException e) {
                logger.error("Unable to parse policy : " + e.getMessage(), e);
            }
            return reports;
        } catch (DSSException | IOException e) {
            logger.error("Unable to read document : " + e.getMessage(), e);
        }
        return null;
    }

}
