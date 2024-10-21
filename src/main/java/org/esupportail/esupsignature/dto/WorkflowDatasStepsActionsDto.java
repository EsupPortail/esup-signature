package org.esupportail.esupsignature.dto;

import org.esupportail.esupsignature.entity.enums.ActionType;

import java.util.Date;

public interface WorkflowDatasStepsActionsDto {

    ActionType getActionType();
    Date getDate();

}
