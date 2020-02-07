package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndOneStepWorkflow extends DefaultWorkflow {

	private String name = "CreatorAndOneStepWorkflow";
	private String description = "Signature du créateur puis d'un signataire";

	@Resource
	private UserRepository userRepository;

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
	public List<WorkflowStep> getWorkflowSteps(Data data, List<String> recipentEmailsStep) {
		//TODO from data owner
		User user = userRepository.findByEppn(data.getOwner()).get(0);
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		//STEP 1
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setStepNumber(1);
		workflowStep1.getRecipients().add(recipientService.createRecipient(data.getId(), user));
		workflowStep1.setDescription("Votre signature");
		workflowSteps.add(workflowStep1);
		//STEP 2
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setStepNumber(2);
		workflowStep1.setDescription("Signataire présélectionné en fonction de vos précédentes saisies");
		workflowStep2.setRecipients(getFavoriteRecipientEmail(2, data.getForm(), recipentEmailsStep, user));
		workflowStep2.setChangeable(true);
		workflowSteps.add(workflowStep2);
		return workflowSteps;
	}
}

