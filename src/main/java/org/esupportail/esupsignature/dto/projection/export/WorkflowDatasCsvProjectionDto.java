package org.esupportail.esupsignature.dto.projection.export;

public interface WorkflowDatasCsvProjectionDto {

    String getSignBookId();
    String getWorkflowDatasSignRequestIds();
    String getWorkflowDatasSignRequestTitles();
    String getSignBookStatus();
    String getSignBookCreateBy();
    String getSignBookCreateDate();
    String getCompletedDate();
    String getCompletedBy();
    String getCurrentStepId();
    String getCurrentStepDescription();
    String getWorkflowDatasStepsRecipientsEmails();
    String getWorkflowDatasStepsActionsTypes();
    String getWorkflowDatasStepsActionsDates();

}
