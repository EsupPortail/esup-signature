package org.esupportail.esupsignature.dto.view.signrequest;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;

public record SignUiFrontDto(
        Long signRequestId,
        Long dataId,
        Long formId,
        List<SignRequestParams> currentSignRequestParamses,
        Integer signImageNumber,
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
        Boolean workflowAvailable,
        List<String> signImages,
        String userName,
        String authUserName,
        List<Field> fields,
        Boolean stepRepeatable,
        SignRequestStatus status,
        String action,
        Integer nbSignRequests,
        Boolean notSigned,
        Boolean attachmentAlert,
        Boolean attachmentRequire,
        Boolean otp,
        Boolean restore,
        String phone,
        Boolean returnToHomeAfterSign,
        Boolean manager
) {}

