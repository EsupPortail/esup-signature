package org.esupportail.esupsignature.dto.view.ui;

public record ExternalSignatureParamsDto(
        Boolean addWatermark,
        Boolean extraDate,
        Boolean extraType,
        Boolean extraName,
        Boolean addExtra,
        String extraText,
        Boolean extraOnTop
) {
}

