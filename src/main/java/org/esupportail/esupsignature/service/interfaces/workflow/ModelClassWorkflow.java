package org.esupportail.esupsignature.service.interfaces.workflow;

import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;

import java.util.ArrayList;
import java.util.List;

public abstract class ModelClassWorkflow extends Workflow {

    public Boolean getFromCode() {
        return true;
    }

    public List<WorkflowStep> getWorkflowSteps() {
        return new ArrayList<>();
    }

    public List<WorkflowStep> generateWorkflowSteps(String userEppn, List<WorkflowStepDto> workflowStepDto) throws EsupSignatureUserException {
        return new ArrayList<>();
    }

    public User getCreateBy() {
        return null;
    }

    public void fillWorkflowSteps(String userEppn, List<WorkflowStepDto> workflowStepDto) {
    }
}
