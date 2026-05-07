package org.esupportail.esupsignature.dto.page.user.signrequest;

public record SignRequestParamsFrontDto(
        Long id,
        String pdSignatureFieldName,
        Integer stepNumber,
        Integer signImageNumber,
        Integer signPageNumber,
        Integer signDocumentNumber,
        Integer signWidth,
        Integer signHeight,
        Integer xPos,
        Integer yPos,
        String extraText,
        Boolean isExtraText,
        Boolean addWatermark,
        Boolean allPages,
        Boolean addImage,
        Boolean addExtra,
        Boolean extraType,
        Boolean extraName,
        Boolean extraDate,
        Boolean extraOnTop,
        String textPart,
        Float signScale,
        Integer red,
        Integer green,
        Integer blue,
        Integer fontSize,
        Long recipientId
) {}

