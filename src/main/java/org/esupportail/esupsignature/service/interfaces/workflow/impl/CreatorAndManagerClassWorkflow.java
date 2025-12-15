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
public class CreatorAndManagerClassWorkflow extends ClassWorkflow implements ModelClassWorkflow {

    transient final UserService userService;

    public CreatorAndManagerClassWorkflow(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String getName() {
        return "CreatorAndManagerClassWorkflow";
    }

    @Override
    public String getDescription() {
        return "Signature du créateur puis du responsable";
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        return generateWorkflowSteps("creator", null);
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
        workflowStep2.setDescription("Signature de votre supérieur hiérarchique (présélectionné en fonction de vos précédentes saisies)");
        workflowStep2.getUsers().add(userService.getGenericUser());
        //à remplacer par l'utilisateur n+1 par ex
        //User userManager = calculManager(userEppn)
        //workflowStep2.getUsers().add(userManager);
        workflowStep2.setChangeable(false);
        workflowSteps.add(workflowStep2);
        return workflowSteps;
    }

}

