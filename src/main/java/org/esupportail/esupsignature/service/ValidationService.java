package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.dss.web.WebAppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ValidationService {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
	
	@Autowired
	private CertificateVerifier certificateVerifier;

	@Autowired
	private Resource defaultPolicy;

	public Reports validate(InputStream inputStream) {
		try {
		SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(WebAppUtils.toDSSDocument(inputStream));
		logger.info("validate with : " + documentValidator.getClass());
		documentValidator.setCertificateVerifier(certificateVerifier);
		documentValidator.setValidationLevel(ValidationLevel.ARCHIVAL_DATA);
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
