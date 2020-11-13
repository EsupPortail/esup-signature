package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BasicWorkflow extends DefaultWorkflow {

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
				this.workflowSteps = generateWorkflowSteps(userService.getCurrentUser(), null, false);
			} catch (EsupSignatureUserException e) {
				return null;
			}
		}
		return this.workflowSteps;
	}

	public void initWorkflowSteps() {
		this.workflowSteps = new ArrayList<>();
	}

	@Override
	public List<WorkflowStep> generateWorkflowSteps(User user, List<String> recipentEmailsStep, boolean computeFavorite) throws EsupSignatureUserException {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setSignType(SignType.pdfImageStamp);
		workflowStep.setDescription("Choix du signataire");
		workflowStep.setChangeable(true);
		if(computeFavorite) {
			workflowStep.setUsers(workflowService.getFavoriteRecipientEmail(1, this, recipentEmailsStep, user));
		} else {
			workflowStep.getUsers().add(userService.getGenericUser("Utilisateur issue des favoris", ""));
		}
		workflowSteps.add(workflowStep);
		return workflowSteps;
	}
}
