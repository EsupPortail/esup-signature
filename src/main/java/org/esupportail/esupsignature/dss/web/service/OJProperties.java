package org.esupportail.esupsignature.dss.web.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix="oj")
public class OJProperties {

    private String lotlUrl;
    private String lotlCountryCode;
    private String lotlRootSchemeInfoUri;
    private String ojUrl;
    private String ksType;
    private String ksFilename;
    private String ksPassword;
    private List<String> trustedCertificatUrlList;

    public String getLotlUrl() {
        return lotlUrl;
    }

    public void setLotlUrl(String lotlUrl) {
        this.lotlUrl = lotlUrl;
    }

    public String getLotlCountryCode() {
        return lotlCountryCode;
    }

    public void setLotlCountryCode(String lotlCountryCode) {
        this.lotlCountryCode = lotlCountryCode;
    }

    public String getLotlRootSchemeInfoUri() {
        return lotlRootSchemeInfoUri;
    }

    public void setLotlRootSchemeInfoUri(String lotlRootSchemeInfoUri) {
        this.lotlRootSchemeInfoUri = lotlRootSchemeInfoUri;
    }

    public String getOjUrl() {
        return ojUrl;
    }

    public void setOjUrl(String ojUrl) {
        this.ojUrl = ojUrl;
    }

    public String getKsType() {
        return ksType;
    }

    public void setKsType(String ksType) {
        this.ksType = ksType;
    }

    public String getKsFilename() {
        return ksFilename;
    }

    public void setKsFilename(String ksFilename) {
        this.ksFilename = ksFilename;
    }

    public String getKsPassword() {
        return ksPassword;
    }

    public void setKsPassword(String ksPassword) {
        this.ksPassword = ksPassword;
    }

    public List<String> getTrustedCertificatUrlList() {
        return trustedCertificatUrlList;
    }

    public void setTrustedCertificatUrlList(List<String> trustedCertificatUrlList) {
        this.trustedCertificatUrlList = trustedCertificatUrlList;
    }
}
