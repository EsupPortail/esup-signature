package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class BasicWorkflow extends DefaultWorkflow {

	private String name = "BasicWorkflow";
	private String description = "Une signature";
	private List<WorkflowStep> workflowSteps = new ArrayList<>();

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
		if(this.workflowSteps.size() == 0) {
			generateWorkflowSteps(userService.getUserFromAuthentication(), null, null);
		}
		return workflowSteps;
	}

	public void generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setStepNumber(1);
		workflowStep.setDescription("Choix du signataire");
		workflowStep.setChangeable(true);
		if(data != null) {
			workflowStep.setRecipients(getFavoriteRecipientEmail(1, data.getForm(), recipentEmailsStep, user));
		} else {
			workflowStep.getRecipients().add(recipientService.createRecipient(null, userService.getGenericUser("Utilisateur issue des favoris", "")));
		}
		workflowSteps.add(workflowStep);
		this.workflowSteps = workflowSteps;
	}
}
