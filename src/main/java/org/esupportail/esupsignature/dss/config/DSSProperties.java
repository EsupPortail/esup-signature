package org.esupportail.esupsignature.dss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix="dss")
public class DSSProperties {

    private String cacheUsername;
    private String cachePassword;
    private String cacheDataSourceUrl;
    private String cacheDataSourceDriverClassName;
    private String defaultValidationPolicy;
    private List<String> tspServers = List.of("http://timestamp.sectigo.com/qualified", "http://tsa.belgium.be");
    private String serverSigningKeystoreType;
    private String serverSigningKeystoreFilename;
    private String serverSigningKeystorePassword;
    private String lotlUrl;
    private String lotlCountryCode;
    private String ojUrl;
    private String rootUrlInTlBrowser;
    private String country;
    private String stateOrProvince;
    private String postalCode;
    private String locality;
    private Boolean checkRevocationForUntrustedChains = true;
    private Boolean multiThreadTlValidation = true;
    private Boolean acceptSignatureFieldOverlap = false;

    public String getCacheUsername() {
        return cacheUsername;
    }

    public void setCacheUsername(String cacheUsername) {
        this.cacheUsername = cacheUsername;
    }

    public String getCachePassword() {
        return cachePassword;
    }

    public void setCachePassword(String cachePassword) {
        this.cachePassword = cachePassword;
    }

    public String getCacheDataSourceUrl() {
        return cacheDataSourceUrl;
    }

    public void setCacheDataSourceUrl(String cacheDataSourceUrl) {
        this.cacheDataSourceUrl = cacheDataSourceUrl;
    }

    public String getCacheDataSourceDriverClassName() {
        return cacheDataSourceDriverClassName;
    }

    public void setCacheDataSourceDriverClassName(String cacheDataSourceDriverClassName) {
        this.cacheDataSourceDriverClassName = cacheDataSourceDriverClassName;
    }

    public String getDefaultValidationPolicy() {
        return defaultValidationPolicy;
    }

    public void setDefaultValidationPolicy(String defaultValidationPolicy) {
        this.defaultValidationPolicy = defaultValidationPolicy;
    }

    public List<String> getTspServers() {
        return tspServers;
    }

    public void setTspServers(List<String> tspServers) {
        this.tspServers = tspServers;
    }

    public String getServerSigningKeystoreType() {
        return serverSigningKeystoreType;
    }

    public void setServerSigningKeystoreType(String serverSigningKeystoreType) {
        this.serverSigningKeystoreType = serverSigningKeystoreType;
    }

    public String getServerSigningKeystoreFilename() {
        return serverSigningKeystoreFilename;
    }

    public void setServerSigningKeystoreFilename(String serverSigningKeystoreFilename) {
        this.serverSigningKeystoreFilename = serverSigningKeystoreFilename;
    }

    public String getServerSigningKeystorePassword() {
        return serverSigningKeystorePassword;
    }

    public void setServerSigningKeystorePassword(String serverSigningKeystorePassword) {
        this.serverSigningKeystorePassword = serverSigningKeystorePassword;
    }

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

    public String getOjUrl() {
        return ojUrl;
    }

    public void setOjUrl(String ojUrl) {
        this.ojUrl = ojUrl;
    }

    public String getRootUrlInTlBrowser() {
        return rootUrlInTlBrowser;
    }

    public void setRootUrlInTlBrowser(String rootUrlInTlBrowser) {
        this.rootUrlInTlBrowser = rootUrlInTlBrowser;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getStateOrProvince() {
        return stateOrProvince;
    }

    public void setStateOrProvince(String stateOrProvince) {
        this.stateOrProvince = stateOrProvince;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public Boolean getCheckRevocationForUntrustedChains() {
        return checkRevocationForUntrustedChains;
    }

    public void setCheckRevocationForUntrustedChains(Boolean checkRevocationForUntrustedChains) {
        this.checkRevocationForUntrustedChains = checkRevocationForUntrustedChains;
    }

    public Boolean getMultiThreadTlValidation() {
        return multiThreadTlValidation;
    }

    public void setMultiThreadTlValidation(Boolean multiThreadTlValidation) {
        this.multiThreadTlValidation = multiThreadTlValidation;
    }

    public Boolean getAcceptSignatureFieldOverlap() {
        if(acceptSignatureFieldOverlap == null) return false;
        return acceptSignatureFieldOverlap;
    }

    public void setAcceptSignatureFieldOverlap(Boolean acceptSignatureFieldOverlap) {
        this.acceptSignatureFieldOverlap = acceptSignatureFieldOverlap;
    }
}
