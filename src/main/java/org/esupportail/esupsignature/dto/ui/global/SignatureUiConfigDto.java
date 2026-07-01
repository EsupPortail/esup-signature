package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.config.GlobalProperties;

public class SignatureUiConfigDto {

    private Integer defaultFontSize;
    private UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams;
    private Boolean returnToHomeAfterSign;
    private String userTitle;

    public SignatureUiConfigDto() {
    }

    public SignatureUiConfigDto(Integer defaultFontSize,
                                UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams,
                                Boolean returnToHomeAfterSign) {
        this.defaultFontSize = defaultFontSize;
        this.externalSignatureParams = externalSignatureParams;
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public Integer getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setDefaultFontSize(Integer defaultFontSize) {
        this.defaultFontSize = defaultFontSize;
    }

    public UiGlobalPropertiesDto.ExternalSignatureParamsDto getExternalSignatureParams() {
        return externalSignatureParams;
    }

    public void setExternalSignatureParams(UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams) {
        this.externalSignatureParams = externalSignatureParams;
    }

    public Boolean getReturnToHomeAfterSign() {
        return returnToHomeAfterSign;
    }

    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) {
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public String getUserTitle() {
        return userTitle;
    }

    public void setUserTitle(String userTitle) {
        this.userTitle = userTitle;
    }

    public Integer defaultFontSize() { return defaultFontSize; }
    public UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams() { return externalSignatureParams; }
    public Boolean returnToHomeAfterSign() { return returnToHomeAfterSign; }
    public String userTitle() { return userTitle; }

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
