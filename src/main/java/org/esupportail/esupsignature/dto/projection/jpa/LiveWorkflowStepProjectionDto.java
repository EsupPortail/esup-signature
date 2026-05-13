package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.SignType;

public interface LiveWorkflowStepProjectionDto {
    Long getId();
    String getDescription();
    Boolean getChangeable();
    SignType getSignType();
    Boolean getAutoSign();
    Boolean getAllSignToComplete();
    Boolean getRepeatable();
}

