package org.esupportail.esupsignature.dto;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignWith;

import java.util.List;

public class FrontendGlobalProperties {

    private String rootUrl;
    private String domain;
    private Boolean hideWizard;
    private Boolean hideWizardWorkflow;
    private Boolean hideAutoSign;
    private Boolean hideSendSignRequest;
    private String applicationEmail;
    private Boolean infiniteScrolling;
    private Boolean returnToHomeAfterSign;
    private String namingTemplate;
    private String signedSuffix;
    private Integer maxUploadSize;
    private Boolean pdfOnly;
    private Boolean exportAttachements;
    private String nexuUrl;
    private List<SignWith> authorizedSignTypes;
    private Integer signatureImageDpi;
    private Float fixFactor;
    private Boolean externalCanEdit;
    private Boolean hideHiddenVisa;
    private Boolean disablePdfFontAlert;
    private Integer defaultFontSize;
    private Boolean enableHelp;
    private Integer otpValidity;
    private Boolean smsRequired;
    private Integer nbSignOtpTries;
    private Integer nbViewOtpTries;
    private Boolean enableTransfertForUsers;
    private Boolean enableCaptcha;
    private Boolean enableSu;
    private Integer shareMode;
    private SignRequestParams externalSignatureParams;
    private Integer nbDaysBeforeWarning;
    private Integer nbDaysBeforeDeleting;
    private String newVersion;
    private Boolean disableCertStorage;
    private Boolean sealCertificatConfigured;
    private Boolean sealDriverConfigured;

    public FrontendGlobalProperties() {
    }

    public static FrontendGlobalProperties fromGlobalProperties(GlobalProperties props) {
        FrontendGlobalProperties dto = new FrontendGlobalProperties();
        dto.setRootUrl(props.getRootUrl());
        dto.setDomain(props.getDomain());
        dto.setHideWizard(props.getHideWizard());
        dto.setHideWizardWorkflow(props.getHideWizardWorkflow());
        dto.setHideAutoSign(props.getHideAutoSign());
        dto.setHideSendSignRequest(props.getHideSendSignRequest());
        dto.setApplicationEmail(props.getApplicationEmail());
        dto.setInfiniteScrolling(props.getInfiniteScrolling());
        dto.setReturnToHomeAfterSign(props.getReturnToHomeAfterSign());
        dto.setNamingTemplate(props.getNamingTemplate());
        dto.setSignedSuffix(props.getSignedSuffix());
        dto.setMaxUploadSize(props.getMaxUploadSize());
        dto.setPdfOnly(props.getPdfOnly());
        dto.setExportAttachements(props.getExportAttachements());
        dto.setNexuUrl(props.getNexuUrl());
        dto.setAuthorizedSignTypes(props.getAuthorizedSignTypes());
        dto.setSignatureImageDpi(props.getSignatureImageDpi());
        dto.setFixFactor(props.getFixFactor());
        dto.setExternalCanEdit(props.getExternalCanEdit());
        dto.setHideHiddenVisa(props.getHideHiddenVisa());
        dto.setDisablePdfFontAlert(props.getDisablePdfFontAlert());
        dto.setDefaultFontSize(props.getDefaultFontSize());
        dto.setEnableHelp(props.getEnableHelp());
        dto.setOtpValidity(props.getOtpValidity());
        dto.setSmsRequired(props.getSmsRequired());
        dto.setNbSignOtpTries(props.getNbSignOtpTries());
        dto.setNbViewOtpTries(props.getNbViewOtpTries());
        dto.setEnableTransfertForUsers(props.getEnableTransfertForUsers());
        dto.setEnableCaptcha(props.getEnableCaptcha());
        dto.setEnableSu(props.getEnableSu());
        dto.setShareMode(props.getShareMode());
        dto.setExternalSignatureParams(props.getExternalSignatureParams());
        dto.setNbDaysBeforeWarning(props.getNbDaysBeforeWarning());
        dto.setNbDaysBeforeDeleting(props.getNbDaysBeforeDeleting());
        dto.newVersion = props.newVersion;
        dto.setDisableCertStorage(props.getDisableCertStorage());
        dto.setSealCertificatConfigured(props.getSealCertificatType() != null);
        dto.setSealDriverConfigured(props.getSealCertificatDriver() != null);
        return dto;
    }

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

    public Boolean getHideWizard() {
        return hideWizard;
    }

    public void setHideWizard(Boolean hideWizard) {
        this.hideWizard = hideWizard;
    }

    public Boolean getHideWizardWorkflow() {
        return hideWizardWorkflow;
    }

