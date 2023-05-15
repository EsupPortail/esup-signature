package org.esupportail.esupsignature.config.sign;

import eu.europa.esig.dss.enumerations.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sign")
public class SignProperties {

    private SignatureForm defaultSignatureForm;
    private DigestAlgorithm padesDigestAlgorithm;
    private SignatureLevel padesSignatureLevel;
    private DigestAlgorithm xadesDigestAlgorithm;
    private SignatureLevel xadesSignatureLevel;
    private DigestAlgorithm cadesDigestAlgorithm;
    private SignatureLevel cadesSignatureLevel;
    private ASiCContainerType containerType;
    private SignaturePackaging signaturePackaging;
    private Boolean signWithExpiredCertificate = false;
    private Long passwordTimeout;
    private String aesKey;

    public SignatureForm getDefaultSignatureForm() {
        return defaultSignatureForm;
    }

    public void setDefaultSignatureForm(SignatureForm defaultSignatureForm) {
        this.defaultSignatureForm = defaultSignatureForm;
    }

    public DigestAlgorithm getPadesDigestAlgorithm() {
        return padesDigestAlgorithm;
    }

    public void setPadesDigestAlgorithm(DigestAlgorithm padesDigestAlgorithm) {
        this.padesDigestAlgorithm = padesDigestAlgorithm;
    }

    public SignatureLevel getPadesSignatureLevel() {
        return padesSignatureLevel;
    }

    public void setPadesSignatureLevel(SignatureLevel padesSignatureLevel) {
        this.padesSignatureLevel = padesSignatureLevel;
    }

    public DigestAlgorithm getXadesDigestAlgorithm() {
        return xadesDigestAlgorithm;
    }

    public void setXadesDigestAlgorithm(DigestAlgorithm xadesDigestAlgorithm) {
        this.xadesDigestAlgorithm = xadesDigestAlgorithm;
    }

    public SignatureLevel getXadesSignatureLevel() {
        return xadesSignatureLevel;
    }

    public void setXadesSignatureLevel(SignatureLevel xadesSignatureLevel) {
        this.xadesSignatureLevel = xadesSignatureLevel;
    }
    public DigestAlgorithm getCadesDigestAlgorithm() {
        return cadesDigestAlgorithm;
    }

    public void setCadesDigestAlgorithm(DigestAlgorithm cadesDigestAlgorithm) {
        this.cadesDigestAlgorithm = cadesDigestAlgorithm;
    }

    public SignatureLevel getCadesSignatureLevel() {
        return cadesSignatureLevel;
    }

    public void setCadesSignatureLevel(SignatureLevel cadesSignatureLevel) {
        this.cadesSignatureLevel = cadesSignatureLevel;
    }

    public ASiCContainerType getContainerType() {
        return containerType;
    }

    public void setContainerType(ASiCContainerType containerType) {
        this.containerType = containerType;
    }

    public SignaturePackaging getSignaturePackaging() {
        return signaturePackaging;
    }

    public void setSignaturePackaging(SignaturePackaging signaturePackaging) {
        this.signaturePackaging = signaturePackaging;
    }

    public Boolean getSignWithExpiredCertificate() {
        return signWithExpiredCertificate;
    }

    public void setSignWithExpiredCertificate(Boolean signWithExpiredCertificate) {
        this.signWithExpiredCertificate = signWithExpiredCertificate;
    }

    public Long getPasswordTimeout() {
        return passwordTimeout;
    }

    public void setPasswordTimeout(Long passwordTimeout) {
        this.passwordTimeout = passwordTimeout;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
