package org.esupportail.esupsignature.dto.view.ui;

import org.esupportail.esupsignature.entity.enums.SignWith;

import java.util.List;

public record UiGlobalPropertiesDto(
        String rootUrl,
        String domain,
        Boolean hideWizard,
        Boolean hideWizardWorkflow,
        Boolean hideAutoSign,
        Boolean hideSendSignRequest,
        String applicationEmail,
        Boolean infiniteScrolling,
        Boolean returnToHomeAfterSign,
        String namingTemplate,
        String signedSuffix,
        Integer maxUploadSize,
        Boolean pdfOnly,
        Boolean exportAttachements,
        String nexuUrl,
        List<SignWith> authorizedSignTypes,
        Integer signatureImageDpi,
        Float fixFactor,
        Boolean externalCanEdit,
        Boolean hideHiddenVisa,
        Boolean disablePdfFontAlert,
        Integer defaultFontSize,
        Boolean enableHelp,
        Integer otpValidity,
        Boolean smsRequired,
        Integer nbSignOtpTries,
        Integer nbViewOtpTries,
        Boolean enableTransfertForUsers,
        Boolean enableCaptcha,
        Boolean enableSu,
        Integer shareMode,
        ExternalSignatureParamsDto externalSignatureParams,
        Integer nbDaysBeforeWarning,
        Integer nbDaysBeforeDeleting,
        String newVersion,
        Boolean disableCertStorage,
        Boolean sealCertificatConfigured,
        Boolean sealDriverConfigured
) {
}

