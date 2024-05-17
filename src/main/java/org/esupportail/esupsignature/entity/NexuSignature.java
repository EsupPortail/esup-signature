package org.esupportail.esupsignature.entity;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class NexuSignature {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
	@SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
	private Long id;

	@OneToOne(cascade = CascadeType.DETACH)
	private SignRequest signRequest;

	@Temporal(TemporalType.TIMESTAMP)
	private Date signingDate;

	private Boolean signWithExpiredCertificate;

	@Enumerated(EnumType.STRING)
	private SignatureForm signatureForm;

	@Enumerated(EnumType.STRING)
	private SignatureLevel signatureLevel;

	@Enumerated(EnumType.STRING)
	private DigestAlgorithm digestAlgorithm;

	private byte[] certificate;

	@ElementCollection
	private List<byte[]> certificateChain;

	@Enumerated(EnumType.STRING)
	private EncryptionAlgorithm encryptionAlgorithm;

	private byte[] signatureValue;

	@OneToMany(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
	private List<Document> documentToSign = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SignRequest getSignRequest() {
		return signRequest;
	}

	public void setSignRequest(SignRequest signRequest) {
		this.signRequest = signRequest;
	}

	public Date getSigningDate() {
		return signingDate;
	}

	public void setSigningDate(Date signingDate) {
		this.signingDate = signingDate;
	}

	public Boolean getSignWithExpiredCertificate() {
		return signWithExpiredCertificate;
	}

	public void setSignWithExpiredCertificate(Boolean signWithExpiredCertificate) {
		this.signWithExpiredCertificate = signWithExpiredCertificate;
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

	public List<Document> getDocumentToSign() {
		return documentToSign;
	}

	public void setDocumentToSign(List<Document> documentToSign) {
		this.documentToSign = documentToSign;
	}
}