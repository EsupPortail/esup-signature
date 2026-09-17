package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.UserType;

public interface LiveWorkflowStepRecipientProjectionDto {
    Long getStepId();
    Long getRecipientId();
    Boolean getSigned();
    Long getUserId();
    String getUserFirstname();
    String getUserName();
    String getUserEmail();
    String getUserPhone();
    UserType getUserUserType();
}


