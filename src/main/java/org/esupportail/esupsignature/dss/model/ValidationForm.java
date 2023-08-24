package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.validation.executor.ValidationLevel;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public class ValidationForm {

	private MultipartFile signedFile;

	private List<MultipartFile> originalFiles;

	private ValidationLevel validationLevel;

	private boolean defaultPolicy;

	private MultipartFile policyFile;

	public MultipartFile getSignedFile() {
		return signedFile;
	}

	public void setSignedFile(MultipartFile signedFile) {
		this.signedFile = signedFile;
	}

	public List<MultipartFile> getOriginalFiles() {
		return originalFiles;
	}

	public void setOriginalFiles(List<MultipartFile> originalFiles) {
		this.originalFiles = originalFiles;
	}

	public ValidationLevel getValidationLevel() {
		return validationLevel;
	}

	public void setValidationLevel(ValidationLevel validationLevel) {
		this.validationLevel = validationLevel;
	}

	public boolean isDefaultPolicy() {
		return defaultPolicy;
	}

	public void setDefaultPolicy(boolean defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public MultipartFile getPolicyFile() {
		return policyFile;
	}

	public void setPolicyFile(MultipartFile policyFile) {
		this.policyFile = policyFile;
	}

	@AssertTrue(message = "{error.signed.file.mandatory}")
	public boolean isSignedFile() {
		return (signedFile != null) && (!signedFile.isEmpty());
	}

}
