package org.esupportail.esupsignature.dss.model;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.AssertTrue;
import java.util.Date;
import java.util.List;

public class CertificateValidationForm {

	private Date validationTime;

	private MultipartFile certificateFile;

	private List<MultipartFile> certificateChainFiles;

	public Date getValidationTime() {
		return validationTime;
	}

	public void setValidationTime(Date validationTime) {
		this.validationTime = validationTime;
	}

	public MultipartFile getCertificateFile() {
		return certificateFile;
	}

	public void setCertificateFile(MultipartFile certificateFile) {
		this.certificateFile = certificateFile;
	}

	public List<MultipartFile> getCertificateChainFiles() {
		return certificateChainFiles;
	}

	public void setCertificateChainFiles(List<MultipartFile> certificateChainFiles) {
		this.certificateChainFiles = certificateChainFiles;
	}

	@AssertTrue(message = "{error.certificate.mandatory}")
	public boolean isCertificateFile() {
		return (certificateFile != null) && (!certificateFile.isEmpty());
	}

}
