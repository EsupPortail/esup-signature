package org.esupportail.esupsignature.dto.projection.jpa;

import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;

public interface LiveWorkflowStepProjectionDto {
    Long getId();
    String getDescription();
    Boolean getChangeable();
    SignType getSignType();
    SignLevel getMinSignLevel();
    Boolean getAutoSign();
    Boolean getAllSignToComplete();
    Boolean getRepeatable();
    Boolean getSealVisa();
}

