package org.esupportail.esupsignature.dto.ui.global;

import org.esupportail.esupsignature.config.GlobalProperties;

public class SignatureUiConfigDto {

    private Integer defaultFontSize;
    private Float signatureExtraLineHeightFactor;
    private UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams;
    private Boolean returnToHomeAfterSign;

    public SignatureUiConfigDto() {
    }

    public SignatureUiConfigDto(Integer defaultFontSize,
                                Float signatureExtraLineHeightFactor,
                                UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams,
                                Boolean returnToHomeAfterSign) {
        this.defaultFontSize = defaultFontSize;
        this.signatureExtraLineHeightFactor = signatureExtraLineHeightFactor;
        this.externalSignatureParams = externalSignatureParams;
        this.returnToHomeAfterSign = returnToHomeAfterSign;
    }

    public Integer getDefaultFontSize() {
        return defaultFontSize;
    }

    public void setDefaultFontSize(Integer defaultFontSize) {
        this.defaultFontSize = defaultFontSize;
    }

    public Float getSignatureExtraLineHeightFactor() {
        return signatureExtraLineHeightFactor;
    }

    public void setSignatureExtraLineHeightFactor(Float signatureExtraLineHeightFactor) {
        this.signatureExtraLineHeightFactor = signatureExtraLineHeightFactor;
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

    public Integer defaultFontSize() { return defaultFontSize; }
    public Float signatureExtraLineHeightFactor() { return signatureExtraLineHeightFactor; }
    public UiGlobalPropertiesDto.ExternalSignatureParamsDto externalSignatureParams() { return externalSignatureParams; }
    public Boolean returnToHomeAfterSign() { return returnToHomeAfterSign; }

    public static SignatureUiConfigDto fromGlobalProperties(GlobalProperties props) {
        if (props == null) {
            return null;
        }
        return new SignatureUiConfigDto(
                props.getDefaultFontSize(),
                props.getSignatureExtraLineHeightFactor(),
                UiGlobalPropertiesDto.ExternalSignatureParamsDto.fromSignRequestParams(props.getExternalSignatureParams()),
                props.getReturnToHomeAfterSign()
        );
    }
}