    public void setHideWizardWorkflow(Boolean hideWizardWorkflow) {
        this.hideWizardWorkflow = hideWizardWorkflow;
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

    public String getApplicationEmail() {
        return applicationEmail;
    }

    public void setApplicationEmail(String applicationEmail) {
        this.applicationEmail = applicationEmail;
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

    public Integer getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(Integer maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public Boolean getPdfOnly() {
        return pdfOnly;
    }

    public void setPdfOnly(Boolean pdfOnly) {
        this.pdfOnly = pdfOnly;
    }

    public Boolean getExportAttachements() {
        return exportAttachements;
    }

    public void setExportAttachements(Boolean exportAttachements) {
        this.exportAttachements = exportAttachements;
    }

    public String getNexuUrl() {
        return nexuUrl;
    }

    public void setNexuUrl(String nexuUrl) {
        this.nexuUrl = nexuUrl;
    }

    public List<SignWith> getAuthorizedSignTypes() {
        return authorizedSignTypes;
    }

    public void setAuthorizedSignTypes(List<SignWith> authorizedSignTypes) {
        this.authorizedSignTypes = authorizedSignTypes;
    }

    public Integer getSignatureImageDpi() {
        return signatureImageDpi;
    }

    public void setSignatureImageDpi(Integer signatureImageDpi) {
        this.signatureImageDpi = signatureImageDpi;
    }

    public Float getFixFactor() {
        return fixFactor;
    }

    public void setFixFactor(Float fixFactor) {
        this.fixFactor = fixFactor;
    }

    public Boolean getExternalCanEdit() {
        return externalCanEdit;
    }

    public void setExternalCanEdit(Boolean externalCanEdit) {
        this.externalCanEdit = externalCanEdit;
    }

    public Boolean getHideHiddenVisa() {
        return hideHiddenVisa;
    }

    public void setHideHiddenVisa(Boolean hideHiddenVisa) {
        this.hideHiddenVisa = hideHiddenVisa;
    }

    public Boolean getDisablePdfFontAlert() {
        return disablePdfFontAlert;
    }

    public void setDisablePdfFontAlert(Boolean disablePdfFontAlert) {
        this.disablePdfFontAlert = disablePdfFontAlert;
    }

    public Integer getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setDefaultFontSize(Integer defaultFontSize) {
        this.defaultFontSize = defaultFontSize;
    }

    public Boolean getEnableHelp() {
        return enableHelp;
    }

    public void setEnableHelp(Boolean enableHelp) {
        this.enableHelp = enableHelp;
    }

    public Integer getOtpValidity() {
        return otpValidity;
    }

    public void setOtpValidity(Integer otpValidity) {
        this.otpValidity = otpValidity;
    }

    public Boolean getSmsRequired() {
        return smsRequired;
    }

    public void setSmsRequired(Boolean smsRequired) {
        this.smsRequired = smsRequired;
    }

    public Integer getNbSignOtpTries() {
        return nbSignOtpTries;
    }

    public void setNbSignOtpTries(Integer nbSignOtpTries) {
        this.nbSignOtpTries = nbSignOtpTries;
    }

    public Integer getNbViewOtpTries() {
        return nbViewOtpTries;
    }

    public void setNbViewOtpTries(Integer nbViewOtpTries) {
        this.nbViewOtpTries = nbViewOtpTries;
    }

    public Boolean getEnableTransfertForUsers() {
        return enableTransfertForUsers;
    }

    public void setEnableTransfertForUsers(Boolean enableTransfertForUsers) {
        this.enableTransfertForUsers = enableTransfertForUsers;
    }

    public Boolean getEnableCaptcha() {
        return enableCaptcha;
    }

    public void setEnableCaptcha(Boolean enableCaptcha) {
        this.enableCaptcha = enableCaptcha;
    }

    public Boolean getEnableSu() {
        return enableSu;
    }

    public void setEnableSu(Boolean enableSu) {
        this.enableSu = enableSu;
    }

    public Integer getShareMode() {
        return shareMode;
    }

    public void setShareMode(Integer shareMode) {
        this.shareMode = shareMode;
    }

    public SignRequestParams getExternalSignatureParams() {
        return externalSignatureParams;
    }

    public void setExternalSignatureParams(SignRequestParams externalSignatureParams) {
        this.externalSignatureParams = externalSignatureParams;
    }

    public Integer getNbDaysBeforeWarning() {
        return nbDaysBeforeWarning;
    }

    public void setNbDaysBeforeWarning(Integer nbDaysBeforeWarning) {
        this.nbDaysBeforeWarning = nbDaysBeforeWarning;
    }

    public Integer getNbDaysBeforeDeleting() {
        return nbDaysBeforeDeleting;
    }

    public void setNbDaysBeforeDeleting(Integer nbDaysBeforeDeleting) {
        this.nbDaysBeforeDeleting = nbDaysBeforeDeleting;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public Boolean getDisableCertStorage() {
        return disableCertStorage;
    }

    public void setDisableCertStorage(Boolean disableCertStorage) {
        this.disableCertStorage = disableCertStorage;
    }

    public Boolean getSealCertificatConfigured() {
        return sealCertificatConfigured;
    }

    public void setSealCertificatConfigured(Boolean sealCertificatConfigured) {
        this.sealCertificatConfigured = sealCertificatConfigured;
    }

    public Boolean getSealDriverConfigured() {
        return sealDriverConfigured;
    }

    public void setSealDriverConfigured(Boolean sealDriverConfigured) {
        this.sealDriverConfigured = sealDriverConfigured;
    }
}