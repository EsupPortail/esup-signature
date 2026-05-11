package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.config.GlobalProperties;

public record SignatureUiConfigDto(
        Integer defaultFontSize,
        UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams,
        Boolean returnToHomeAfterSign
) {
    public static SignatureUiConfigDto fromGlobalProperties(GlobalProperties props) {
        if (props == null) {
            return null;
        }
        return new SignatureUiConfigDto(
                props.getDefaultFontSize(),
                UiGlobalPropertiesDto.ExternalSignatureParamsDto.fromSignRequestParams(props.getExternalSignatureParams()),
                props.getReturnToHomeAfterSign()
        );
    }
}


