package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignaturePackaging;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class SignatureDocumentForm extends AbstractSignatureForm implements Serializable {

	private byte[] documentToSign;

	@NotNull(message = "{error.signature.packaging.mandatory}")
	private SignaturePackaging signaturePackaging;

	private ASiCContainerType containerType;

	public byte[] getDocumentToSign() {
		return documentToSign;
	}

	public void setDocumentToSign(byte[] documentToSign) {
		this.documentToSign = documentToSign;
	}

	public SignaturePackaging getSignaturePackaging() {
		return signaturePackaging;
	}

	public void setSignaturePackaging(SignaturePackaging signaturePackaging) {
		this.signaturePackaging = signaturePackaging;
	}

	public ASiCContainerType getContainerType() {
		return containerType;
	}

	public void setContainerType(ASiCContainerType containerType) {
		this.containerType = containerType;
	}

	@AssertTrue(message = "{error.to.sign.file.mandatory}")
	public boolean isDocumentToSign() {
		return documentToSign != null && documentToSign.length > 0;
	}

}
