package org.esupportail.esupsignature.service.workflow;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultWorkflow extends Workflow {

    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private UserService userService;

    @Resource
    private RecipientService recipientService;

    @Override
    public String getName() {
        return "DefaultWorkflow";
    }

    @Override
    public DocumentIOType getSourceType() {
        return DocumentIOType.none;
    }

    @Override
    public DocumentIOType getTargetType() {
        return DocumentIOType.none;
    }

    @Override
    public String getDescription() {
        return "Workflow par défaut";
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        return this.workflowSteps;
    }

    public void generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) {
    }

    public List<Recipient> getFavoriteRecipientEmail(int step, Form form, List<String> recipientEmails, User user) {
        List<Recipient> recipients = new ArrayList<>();
        if(recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(step))).collect(Collectors.toList());
            for(String recipientEmail : recipientEmails) {
                recipients.add(recipientService.getRecipientByEmail(form.getId(), recipientEmail.substring(recipientEmail.indexOf("*") + 1)));
            }
        } else {
            List<String> favoritesEmail = userPropertieService.getFavoritesEmails(user, step, form);
            for(String email : favoritesEmail) {
                User recipientUser = userService.getUser(email);
                recipients.add(recipientService.createRecipient(null, recipientUser));
            }
        }
        return recipients;
    }
}
