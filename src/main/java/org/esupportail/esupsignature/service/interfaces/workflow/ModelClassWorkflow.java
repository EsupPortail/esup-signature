package org.esupportail.esupsignature.service.interfaces.workflow;

import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;

import java.util.List;

public interface ModelClassWorkflow {
    Boolean getFromCode();

    List<WorkflowStep> getWorkflowSteps();

    List<WorkflowStep> generateWorkflowSteps(String userEppn, List<WorkflowStepDto> workflowStepDto);

    User getCreateBy();
}
