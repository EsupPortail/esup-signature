package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndOneStepWorkflow extends DefaultWorkflow {

	private String name = "CreatorAndOneStepWorkflow";
	private String description = "Signature du créateur puis d'un signataire";
	private List<WorkflowStep> workflowSteps;

	@Resource
	private UserService userService;

	@Resource
	private RecipientService recipientService;

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<WorkflowStep> getWorkflowSteps() {
		if(this.workflowSteps == null) {
			try {
				this.workflowSteps = generateWorkflowSteps(userService.getCreatorUser(), null, null);
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
	public List<WorkflowStep> generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) throws EsupSignatureUserException {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		//STEP 1
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setStepNumber(1);
		workflowStep1.getRecipients().add(recipientService.createRecipient(null, user));
		workflowStep1.setDescription("Votre signature");
		workflowSteps.add(workflowStep1);
		//STEP 2
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setStepNumber(2);
		workflowStep2.setDescription("Signataire présélectionné en fonction de vos précédentes saisies");
		if(data != null) {
			workflowStep2.setRecipients(getFavoriteRecipientEmail(1, data.getForm(), recipentEmailsStep, user));
		} else {
			workflowStep2.getRecipients().add(recipientService.createRecipient(null, userService.getGenericUser("Utilisateur issue des favoris", "")));
		}
		workflowStep2.setChangeable(true);
		workflowSteps.add(workflowStep2);
		return workflowSteps;
	}
}

