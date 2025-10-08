package org.esupportail.esupsignature.dss.model;

import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.ws.dto.TimestampDTO;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public abstract class AbstractSignatureForm implements Serializable {

	private Date signingDate;

	@NotNull
	private SignatureForm signatureForm;

	@NotNull
	private SignatureLevel signatureLevel;

    @NotNull
    private ASiCContainerType containerType;

	@NotNull
	private DigestAlgorithm digestAlgorithm;

	private byte[] certificate;

	private List<byte[]> certificateChain;

	private EncryptionAlgorithm encryptionAlgorithm;

	private byte[] signatureValue;

	private TimestampDTO contentTimestamp;

	public Date getSigningDate() {
		return signingDate;
	}

	public void setSigningDate(Date signingDate) {
		this.signingDate = signingDate;
	}

	public SignatureForm getSignatureForm() {
		return signatureForm;
	}

	public void setSignatureForm(SignatureForm signatureForm) {
		this.signatureForm = signatureForm;
	}

	public SignatureLevel getSignatureLevel() {
		return signatureLevel;
	}

	public void setSignatureLevel(SignatureLevel signatureLevel) {
		this.signatureLevel = signatureLevel;
	}

    public ASiCContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ASiCContainerType containerType) {
        this.containerType = containerType;
    }

    public DigestAlgorithm getDigestAlgorithm() {
		return digestAlgorithm;
	}

	public void setDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
		this.digestAlgorithm = digestAlgorithm;
	}

	public byte[] getCertificate() {
		return certificate;
	}

	public void setCertificate(byte[] certificate) {
		this.certificate = certificate;
	}

	public List<byte[]> getCertificateChain() {
		return certificateChain;
	}

	public void setCertificateChain(List<byte[]> certificateChain) {
		this.certificateChain = certificateChain;
	}

	public EncryptionAlgorithm getEncryptionAlgorithm() {
		return encryptionAlgorithm;
	}

	public void setEncryptionAlgorithm(EncryptionAlgorithm encryptionAlgorithm) {
		this.encryptionAlgorithm = encryptionAlgorithm;
	}

	public byte[] getSignatureValue() {
		return signatureValue;
	}

	public void setSignatureValue(byte[] signatureValue) {
		this.signatureValue = signatureValue;
	}

	public TimestampDTO getContentTimestamp() {
		return contentTimestamp;
	}

	public void setContentTimestamp(TimestampDTO contentTimestamp) {
		this.contentTimestamp = contentTimestamp;
	}

}