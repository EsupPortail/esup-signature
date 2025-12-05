package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.workflow.ModelClassWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndOneStepClassWorkflow extends ModelClassWorkflow {

    private final UserService userService;

    public CreatorAndOneStepClassWorkflow(UserService userService) {
        this.userService = userService;
    }

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
		return false;
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
		//STEP 1
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.getUsers().add(userService.getCreatorUser());
		workflowStep1.setDescription("Votre signature");
		workflowStep1.setSignType(SignType.signature);
		workflowSteps.add(workflowStep1);
		//STEP 2
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setSignType(SignType.signature);
		workflowStep2.setDescription("Signataire présélectionné en fonction de vos précédentes saisies");
		workflowStep2.getUsers().add(userService.getGenericUser());
		workflowStep2.setChangeable(true);
		workflowSteps.add(workflowStep2);
		return workflowSteps;
	}
}

