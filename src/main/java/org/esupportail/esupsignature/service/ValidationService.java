package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.io.InputStream;

import org.esupportail.esupsignature.dss.web.WebAppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;

@Service
public class ValidationService {
	
	private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
	
	@Autowired
	private CertificateVerifier certificateVerifier;

	@Autowired
	private Resource defaultPolicy;
	
	@Value("${validation.level}")
	private String validationLevel;
	
	public Reports validate(MultipartFile multipartFile) {
		
		SignedDocumentValidator documentValidator = SignedDocumentValidator.fromDocument(WebAppUtils.toDSSDocument(multipartFile));
		logger.info("validate with : " + documentValidator.getClass());
		documentValidator.setCertificateVerifier(certificateVerifier);
		documentValidator.setValidationLevel(ValidationLevel.valueOf(validationLevel));
		Reports reports = null;
		try (InputStream is = defaultPolicy.getInputStream()) {
			reports = documentValidator.validateDocument(is);
		} catch (IOException e) {
			logger.error("Unable to parse policy : " + e.getMessage(), e);
		}
		return reports;
	}

}
