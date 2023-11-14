package org.esupportail.esupsignature.dss.model;

import jakarta.validation.constraints.NotNull;

public class SignResponse {

	@NotNull
	private byte[] signatureValue;

	public SignResponse() {
	}

	public byte[] getSignatureValue() {
		return signatureValue;
	}

	public void setSignatureValue(byte[] signatureValue) {
		this.signatureValue = signatureValue;
	}

}
