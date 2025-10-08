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

    /**
     * Définit la méthode d'emballage de la signature à utiliser pour les documents non PDF
     *
     * SignaturePackaging représente une norme décrivant comment la signature est intégrée
     * par rapport aux données. Les options possibles incluent notamment :
     * - ENVELOPED : la signature est incluse dans les données signées.
     * - ENVELOPING : la signature contient les données signées.
     * - DETACHED : la signature est séparée des données signées.
     *
     * Cette variable est utilisée pour configurer le mode d'intégration de la signature par défaut.
     */
    private SignaturePackaging signaturePackaging = SignaturePackaging.ENVELOPED;
    private Boolean signWithExpiredCertificate = false;
    private Long passwordTimeout;
    private String aesKey;
    private String openscCommandSign = "pkcs11-tool --sign -v --id {0} -p {1} --mechanism SHA256-RSA-PKCS --input-file {2} --output-file {3}";
    private String openscCommandGetId = "pkcs11-tool -O --type pubkey";
    private String openscCommandGetKey = "pkcs11-tool -r --id {0} --type cert";
    private String openscCommandCertId;
    private String openscCommandModule;
    private String openscPathLinux = "";

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

    public String getOpenscCommandSign() {
        return openscCommandSign;
    }

    public void setOpenscCommandSign(String openscCommandSign) {
        this.openscCommandSign = openscCommandSign;
    }

    public String getOpenscCommandGetId() {
        return openscCommandGetId;
    }

    public void setOpenscCommandGetId(String openscCommandGetId) {
        this.openscCommandGetId = openscCommandGetId;
    }

    public String getOpenscCommandGetKey() {
        return openscCommandGetKey;
    }

    public void setOpenscCommandGetKey(String openscCommandGetKey) {
        this.openscCommandGetKey = openscCommandGetKey;
    }

    public String getOpenscCommandCertId() {
        return openscCommandCertId;
    }

    public void setOpenscCommandCertId(String openscCommandCertId) {
        this.openscCommandCertId = openscCommandCertId;
    }

    public String getOpenscCommandModule() {
        return openscCommandModule;
    }

    public void setOpenscCommandModule(String openscCommandModule) {
        this.openscCommandModule = openscCommandModule;
    }

    public String getOpenscPathLinux() {
        return openscPathLinux;
    }

    public void setOpenscPathLinux(String openscPathLinux) {
        this.openscPathLinux = openscPathLinux;
    }
}
