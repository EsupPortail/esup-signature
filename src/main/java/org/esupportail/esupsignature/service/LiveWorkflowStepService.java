package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class LiveWorkflowStepService {

    private static final Logger logger = LoggerFactory.getLogger(LiveWorkflowService.class);

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    @Resource
    private RecipientService recipientService;

    @Resource
    private UserService userService;

    @Resource
    private SignBookService signBookService;

    public LiveWorkflowStep createWorkflowStep(Boolean repeatable, Boolean allSignToComplete, SignType signType, String... recipientEmails) {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        liveWorkflowStep.setRepeatable(repeatable);
        liveWorkflowStep.setAllSignToComplete(allSignToComplete);
        liveWorkflowStep.setSignType(signType);
        if(recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(liveWorkflowStep, recipientEmails);
        }
        liveWorkflowStepRepository.save(liveWorkflowStep);
        return liveWorkflowStep;
    }

    public void addRecipientsToWorkflowStep(LiveWorkflowStep liveWorkflowStep, String... recipientsEmail) {
        recipientsEmail = Arrays.stream(recipientsEmail).distinct().toArray(String[]::new);
        for (String recipientEmail : recipientsEmail) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if(liveWorkflowStep.getId() != null) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return;
                    }
                }
            }
            if(recipientUser != null) {
                Recipient recipient = recipientService.createRecipient(recipientUser);
                liveWorkflowStep.getRecipients().add(recipient);
            }
        }
    }

    @Transactional
    public void addNewStepToSignBook(SignType signType, Boolean allSignToComplete, String[] recipientsEmail, Long signBookId) {
        SignBook signBook = signBookService.getById(signBookId);
        logger.info("add new workflow step to signBook " + signBook.getName() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = createWorkflowStep(false, allSignToComplete, signType, recipientsEmail);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
    }

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
