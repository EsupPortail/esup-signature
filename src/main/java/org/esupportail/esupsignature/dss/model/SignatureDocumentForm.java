package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

public class SignatureDocumentForm extends AbstractSignatureForm implements ContainerDocumentForm, Serializable {

	private MultipartFile documentToSign;

	@NotNull
	private SignaturePackaging signaturePackaging;

	private ASiCContainerType containerType;

	public MultipartFile getDocumentToSign() {
		return documentToSign;
	}

	public void setDocumentToSign(MultipartFile documentToSign) {
		this.documentToSign = documentToSign;
	}

	public SignaturePackaging getSignaturePackaging() {
		return signaturePackaging;
	}

	public void setSignaturePackaging(SignaturePackaging signaturePackaging) {
		this.signaturePackaging = signaturePackaging;
	}

	@Override
	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	@AssertTrue(message = "{error.to.sign.file.mandatory}")
	public boolean isDocumentToSign() {
		return (documentToSign != null) && (!documentToSign.isEmpty());
	}

}
