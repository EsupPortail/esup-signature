package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.service.interfaces.workflow.ClassWorkflow;
import org.esupportail.esupsignature.service.interfaces.workflow.ModelClassWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoWorkflowClassWorkflow extends ClassWorkflow implements ModelClassWorkflow {

	@Override
	public String getName() {
		return "NoWorkflowClassWorkflow";
	}

	@Override
	public String getDescription() {
		return "Pas de workflow";
	}

    @Override
    public List<WorkflowStep> generateWorkflowSteps(String userEppn, List<WorkflowStepDto> workflowStepDto) {
        return List.of();
    }
}
