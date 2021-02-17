package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

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

    @Resource
    private UserPropertieService userPropertieService;

    public LiveWorkflowStep createLiveWorkflowStep(WorkflowStep workflowStep, Boolean repeatable, Boolean allSignToComplete, SignType signType, String... recipientEmails) {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        liveWorkflowStep.setWorkflowStep(workflowStep);
        if(repeatable == null) {
            liveWorkflowStep.setRepeatable(false);
        } else {
            liveWorkflowStep.setRepeatable(repeatable);
        }

        if(allSignToComplete == null) {
            liveWorkflowStep.setAllSignToComplete(false);
        } else {
            liveWorkflowStep.setAllSignToComplete(allSignToComplete);
        }
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
    public void addNewStepToSignBook(SignType signType, Boolean allSignToComplete, String[] recipientsEmails, Long signBookId, String authUserEppn) {
        SignBook signBook = signBookService.getById(signBookId);
        logger.info("add new workflow step to signBook " + signBook.getName() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = createLiveWorkflowStep(null,false, allSignToComplete, signType, recipientsEmails);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Arrays.asList(recipientsEmails));
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

    public void delete(Long id) {
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepRepository.findById(id).get();
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }

    public List<LiveWorkflowStep> getLiveWorkflowStepByWorkflowStep(WorkflowStep workflowStep) {
        return liveWorkflowStepRepository.findByWorkflowStep(workflowStep);
    }
}
