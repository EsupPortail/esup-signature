package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.SignRequestParams;
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
    public static UiGlobalPropertiesDto fromGlobalProperties(GlobalProperties props) {
        if (props == null) {
            return null;
        }
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
                props.getSealCertificatType() != null,
                props.getSealCertificatDriver() != null
        );
    }

    public record ExternalSignatureParamsDto(
            Boolean addWatermark,
            Boolean extraDate,
            Boolean extraType,
            Boolean extraName,
            Boolean addExtra,
            String extraText,
            Boolean extraOnTop
    ) {
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

