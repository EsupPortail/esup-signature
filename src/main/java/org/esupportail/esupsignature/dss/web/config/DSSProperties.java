package org.esupportail.esupsignature.dss.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="dss")
public class DSSProperties {

    private String cacheUsername;

    private String cachePassword;

    private String cacheDataSourceUrl;

    private String cacheDataSourceDriverClassName;

    private String defaultValidationPolicy;

    private String tspServer;

    private String ksType;

    private String ksFilename;

    private String ksPassword;

    private String serverSigningKeystoreType;

    private String serverSigningKeystoreFilename;

    private String serverSigningKeystorePassword;



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

    public String getTspServer() {
        return tspServer;
    }

    public void setTspServer(String tspServer) {
        this.tspServer = tspServer;
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
}
