package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndOneStepWorkflow extends DefaultWorkflow {

	@Override
	public String getName() {
		return "CreatorAndOneStepClassWorkflow";
	}
	
	@Override
	public String getDescription() {
		return "Signature du créateur puis d'un signataire";
	}

	@Override
	public Boolean getPublicUsage() {
		return true;
	}

	private List<WorkflowStep> workflowSteps;

	@Override
	public List<WorkflowStep> getWorkflowSteps() {
		if(this.workflowSteps == null) {
			try {
				this.workflowSteps = generateWorkflowSteps(userService.getCreatorUser(), null, false);
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
		//STEP 1
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.getUsers().add(user);
		workflowStep1.setDescription("Votre signature");
		workflowStep1.setSignType(SignType.pdfImageStamp);
		workflowSteps.add(workflowStep1);
		//STEP 2
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setSignType(SignType.pdfImageStamp);
		workflowStep2.setDescription("Signataire présélectionné en fonction de vos précédentes saisies");
		if(computeFavorite) {
			workflowStep2.setUsers(workflowService.getFavoriteRecipientEmail(2, workflowService.getWorkflowByName(this.getClass().getSimpleName()).getWorkflowSteps().get(1), recipentEmailsStep, user));
		} else {
			workflowStep2.getUsers().add(userService.getGenericUser("Utilisateur issue des favoris", ""));
		}
		workflowStep2.setChangeable(true);
		workflowSteps.add(workflowStep2);
		return workflowSteps;
	}

	@Override
	public void fillWorkflowSteps(Workflow workflow, User user, List<String> recipentEmailsStep) throws EsupSignatureUserException {
		int i = 0;
		workflow.getWorkflowSteps().get(0).getUsers().clear();
		workflow.getWorkflowSteps().get(0).getUsers().add(user);
		workflow.getWorkflowSteps().get(1).getUsers().clear();
		workflow.getWorkflowSteps().get(1).getUsers().addAll(workflowService.getFavoriteRecipientEmail(2, workflow.getWorkflowSteps().get(1), recipentEmailsStep, user));
	}
}

