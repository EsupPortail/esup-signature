package org.esupportail.esupsignature.config.fs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="fs")
public class FsProperties {

    private String smbTestUri;
    private String smbLogin;
    private String smbPassword;
    private String smbDomain;
    private String vfsTestUri;
    private String cmisUri;
    private String cmisLogin;
    private String cmisPassword;
    private String cmisRespositoryId;
    private String cmisRootPath;

    public String getSmbTestUri() {
        return smbTestUri;
    }

    public void setSmbTestUri(String smbTestUri) {
        this.smbTestUri = smbTestUri;
    }

    public String getSmbLogin() {
        return smbLogin;
    }

    public void setSmbLogin(String smbLogin) {
        this.smbLogin = smbLogin;
    }

    public String getSmbPassword() {
        return smbPassword;
    }

    public void setSmbPassword(String smbPassword) {
        this.smbPassword = smbPassword;
    }

    public String getSmbDomain() {
        return smbDomain;
    }

    public void setSmbDomain(String smbDomain) {
        this.smbDomain = smbDomain;
    }

    public String getVfsTestUri() {
        return vfsTestUri;
    }

    public void setVfsTestUri(String vfsTestUri) {
        this.vfsTestUri = vfsTestUri;
    }

    public String getCmisUri() {
        return cmisUri;
    }

    public void setCmisUri(String cmisUri) {
        this.cmisUri = cmisUri;
    }

    public String getCmisLogin() {
        return cmisLogin;
    }

    public void setCmisLogin(String cmisLogin) {
        this.cmisLogin = cmisLogin;
    }

    public String getCmisPassword() {
        return cmisPassword;
    }

    public void setCmisPassword(String cmisPassword) {
        this.cmisPassword = cmisPassword;
    }

    public String getCmisRespositoryId() {
        return cmisRespositoryId;
    }

    public void setCmisRespositoryId(String cmisRespositoryId) {
        this.cmisRespositoryId = cmisRespositoryId;
    }

    public String getCmisRootPath() {
        return cmisRootPath;
    }

    public void setCmisRootPath(String cmisRootPath) {
        this.cmisRootPath = cmisRootPath;
    }

}
