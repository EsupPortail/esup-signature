package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class VisaAndSignWorkflow extends DefaultWorkflow {

    @Override
    public String getName() {
        return "VisaAndSignClassWorkflow";
    }

    @Override
    public String getDescription() {
        return "Visa du responsable puis signature de la présidence";
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
    public List<WorkflowStep> generateWorkflowSteps(User user, List<String> recipientEmailsStep) throws EsupSignatureUserException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        //STEP 1
        WorkflowStep workflowStep1 = new WorkflowStep();
        workflowStep1.setName("Directeur de composante du lieu d’accueil");
        workflowStep1.setDescription("Visa du directeur de composante du lieu d’accueil");
        workflowStep1.setSignType(SignType.visa);
        workflowStep1.getUsers().add(userService.getGenericUser());
        workflowStep1.setChangeable(true);
        workflowStep1.setMaxRecipients(1);
        workflowSteps.add(workflowStep1);
        //STEP 2
        String step2Recipient =  "president@univ-ville.fr";
        WorkflowStep workflowStep2 = new WorkflowStep();
        workflowStep2.setName("Président de l’université");
        workflowStep2.setSignType(SignType.signature);
        workflowStep2.setDescription("Signature du Président de l’université");
        Set<User> recipientsStep2 = new HashSet<>();
        recipientsStep2.add(userService.getUserByEmail(step2Recipient));
        workflowStep2.setUsers(recipientsStep2);
        workflowStep2.setAllSignToComplete(false);
        workflowSteps.add(workflowStep2);
        return workflowSteps;
    }

}

