package org.esupportail.esupsignature.dto.view.signrequest;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;

public record SignRequestUiCommonDto(
        Long signRequestId,
        Long dataId,
        Long formId,
        List<SignRequestParams> signRequestParams,
        SignType currentSignType,
        Boolean signable,
        Boolean editable,
        List<Comment> comments,
        List<SignRequestParams> spots,
        Boolean pdf,
        Integer currentStepNumber,
        Boolean currentStepMultiSign,
        Boolean currentStepSingleSignWithAnnotation,
        SignLevel currentStepMinSignLevel,
        List<String> signImages,
        List<Field> fields,
        Boolean stepRepeatable,
        SignRequestStatus status,
        String action,
        Integer nbSignRequests,
        Boolean notSigned,
        Boolean attachmentAlert,
        Boolean attachmentRequire,
        Boolean manager
) {}

