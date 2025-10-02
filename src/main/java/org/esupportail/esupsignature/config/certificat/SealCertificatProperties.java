package org.esupportail.esupsignature.config.certificat;

public class SealCertificatProperties {

    /**
     *  Type de certificat cachet (PKCS11, PKCS12, OPENSC)
     */
    private TokenType sealCertificatType;

    public enum TokenType {
        PKCS11, PKCS12, OPENSC
    }

    /**
     *  Emplacement du certificat cachet (actif pour PKCS12)
     */
    private String sealCertificatFile;

    /**
     *  Pilote du certificat cachet
     */
    private String sealCertificatDriver;

    /**
     *  Pin du certificat cachet
     */
    private String sealCertificatPin = "";

    public SealCertificatProperties() {
    }

    public SealCertificatProperties(TokenType sealCertificatType, String sealCertificatFile, String sealCertificatDriver, String sealCertificatPin) {
        this.sealCertificatType = sealCertificatType;
        this.sealCertificatFile = sealCertificatFile;
        this.sealCertificatDriver = sealCertificatDriver;
        this.sealCertificatPin = sealCertificatPin;
    }

    public TokenType getSealCertificatType() {
        return sealCertificatType;
    }

    public void setSealCertificatType(TokenType sealCertificatType) {
        this.sealCertificatType = sealCertificatType;
    }

    public String getSealCertificatFile() {
        return sealCertificatFile;
    }

    public void setSealCertificatFile(String sealCertificatFile) {
        this.sealCertificatFile = sealCertificatFile;
    }

    public String getSealCertificatDriver() {
        return sealCertificatDriver;
    }

    public void setSealCertificatDriver(String sealCertificatDriver) {
        this.sealCertificatDriver = sealCertificatDriver;
    }

    public String getSealCertificatPin() {
        return sealCertificatPin;
    }

    public void setSealCertificatPin(String sealCertificatPin) {
        this.sealCertificatPin = sealCertificatPin;
    }

}
