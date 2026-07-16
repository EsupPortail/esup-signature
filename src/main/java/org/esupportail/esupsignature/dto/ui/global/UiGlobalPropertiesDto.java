package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignWith;

import java.util.List;

public class UiGlobalPropertiesDto {

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
    private Float signatureExtraLineHeightFactor;
    private Boolean enableHelp;
    private Integer otpValidity;
    private Boolean smsRequired;
    private Integer nbSignOtpTries;
    private Integer nbViewOtpTries;
    private Boolean enableTransfertForUsers;
    private Boolean enableCaptcha;
    private Boolean enableSu;
    private Integer shareMode;
    private ExternalSignatureParamsDto externalSignatureParams;
    private Integer nbDaysBeforeWarning;
    private Integer nbDaysBeforeDeleting;
    private String newVersion;
    private Boolean disableCertStorage;
    private Boolean sealCertificatConfigured;
    private Boolean sealDriverConfigured;

    public UiGlobalPropertiesDto() {
    }

    public UiGlobalPropertiesDto(String rootUrl, String domain, Boolean hideWizard, Boolean hideWizardWorkflow,
                                 Boolean hideAutoSign, Boolean hideSendSignRequest, String applicationEmail,
                                 Boolean infiniteScrolling, Boolean returnToHomeAfterSign, String namingTemplate,
                                 String signedSuffix, Integer maxUploadSize, Boolean pdfOnly,
                                 Boolean exportAttachements, String nexuUrl, List<SignWith> authorizedSignTypes,
                                 Integer signatureImageDpi, Float fixFactor, Boolean externalCanEdit,
                                 Boolean hideHiddenVisa, Boolean disablePdfFontAlert, Integer defaultFontSize,
                                 Float signatureExtraLineHeightFactor,
                                 Boolean enableHelp, Integer otpValidity, Boolean smsRequired,
                                 Integer nbSignOtpTries, Integer nbViewOtpTries,
                                 Boolean enableTransfertForUsers, Boolean enableCaptcha, Boolean enableSu,
                                 Integer shareMode, ExternalSignatureParamsDto externalSignatureParams,
                                 Integer nbDaysBeforeWarning, Integer nbDaysBeforeDeleting, String newVersion,
                                 Boolean disableCertStorage, Boolean sealCertificatConfigured,
                                 Boolean sealDriverConfigured) {
        this.rootUrl = rootUrl;
        this.domain = domain;
        this.hideWizard = hideWizard;
        this.hideWizardWorkflow = hideWizardWorkflow;
        this.hideAutoSign = hideAutoSign;
        this.hideSendSignRequest = hideSendSignRequest;
        this.applicationEmail = applicationEmail;
        this.infiniteScrolling = infiniteScrolling;
        this.returnToHomeAfterSign = returnToHomeAfterSign;
        this.namingTemplate = namingTemplate;
        this.signedSuffix = signedSuffix;
        this.maxUploadSize = maxUploadSize;
        this.pdfOnly = pdfOnly;
        this.exportAttachements = exportAttachements;
        this.nexuUrl = nexuUrl;
        this.authorizedSignTypes = authorizedSignTypes;
        this.signatureImageDpi = signatureImageDpi;
        this.fixFactor = fixFactor;
        this.externalCanEdit = externalCanEdit;
        this.hideHiddenVisa = hideHiddenVisa;
        this.disablePdfFontAlert = disablePdfFontAlert;
        this.defaultFontSize = defaultFontSize;
        this.signatureExtraLineHeightFactor = signatureExtraLineHeightFactor;
        this.enableHelp = enableHelp;
        this.otpValidity = otpValidity;
        this.smsRequired = smsRequired;
        this.nbSignOtpTries = nbSignOtpTries;
        this.nbViewOtpTries = nbViewOtpTries;
        this.enableTransfertForUsers = enableTransfertForUsers;
        this.enableCaptcha = enableCaptcha;
        this.enableSu = enableSu;
        this.shareMode = shareMode;
        this.externalSignatureParams = externalSignatureParams;
        this.nbDaysBeforeWarning = nbDaysBeforeWarning;
        this.nbDaysBeforeDeleting = nbDaysBeforeDeleting;
        this.newVersion = newVersion;
        this.disableCertStorage = disableCertStorage;
        this.sealCertificatConfigured = sealCertificatConfigured;
        this.sealDriverConfigured = sealDriverConfigured;
    }

