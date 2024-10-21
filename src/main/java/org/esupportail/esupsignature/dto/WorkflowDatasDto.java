package org.esupportail.esupsignature.dto;

import java.util.List;

public interface WorkflowDatasDto {

    String getSignBookId();
    List<WorkflowDatasSignRequestDto> getWorkflowDatasSignRequestDtos();
    String getSignBookStatus();
    String getSignBookCreateBy();
    String getSignBookCreateDate();
    String getCompletedDate();
    String getCompletedBy();
    String getCurrentStepId();
    String getCurrentStepDescription();
    List<WorkflowDatasStepsRecipientsDto> getWorkflowDatasStepsRecipientsDtos();
    List<WorkflowDatasStepsActionsDto> getWorkflowDatasStepsActionsDtos();

}
