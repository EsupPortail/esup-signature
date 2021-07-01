package org.esupportail.esupsignature.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix="global")
public class GlobalProperties implements Cloneable {

    /**
     * Chemin d'acces à l'application
     */
    private String rootUrl;
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
    private Integer trashKeepDelay = -1;
    private Boolean enableSu = false;
    private Boolean enableSplash = false;
    private String version = "";
    private String applicationEmail = "esup.signature@univ-ville.fr";
    private int hoursBeforeRefreshNotif = 24;
    private Boolean infiniteScrolling = true;
    private Boolean returnToHomeAfterSign = true;
    /**
     *Le modèle est construit à l'aide d'attributs entre crochets.
     *default : [title]
     *Les attributs disponibles sont :
     *<ul>
     *  <li>[title] : titre du document original</li>
     *  <li>[id] : identifiant du parapheur</li>
     *  <li>[worflowName] : nom du circuit</li>
     *  <li>[user.name] : nom prénom de l'utilisateur courant</li>
     *  <li>[user.eppn] : eppn de l'utilisateur courant</li>
     *  <li>[user.initials] : initiales de l'utilisateur courant</li>
     *  <li>[UUID] : un identifiant unique</li>
     *  <li>[order] : le numéro d'ordre de création pour un même circuit</li>
     *  <li>[timestamp] : timestamp sous forme de long</li>
     *  <li>[date-fr] : date dd/MM/yyyy hh:mm</li>
     *  <li>[date-en] : date yyyy-MM-dd hh:mm</li>
     *</ul>
     */
    private String namingTemplate = "[title]";
    private String signedSuffix = "_signed";
    /**
     * Choisir le fonctionnement des délégations :
     *  <ul>
     *      <li>0 : système de délégation désactivé</li>
     *      <li>1 : le délégué ne peut signer qu'avec sa propre signature</li>
     *      <li>2 : le délégué ne peut signer qu'avec la signature du mandant</li>
     *      <li>3 : le mandant peut choisir la signature du délégué</li>
     *  </ul>
     */
    private int shareMode = 0;

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
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

    public Integer getTrashKeepDelay() {
        return trashKeepDelay;
    }

    public void setTrashKeepDelay(Integer trashKeepDelay) {
        this.trashKeepDelay = trashKeepDelay;
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

    public int getHoursBeforeRefreshNotif() {
        return hoursBeforeRefreshNotif;
    }

    public void setHoursBeforeRefreshNotif(int hoursBeforeRefreshNotif) {
        this.hoursBeforeRefreshNotif = hoursBeforeRefreshNotif;
    }

    public int getShareMode() {
        return shareMode;
    }

    public void setShareMode(int shareMode) {
        this.shareMode = shareMode;
    }

    public Boolean getInfiniteScrolling() {
        return infiniteScrolling;
    }

    public void setInfiniteScrolling(Boolean infiniteScrolling) {
        this.infiniteScrolling = infiniteScrolling;
    }

    public Boolean getReturnToHomeAfterSign() {
        return returnToHomeAfterSign;
    }

    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) {
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public String getNamingTemplate() {
        return namingTemplate;
    }

    public void setNamingTemplate(String namingTemplate) {
        this.namingTemplate = namingTemplate;
    }

    public String getSignedSuffix() {
        return signedSuffix;
    }

    public void setSignedSuffix(String signedSuffix) {
        this.signedSuffix = signedSuffix;
    }
}
