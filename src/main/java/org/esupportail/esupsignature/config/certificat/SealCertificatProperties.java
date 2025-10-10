package org.esupportail.esupsignature.config.certificat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

import java.util.ArrayList;
import java.util.List;

public class SealCertificatProperties {


    public String sealCertificatName;

    /**
     * Titre ou description du certificat cachet.
     */
    private String sealCertificatTitle;

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


    /**
     * Identifiant de l'emplacement (slot) utilisé pour accéder au certificat cachet.
     * Cet identifiant représente l'index ou le numéro du slot matériel ou logiciel
     * Lancer pkcs11-tool -L (présent après installation d'OpenSC) pour obtenir la liste des slots
     */
    private Integer sealCertificatSlotId;

    /**
     * Propriété qui désigne une référence ou un lien vers un certificat cachet secondaire,
     * utilisé comme certificat de substitution ou de secours.
     */
    private String sealSpareOf = "";

    private List<String> roles = new ArrayList<>();

    @JsonIgnore
    public DSSPrivateKeyEntry dssPrivateKeyEntry;

    public boolean eIDasValidity = false;

    public SealCertificatProperties() {
    }

    public SealCertificatProperties(String sealCertificatTitle, TokenType sealCertificatType, String sealCertificatFile, String sealCertificatDriver, String sealCertificatPin) {
        this.sealCertificatTitle = sealCertificatTitle;
        this.sealCertificatType = sealCertificatType;
        this.sealCertificatFile = sealCertificatFile;
        this.sealCertificatDriver = sealCertificatDriver;
        this.sealCertificatPin = sealCertificatPin;
    }

    public String getSealCertificatName() {
        return sealCertificatName;
    }

    public String getSealCertificatTitle() {
        return sealCertificatTitle;
    }

    public void setSealCertificatTitle(String sealCertificatTitle) {
        this.sealCertificatTitle = sealCertificatTitle;
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

    public Integer getSealCertificatSlotId() {
        return sealCertificatSlotId;
    }

    public void setSealCertificatSlotId(Integer sealCertificatSlotId) {
        this.sealCertificatSlotId = sealCertificatSlotId;
    }

    public String getSealSpareOf() {
        return sealSpareOf;
    }

    public void setSealSpareOf(String sealSpareOf) {
        this.sealSpareOf = sealSpareOf;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public DSSPrivateKeyEntry getDssPrivateKeyEntry() {
        return dssPrivateKeyEntry;
    }

    public boolean containsRole(List<String> userRoles) {
        return roles.stream().anyMatch(userRoles::contains);
    }

}
