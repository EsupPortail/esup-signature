package org.esupportail.esupsignature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix="global")
public class GlobalProperties implements Cloneable {

    private String rootUrl;
    private String groupPrefixRoleName;
    private String domain;
    private String nexuUrl;
    private String nexuVersion;
    private String nexuDownloadUrl;
    private Boolean hideWizard;
    private Boolean hideAutoSign;
    private Boolean hideSendSignRequest;
    private List<String> hideWizardExceptRoles = new ArrayList<>();
    private List<String> hideAutoSignExceptRoles = new ArrayList<>();
    private List<String> hideSendSignExceptRoles = new ArrayList<>();
    private String archiveUri;
    private Integer delayBeforeCleaning = -1;
    private Boolean enableSu = false;
    private Boolean enableSplash = false;
    private String version = "";
    private String applicationEmail = "esup.signature@univ-ville.fr";
    private Map<String, String> mappingGroupsRoles;

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

    public Boolean getHideWizard() {
        return hideWizard;
    }

    public void setHideWizard(Boolean hideWizard) {
        this.hideWizard = hideWizard;
    }

    public Boolean getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(Boolean hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }

    public Boolean getHideSendSignRequest() {
        return hideSendSignRequest;
    }

    public void setHideSendSignRequest(Boolean hideSendSignRequest) {
        this.hideSendSignRequest = hideSendSignRequest;
    }

    public List<String> getHideWizardExceptRoles() {
        return hideWizardExceptRoles;
    }

    public void setHideWizardExceptRoles(List<String> hideWizardExceptRoles) {
        this.hideWizardExceptRoles = hideWizardExceptRoles;
    }

    public List<String> getHideAutoSignExceptRoles() {
        return hideAutoSignExceptRoles;
    }

    public void setHideAutoSignExceptRoles(List<String> hideAutoSignExceptRoles) {
        this.hideAutoSignExceptRoles = hideAutoSignExceptRoles;
    }

    public List<String> getHideSendSignExceptRoles() {
        return hideSendSignExceptRoles;
    }

    public void setHideSendSignExceptRoles(List<String> hideSendSignExceptRoles) {
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

    public String getApplicationEmail() {
        return applicationEmail;
    }

    public void setApplicationEmail(String applicationEmail) {
        this.applicationEmail = applicationEmail;
    }

    public Map<String, String> getMappingGroupsRoles() {
        return mappingGroupsRoles;
    }

    public void setMappingGroupsRoles(Map<String, String> mappingGroupsRoles) {
        this.mappingGroupsRoles = mappingGroupsRoles;
    }
}
