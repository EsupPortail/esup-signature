package org.esupportail.esupsignature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="global")
public class GlobalProperties implements Cloneable {

    private String rootUrl;
    private String groupPrefixRoleName;
    private String domain;
    private String nexuUrl;
    private String nexuVersion;
    private String nexuDownloadUrl;
    private String hideWizard;
    private String hideAutoSign;
    private String hideSendSignRequest;
    private String[] hideWizardExceptRoles;
    private String[] hideAutoSignExceptRoles;
    private String[] hideSendSignExceptRoles;
    private String archiveUri;
    private Integer delayBeforeCleaning = -1;
    private Boolean enableSu = false;
    private Boolean enableSplash = false;
    private String version = "";

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getGroupPrefixRoleName() {
        return groupPrefixRoleName;
    }

    public void setGroupPrefixRoleName(String groupPrefixRoleName) {
        this.groupPrefixRoleName = groupPrefixRoleName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getNexuUrl() {
        return nexuUrl;
    }

    public void setNexuUrl(String nexuUrl) {
        this.nexuUrl = nexuUrl;
    }

    public String getNexuVersion() {
        return nexuVersion;
    }

    public void setNexuVersion(String nexuVersion) {
        this.nexuVersion = nexuVersion;
    }

    public String getNexuDownloadUrl() {
        return nexuDownloadUrl;
    }

    public void setNexuDownloadUrl(String nexuDownloadUrl) {
        this.nexuDownloadUrl = nexuDownloadUrl;
    }

    public String getHideWizard() {
        return hideWizard;
    }

    public void setHideWizard(String hideWizard) {
        this.hideWizard = hideWizard;
    }

    public String getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(String hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }

    public String getHideSendSignRequest() {
        return hideSendSignRequest;
    }

    public void setHideSendSignRequest(String hideSendSignRequest) {
        this.hideSendSignRequest = hideSendSignRequest;
    }

    public String[] getHideWizardExceptRoles() {
        return hideWizardExceptRoles;
    }

    public void setHideWizardExceptRoles(String[] hideWizardExceptRoles) {
        this.hideWizardExceptRoles = hideWizardExceptRoles;
    }

    public String[] getHideAutoSignExceptRoles() {
        return hideAutoSignExceptRoles;
    }

    public void setHideAutoSignExceptRoles(String[] hideAutoSignExceptRoles) {
        this.hideAutoSignExceptRoles = hideAutoSignExceptRoles;
    }

    public String[] getHideSendSignExceptRoles() {
        return hideSendSignExceptRoles;
    }

    public void setHideSendSignExceptRoles(String[] hideSendSignExceptRoles) {
        this.hideSendSignExceptRoles = hideSendSignExceptRoles;
    }

    public String getArchiveUri() {
        return archiveUri;
    }

    public void setArchiveUri(String archiveUri) {
        this.archiveUri = archiveUri;
    }

    public Integer getDelayBeforeCleaning() {
        return delayBeforeCleaning;
    }

    public void setDelayBeforeCleaning(Integer delayBeforeCleaning) {
        this.delayBeforeCleaning = delayBeforeCleaning;
    }

    public Boolean getEnableSu() {
        return enableSu;
    }

    public void setEnableSu(Boolean enableSu) {
        this.enableSu = enableSu;
    }

    public Boolean getEnableSplash() {
        return enableSplash;
    }

    public void setEnableSplash(Boolean enableSplash) {
        this.enableSplash = enableSplash;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
