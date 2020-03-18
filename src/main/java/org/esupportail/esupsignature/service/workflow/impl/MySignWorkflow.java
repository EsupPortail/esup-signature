package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySignWorkflow extends DefaultWorkflow {

	private String name = "MySignWorkflow";
	private String description = "Ma signature";
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
			this.workflowSteps = generateWorkflowSteps(userService.getUserFromAuthentication(), null, null);
		}
		return this.workflowSteps;
	}

	public void initWorkflowSteps() {
		this.workflowSteps = new ArrayList<>();
	}

	@Override
	public List<WorkflowStep> generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setStepNumber(1);
		workflowStep1.getRecipients().add(recipientService.createRecipient(null, user));
		workflowStep1.setDescription("Votre signature");
		workflowStep1.setSignType(SignType.pdfImageStamp);
		workflowSteps.add(workflowStep1);
		return workflowSteps;
	}
}
