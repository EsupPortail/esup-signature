package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    public List<WorkflowStep> generateWorkflowSteps(User user, Data data, List<String> recipientEmailsStep) throws EsupSignatureUserException {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        //STEP 1
        WorkflowStep workflowStep1 = new WorkflowStep();
        workflowStep1.setName("Directeur de composante du lieu d’accueil");
        workflowStep1.setDescription("Visa du directeur de composante du lieu d’accueil");
        workflowStep1.setSignType(SignType.visa);
        if(data != null) {
            workflowStep1.setUsers(workflowService.getFavoriteRecipientEmail(1, data.getForm(), recipientEmailsStep, user));
        } else {
            workflowStep1.getUsers().add(userService.getGenericUser("Utilisateur issue des favoris", ""));
        }
        workflowStep1.setChangeable(true);
        workflowStep1.setMaxRecipients(1);
        workflowSteps.add(workflowStep1);
        //STEP 2
        String step2Recipient =  "joel.alexandre@univ-rouen.fr";
        WorkflowStep workflowStep2 = new WorkflowStep();
        workflowStep2.setName("Président de l’université");
        workflowStep2.setSignType(SignType.pdfImageStamp);
        workflowStep2.setDescription("Signature du Président de l’université");
        List<User> recipientsStep2 = new ArrayList<>();
        if(data != null) {
            recipientsStep2.add(userService.checkUserByEmail(step2Recipient));
        } else {
            recipientsStep2.add(userService.getGenericUser(step2Recipient, ""));
        }
        workflowStep2.setUsers(recipientsStep2);
        workflowStep2.setAllSignToComplete(false);
        workflowSteps.add(workflowStep2);
        return workflowSteps;
    }

}

