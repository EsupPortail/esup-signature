package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class LiveWorkflowStepService {

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    @Resource
    private RecipientService recipientService;

    @Resource
    private UserService userService;

    public void addRecipients(LiveWorkflowStep liveWorkflowStep, String... recipientsEmail) {
        for (String recipientEmail : recipientsEmail) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if (liveWorkflowStep.getRecipients().stream().anyMatch(r -> r.getUser().equals(recipientUser))) {
                Recipient recipient = recipientService.createRecipient(recipientUser);
                liveWorkflowStep.getRecipients().add(recipient);
            }
        }
    }

    public void delete(LiveWorkflowStep liveWorkflowStep) {
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }
}
