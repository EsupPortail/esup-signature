package org.esupportail.esupsignature.service.workflow;

import org.esupportail.esupsignature.entity.*;
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

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private RecipientService recipientService;

    @Override
    public String getName() {
        return "DefaultWorkflow";
    }

    @Override
    public String getDescription() {
        return "Workflow par défaut";
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps(Data data, List<String> recipentEmails) {
        return new ArrayList<>();
    }

    public List<Recipient> getFavoriteRecipientEmail(int step, Form form, List<String> recipientEmails, User user) {
        List<Recipient> recipients = new ArrayList<>();
        if(recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(step))).collect(Collectors.toList());
            for(String recipientEmail : recipientEmails) {
                recipients.add(recipientService.getRecipientByEmail(form.getId(), recipientEmail.substring(recipientEmail.indexOf("*") + 1)));
            }
        } else {
            recipients.addAll(userPropertieService.getFavoritesEmails(user, step, form));
        }
        return recipients;
    }
}