    public String getRootUrl() { return rootUrl; }
    public void setRootUrl(String rootUrl) { this.rootUrl = rootUrl; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public Boolean getHideWizard() { return hideWizard; }
    public void setHideWizard(Boolean hideWizard) { this.hideWizard = hideWizard; }
    public Boolean getHideWizardWorkflow() { return hideWizardWorkflow; }
    public void setHideWizardWorkflow(Boolean hideWizardWorkflow) { this.hideWizardWorkflow = hideWizardWorkflow; }
    public Boolean getHideAutoSign() { return hideAutoSign; }
    public void setHideAutoSign(Boolean hideAutoSign) { this.hideAutoSign = hideAutoSign; }
    public Boolean getHideSendSignRequest() { return hideSendSignRequest; }
    public void setHideSendSignRequest(Boolean hideSendSignRequest) { this.hideSendSignRequest = hideSendSignRequest; }
    public String getApplicationEmail() { return applicationEmail; }
    public void setApplicationEmail(String applicationEmail) { this.applicationEmail = applicationEmail; }
    public Boolean getInfiniteScrolling() { return infiniteScrolling; }
    public void setInfiniteScrolling(Boolean infiniteScrolling) { this.infiniteScrolling = infiniteScrolling; }
    public Boolean getReturnToHomeAfterSign() { return returnToHomeAfterSign; }
    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) { this.returnToHomeAfterSign = returnToHomeAfterSign; }
    public String getNamingTemplate() { return namingTemplate; }
    public void setNamingTemplate(String namingTemplate) { this.namingTemplate = namingTemplate; }
    public String getSignedSuffix() { return signedSuffix; }
    public void setSignedSuffix(String signedSuffix) { this.signedSuffix = signedSuffix; }
    public Integer getMaxUploadSize() { return maxUploadSize; }
    public void setMaxUploadSize(Integer maxUploadSize) { this.maxUploadSize = maxUploadSize; }
    public Boolean getPdfOnly() { return pdfOnly; }
    public void setPdfOnly(Boolean pdfOnly) { this.pdfOnly = pdfOnly; }
    public Boolean getExportAttachements() { return exportAttachements; }
    public void setExportAttachements(Boolean exportAttachements) { this.exportAttachements = exportAttachements; }
    public String getNexuUrl() { return nexuUrl; }
    public void setNexuUrl(String nexuUrl) { this.nexuUrl = nexuUrl; }
    public List<SignWith> getAuthorizedSignTypes() { return authorizedSignTypes; }
    public void setAuthorizedSignTypes(List<SignWith> authorizedSignTypes) { this.authorizedSignTypes = authorizedSignTypes; }
    public Integer getSignatureImageDpi() { return signatureImageDpi; }
    public void setSignatureImageDpi(Integer signatureImageDpi) { this.signatureImageDpi = signatureImageDpi; }
    public Float getFixFactor() { return fixFactor; }
    public void setFixFactor(Float fixFactor) { this.fixFactor = fixFactor; }
    public Boolean getExternalCanEdit() { return externalCanEdit; }
    public void setExternalCanEdit(Boolean externalCanEdit) { this.externalCanEdit = externalCanEdit; }
    public Boolean getHideHiddenVisa() { return hideHiddenVisa; }
    public void setHideHiddenVisa(Boolean hideHiddenVisa) { this.hideHiddenVisa = hideHiddenVisa; }
    public Boolean getDisablePdfFontAlert() { return disablePdfFontAlert; }
    public void setDisablePdfFontAlert(Boolean disablePdfFontAlert) { this.disablePdfFontAlert = disablePdfFontAlert; }
    public Integer getDefaultFontSize() { return defaultFontSize; }
    public void setDefaultFontSize(Integer defaultFontSize) { this.defaultFontSize = defaultFontSize; }
    public Float getSignatureExtraLineHeightFactor() { return signatureExtraLineHeightFactor; }
    public void setSignatureExtraLineHeightFactor(Float signatureExtraLineHeightFactor) { this.signatureExtraLineHeightFactor = signatureExtraLineHeightFactor; }
    public Boolean getEnableHelp() { return enableHelp; }
    public void setEnableHelp(Boolean enableHelp) { this.enableHelp = enableHelp; }
    public Integer getOtpValidity() { return otpValidity; }
    public void setOtpValidity(Integer otpValidity) { this.otpValidity = otpValidity; }
    public Boolean getSmsRequired() { return smsRequired; }
    public void setSmsRequired(Boolean smsRequired) { this.smsRequired = smsRequired; }
    public Integer getNbSignOtpTries() { return nbSignOtpTries; }
    public void setNbSignOtpTries(Integer nbSignOtpTries) { this.nbSignOtpTries = nbSignOtpTries; }
    public Integer getNbViewOtpTries() { return nbViewOtpTries; }
    public void setNbViewOtpTries(Integer nbViewOtpTries) { this.nbViewOtpTries = nbViewOtpTries; }
    public Boolean getEnableTransfertForUsers() { return enableTransfertForUsers; }
    public void setEnableTransfertForUsers(Boolean enableTransfertForUsers) { this.enableTransfertForUsers = enableTransfertForUsers; }
    public Boolean getEnableCaptcha() { return enableCaptcha; }
    public void setEnableCaptcha(Boolean enableCaptcha) { this.enableCaptcha = enableCaptcha; }
    public Boolean getEnableSu() { return enableSu; }
    public void setEnableSu(Boolean enableSu) { this.enableSu = enableSu; }
    public Integer getShareMode() { return shareMode; }
    public void setShareMode(Integer shareMode) { this.shareMode = shareMode; }
    public ExternalSignatureParamsDto getExternalSignatureParams() { return externalSignatureParams; }
    public void setExternalSignatureParams(ExternalSignatureParamsDto externalSignatureParams) { this.externalSignatureParams = externalSignatureParams; }
    public Integer getNbDaysBeforeWarning() { return nbDaysBeforeWarning; }
    public void setNbDaysBeforeWarning(Integer nbDaysBeforeWarning) { this.nbDaysBeforeWarning = nbDaysBeforeWarning; }
    public Integer getNbDaysBeforeDeleting() { return nbDaysBeforeDeleting; }
    public void setNbDaysBeforeDeleting(Integer nbDaysBeforeDeleting) { this.nbDaysBeforeDeleting = nbDaysBeforeDeleting; }
    public String getNewVersion() { return newVersion; }
    public void setNewVersion(String newVersion) { this.newVersion = newVersion; }
    public Boolean getDisableCertStorage() { return disableCertStorage; }
    public void setDisableCertStorage(Boolean disableCertStorage) { this.disableCertStorage = disableCertStorage; }
    public Boolean getSealCertificatConfigured() { return sealCertificatConfigured; }
    public void setSealCertificatConfigured(Boolean sealCertificatConfigured) { this.sealCertificatConfigured = sealCertificatConfigured; }
    public Boolean getSealDriverConfigured() { return sealDriverConfigured; }
    public void setSealDriverConfigured(Boolean sealDriverConfigured) { this.sealDriverConfigured = sealDriverConfigured; }

