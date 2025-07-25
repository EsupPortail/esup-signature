package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndManagerWorkflow extends DefaultWorkflow {

    @Override
    public String getName() {
        return "CreatorAndManagerClassWorkflow";
    }

    @Override
    public String getDescription() {
        return "Signature du créateur puis du responsable";
    }

    private List<WorkflowStep> workflowSteps;

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        if(this.workflowSteps == null) {
            try {
                this.workflowSteps = generateWorkflowSteps(userService.getCreatorUser(), null);
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
    public List<WorkflowStep> generateWorkflowSteps(User user, List<String> recipentEmailsStep) throws EsupSignatureUserException {
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
        workflowStep2.setChangeable(true);
        workflowSteps.add(workflowStep2);
        return workflowSteps;
    }

}

