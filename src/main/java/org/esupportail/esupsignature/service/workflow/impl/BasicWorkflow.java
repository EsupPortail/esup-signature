package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class BasicWorkflow extends DefaultWorkflow {

	@Resource
	private UserService userService;

	private String name = "BasicWorkflow";
	private String description = "Une signature";

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public List<WorkflowStep> getWorkflowSteps(Data data, List<String> recipentEmailsStep) {
		User user = userService.getUserFromAuthentication();
		List<WorkflowStep> workflowSteps = new ArrayList<WorkflowStep>();
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setStepNumber(1);
		workflowStep.setDescription("Choix du signataire");
		workflowStep.setChangeable(true);
		workflowStep.setRecipients(getFavoriteRecipientEmail(1, data.getForm(), recipentEmailsStep, user));
		workflowSteps.add(workflowStep);
		return workflowSteps;
	}
}
