package org.esupportail.esupsignature.dto.export;

public interface WorkflowDatasCsvDto {

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