    public String rootUrl() { return rootUrl; }
    public String domain() { return domain; }
    public Boolean hideWizard() { return hideWizard; }
    public Boolean hideWizardWorkflow() { return hideWizardWorkflow; }
    public Boolean hideAutoSign() { return hideAutoSign; }
    public Boolean hideSendSignRequest() { return hideSendSignRequest; }
    public String applicationEmail() { return applicationEmail; }
    public Boolean infiniteScrolling() { return infiniteScrolling; }
    public Boolean returnToHomeAfterSign() { return returnToHomeAfterSign; }
    public String namingTemplate() { return namingTemplate; }
    public String signedSuffix() { return signedSuffix; }
    public Integer maxUploadSize() { return maxUploadSize; }
    public Boolean pdfOnly() { return pdfOnly; }
    public Boolean exportAttachements() { return exportAttachements; }
    public String nexuUrl() { return nexuUrl; }
    public List<SignWith> authorizedSignTypes() { return authorizedSignTypes; }
    public Integer signatureImageDpi() { return signatureImageDpi; }
    public Float fixFactor() { return fixFactor; }
    public Boolean externalCanEdit() { return externalCanEdit; }
    public Boolean hideHiddenVisa() { return hideHiddenVisa; }
    public Boolean disablePdfFontAlert() { return disablePdfFontAlert; }
    public Integer defaultFontSize() { return defaultFontSize; }
    public Float signatureExtraLineHeightFactor() { return signatureExtraLineHeightFactor; }
    public Boolean enableHelp() { return enableHelp; }
    public Integer otpValidity() { return otpValidity; }
    public Boolean smsRequired() { return smsRequired; }
    public Integer nbSignOtpTries() { return nbSignOtpTries; }
    public Integer nbViewOtpTries() { return nbViewOtpTries; }
    public Boolean enableTransfertForUsers() { return enableTransfertForUsers; }
    public Boolean enableCaptcha() { return enableCaptcha; }
    public Boolean enableSu() { return enableSu; }
    public Integer shareMode() { return shareMode; }
    public ExternalSignatureParamsDto externalSignatureParams() { return externalSignatureParams; }
    public Integer nbDaysBeforeWarning() { return nbDaysBeforeWarning; }
    public Integer nbDaysBeforeDeleting() { return nbDaysBeforeDeleting; }
    public String newVersion() { return newVersion; }
    public Boolean disableCertStorage() { return disableCertStorage; }
    public Boolean sealCertificatConfigured() { return sealCertificatConfigured; }
    public Boolean sealDriverConfigured() { return sealDriverConfigured; }

