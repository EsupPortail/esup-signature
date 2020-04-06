package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class VisaAndSignWorkflow extends DefaultWorkflow {

    private String name = "VisaAndSignWorkflow";
    private String description = "Visa du responsable puis signature de la présidence";
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
        workflowStep1.setName("Directeur de composante du lieu d’accueil");
        workflowStep1.setStepNumber(1);
        workflowStep1.getRecipients().add(recipientService.createRecipient(null, user));
        workflowStep1.setDescription("Visa du directeur de composante du lieu d’accueil");
        workflowStep1.setSignType(SignType.visa);
        if(data != null) {
            workflowStep1.setParentType("data");
            workflowStep1.setParentId(data.getId());
            workflowStep1.setRecipients(getFavoriteRecipientEmail(1, data.getForm(), recipentEmailsStep, user));
        } else {
            workflowStep1.getRecipients().add(recipientService.createRecipient(null, userService.getGenericUser("Utilisateur issue des favoris", "")));
        }
        workflowStep1.setChangeable(true);
        workflowStep1.setMaxRecipients(1);
        workflowSteps.add(workflowStep1);
        //STEP 2
        String step2Recipient =  "david.lemaignent@univ-rouen.fr";
        WorkflowStep workflowStep2 = new WorkflowStep();
        workflowStep2.setName("Président de l’université");
        workflowStep2.setStepNumber(2);
        workflowStep2.setSignType(SignType.pdfImageStamp);
        workflowStep2.setDescription("Signature du Président de l’université");
        List<Recipient> recipientsStep2 = new ArrayList<>();
        if(data != null) {
            workflowStep2.setParentType("data");
            workflowStep2.setParentId(data.getId());
            recipientsStep2.add(recipientService.createRecipient(data.getId(), userService.getUserByEmail(step2Recipient)));
        } else {
            recipientsStep2.add(recipientService.createRecipient(null, userService.getGenericUser(step2Recipient, "")));
        }
        workflowStep2.setRecipients(recipientsStep2);
        workflowStep2.setAllSignToComplete(false);
        workflowSteps.add(workflowStep2);
        return workflowSteps;
    }

}

