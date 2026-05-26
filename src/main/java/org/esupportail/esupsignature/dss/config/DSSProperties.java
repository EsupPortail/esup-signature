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
    private String lotlUrl;
    private String lotlCountryCode;
    private String ojUrl;
    private String ojContentKeystoreType = "PKCS12";
    private String ojContentKeystoreFilename = "classpath:keystore.p12";
    private String ojContentKeystorePassword = "dss-password";
    private Boolean tlLoaderTrustAll = false;
    private Boolean tlLoaderLotlUseSunsetDate = true;
    private List<Integer> tlLoaderLotlTlVersions = List.of(5, 6);
    private String tlLoaderCacheFolder;
    private Boolean tlLoaderAdesEnabled = false;
    private String tlLoaderAdesLotlUrl = "https://ec.europa.eu/tools/lotl/mra/ades-lotl.xml";
    private String tlLoaderAdesKeystoreType = "PKCS12";
    private String tlLoaderAdesKeystoreFilename = "classpath:ades/ades-keystore.p12";
    private String tlLoaderAdesKeystorePassword = "dss-password";
    private String tlLoaderAdesTslType = "http://ec.europa.eu/tools/lotl/mra/ades-lotl-tsl-type";
    private List<String> tlLoaderAdesTslStatusList = List.of();
    private List<Integer> tlLoaderAdesTlVersions = List.of(5, 6);
    private String trustedSourceKeystoreType;
    private String trustedSourceKeystoreFilename;
    private String trustedSourceKeystorePassword;
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

    public String getOjContentKeystoreType() {
        return ojContentKeystoreType;
    }

    public void setOjContentKeystoreType(String ojContentKeystoreType) {
        this.ojContentKeystoreType = ojContentKeystoreType;
    }

    public String getOjContentKeystoreFilename() {
        return ojContentKeystoreFilename;
    }

    public void setOjContentKeystoreFilename(String ojContentKeystoreFilename) {
        this.ojContentKeystoreFilename = ojContentKeystoreFilename;
    }

    public String getOjContentKeystorePassword() {
        return ojContentKeystorePassword;
    }

    public void setOjContentKeystorePassword(String ojContentKeystorePassword) {
        this.ojContentKeystorePassword = ojContentKeystorePassword;
    }

    public Boolean getTlLoaderTrustAll() {
        return tlLoaderTrustAll;
    }

    public void setTlLoaderTrustAll(Boolean tlLoaderTrustAll) {
        this.tlLoaderTrustAll = tlLoaderTrustAll;
    }

    public Boolean getTlLoaderLotlUseSunsetDate() {
        return tlLoaderLotlUseSunsetDate;
    }

    public void setTlLoaderLotlUseSunsetDate(Boolean tlLoaderLotlUseSunsetDate) {
        this.tlLoaderLotlUseSunsetDate = tlLoaderLotlUseSunsetDate;
    }

    public List<Integer> getTlLoaderLotlTlVersions() {
        return tlLoaderLotlTlVersions;
    }

    public void setTlLoaderLotlTlVersions(List<Integer> tlLoaderLotlTlVersions) {
        this.tlLoaderLotlTlVersions = tlLoaderLotlTlVersions;
    }

    public String getTlLoaderCacheFolder() {
        return tlLoaderCacheFolder;
    }

    public void setTlLoaderCacheFolder(String tlLoaderCacheFolder) {
        this.tlLoaderCacheFolder = tlLoaderCacheFolder;
    }

    public Boolean getTlLoaderAdesEnabled() {
        return tlLoaderAdesEnabled;
    }

    public void setTlLoaderAdesEnabled(Boolean tlLoaderAdesEnabled) {
        this.tlLoaderAdesEnabled = tlLoaderAdesEnabled;
    }

    public String getTlLoaderAdesLotlUrl() {
        return tlLoaderAdesLotlUrl;
    }

    public void setTlLoaderAdesLotlUrl(String tlLoaderAdesLotlUrl) {
        this.tlLoaderAdesLotlUrl = tlLoaderAdesLotlUrl;
    }

    public String getTlLoaderAdesKeystoreType() {
        return tlLoaderAdesKeystoreType;
    }

    public void setTlLoaderAdesKeystoreType(String tlLoaderAdesKeystoreType) {
        this.tlLoaderAdesKeystoreType = tlLoaderAdesKeystoreType;
    }

    public String getTlLoaderAdesKeystoreFilename() {
        return tlLoaderAdesKeystoreFilename;
    }

    public void setTlLoaderAdesKeystoreFilename(String tlLoaderAdesKeystoreFilename) {
        this.tlLoaderAdesKeystoreFilename = tlLoaderAdesKeystoreFilename;
    }

    public String getTlLoaderAdesKeystorePassword() {
        return tlLoaderAdesKeystorePassword;
    }

    public void setTlLoaderAdesKeystorePassword(String tlLoaderAdesKeystorePassword) {
        this.tlLoaderAdesKeystorePassword = tlLoaderAdesKeystorePassword;
    }

    public String getTlLoaderAdesTslType() {
        return tlLoaderAdesTslType;
    }

    public void setTlLoaderAdesTslType(String tlLoaderAdesTslType) {
        this.tlLoaderAdesTslType = tlLoaderAdesTslType;
    }

    public List<String> getTlLoaderAdesTslStatusList() {
        return tlLoaderAdesTslStatusList;
    }

    public void setTlLoaderAdesTslStatusList(List<String> tlLoaderAdesTslStatusList) {
        this.tlLoaderAdesTslStatusList = tlLoaderAdesTslStatusList;
    }

    public List<Integer> getTlLoaderAdesTlVersions() {
        return tlLoaderAdesTlVersions;
    }

    public void setTlLoaderAdesTlVersions(List<Integer> tlLoaderAdesTlVersions) {
        this.tlLoaderAdesTlVersions = tlLoaderAdesTlVersions;
    }

    public String getTrustedSourceKeystoreType() {
        return trustedSourceKeystoreType;
    }

    public void setTrustedSourceKeystoreType(String trustedSourceKeystoreType) {
        this.trustedSourceKeystoreType = trustedSourceKeystoreType;
    }

    public String getTrustedSourceKeystoreFilename() {
        return trustedSourceKeystoreFilename;
    }

    public void setTrustedSourceKeystoreFilename(String trustedSourceKeystoreFilename) {
        this.trustedSourceKeystoreFilename = trustedSourceKeystoreFilename;
    }

    public String getTrustedSourceKeystorePassword() {
        return trustedSourceKeystorePassword;
    }

    public void setTrustedSourceKeystorePassword(String trustedSourceKeystorePassword) {
        this.trustedSourceKeystorePassword = trustedSourceKeystorePassword;
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