    public static UiGlobalPropertiesDto fromGlobalProperties(GlobalProperties props) {
        if (props == null) {
            return null;
        }
        boolean sealCertificatPropertiesConfigured = props.getSealCertificatProperties() != null
                && !props.getSealCertificatProperties().isEmpty();
        return new UiGlobalPropertiesDto(
                props.getRootUrl(),
                props.getDomain(),
                props.getHideWizard(),
                props.getHideWizardWorkflow(),
                props.getHideAutoSign(),
                props.getHideSendSignRequest(),
                props.getApplicationEmail(),
                props.getInfiniteScrolling(),
                props.getReturnToHomeAfterSign(),
                props.getNamingTemplate(),
                props.getSignedSuffix(),
                props.getMaxUploadSize(),
                props.getPdfOnly(),
                props.getExportAttachements(),
                props.getNexuUrl(),
                props.getAuthorizedSignTypes() == null ? List.of() : List.copyOf(props.getAuthorizedSignTypes()),
                props.getSignatureImageDpi(),
                props.getFixFactor(),
                props.getExternalCanEdit(),
                props.getHideHiddenVisa(),
                props.getDisablePdfFontAlert(),
                props.getDefaultFontSize(),
                props.getSignatureExtraLineHeightFactor(),
                props.getEnableHelp(),
                props.getOtpValidity(),
                props.getSmsRequired(),
                props.getNbSignOtpTries(),
                props.getNbViewOtpTries(),
                props.getEnableTransfertForUsers(),
                props.getEnableCaptcha(),
                props.getEnableSu(),
                props.getShareMode(),
                ExternalSignatureParamsDto.fromSignRequestParams(props.getExternalSignatureParams()),
                props.getNbDaysBeforeWarning(),
                props.getNbDaysBeforeDeleting(),
                props.newVersion,
                props.getDisableCertStorage(),
                sealCertificatPropertiesConfigured || props.getSealCertificatType() != null,
                sealCertificatPropertiesConfigured || props.getSealCertificatDriver() != null
        );
    }

    public static class ExternalSignatureParamsDto {
        private Boolean addWatermark;
        private Boolean extraDate;
        private Boolean extraType;
        private Boolean extraName;
        private Boolean addExtra;
        private String extraText;
        private Boolean extraOnTop;

        public ExternalSignatureParamsDto() {
        }

        public ExternalSignatureParamsDto(Boolean addWatermark, Boolean extraDate, Boolean extraType,
                                          Boolean extraName, Boolean addExtra, String extraText,
                                          Boolean extraOnTop) {
            this.addWatermark = addWatermark;
            this.extraDate = extraDate;
            this.extraType = extraType;
            this.extraName = extraName;
            this.addExtra = addExtra;
            this.extraText = extraText;
            this.extraOnTop = extraOnTop;
        }

        public Boolean getAddWatermark() { return addWatermark; }
        public void setAddWatermark(Boolean addWatermark) { this.addWatermark = addWatermark; }
        public Boolean getExtraDate() { return extraDate; }
        public void setExtraDate(Boolean extraDate) { this.extraDate = extraDate; }
        public Boolean getExtraType() { return extraType; }
        public void setExtraType(Boolean extraType) { this.extraType = extraType; }
        public Boolean getExtraName() { return extraName; }
        public void setExtraName(Boolean extraName) { this.extraName = extraName; }
        public Boolean getAddExtra() { return addExtra; }
        public void setAddExtra(Boolean addExtra) { this.addExtra = addExtra; }
        public String getExtraText() { return extraText; }
        public void setExtraText(String extraText) { this.extraText = extraText; }
        public Boolean getExtraOnTop() { return extraOnTop; }
        public void setExtraOnTop(Boolean extraOnTop) { this.extraOnTop = extraOnTop; }

        public Boolean addWatermark() { return addWatermark; }
        public Boolean extraDate() { return extraDate; }
        public Boolean extraType() { return extraType; }
        public Boolean extraName() { return extraName; }
        public Boolean addExtra() { return addExtra; }
        public String extraText() { return extraText; }
        public Boolean extraOnTop() { return extraOnTop; }

        public static ExternalSignatureParamsDto fromSignRequestParams(SignRequestParams params) {
            if (params == null) {
                return null;
            }
            return new ExternalSignatureParamsDto(
                    params.getAddWatermark(),
                    params.getExtraDate(),
                    params.getExtraType(),
                    params.getExtraName(),
                    params.getAddExtra(),
                    params.getExtraText(),
                    params.getExtraOnTop()
            );
        }
    }
}
