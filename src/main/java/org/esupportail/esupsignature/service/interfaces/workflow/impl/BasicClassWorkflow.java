package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.workflow.ClassWorkflow;
import org.esupportail.esupsignature.service.interfaces.workflow.ModelClassWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BasicClassWorkflow extends ClassWorkflow implements ModelClassWorkflow {

    private final UserService userService;

    public BasicClassWorkflow(UserService userService) {
        this.userService = userService;
    }

	@Override
	public String getName() {
		return "BasicClassWorkflow";
	}

	@Override
	public String getDescription() {
		return "Une signature";
	}

	private List<WorkflowStep> workflowSteps;

	@Override
	public List<WorkflowStep> getWorkflowSteps() {
		if(this.workflowSteps == null) {
			try {
				this.workflowSteps = generateWorkflowSteps("creator", null);
			} catch (EsupSignatureUserException e) {
				return null;
			}
		}
		return this.workflowSteps;
	}

	@Override
	public List<WorkflowStep> generateWorkflowSteps(String userEppn, List<WorkflowStepDto> workflowStepDto) throws EsupSignatureUserException {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setSignType(SignType.signature);
		workflowStep.setDescription("Choix du signataire");
		workflowStep.setChangeable(true);
		workflowStep.getUsers().add(userService.getGenericUser());
		workflowSteps.add(workflowStep);
		return workflowSteps;
	}
}
