package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.web.controller.user.WizardController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class LiveWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(LiveWorkflowService.class);

    @Resource
    LiveWorkflowStepRepository liveWorkflowStepRepository;

    @Resource
    UserRepository userRepository;

    @Resource
    UserService userService;

    @Resource
    RecipientService recipientService;

    @Resource
    RecipientRepository recipientRepository;

    public LiveWorkflowStep createWorkflowStep(Boolean allSignToComplete, SignType signType, String... recipientEmails) throws EsupSignatureUserException {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        if(allSignToComplete ==null) {
            liveWorkflowStep.setAllSignToComplete(false);
        } else {
            liveWorkflowStep.setAllSignToComplete(allSignToComplete);
        }
        liveWorkflowStep.setSignType(signType);
        liveWorkflowStepRepository.save(liveWorkflowStep);
        if(recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(liveWorkflowStep, recipientEmails);
        }
        return liveWorkflowStep;
    }

    public void addRecipientsToWorkflowStep(LiveWorkflowStep liveWorkflowStep, String... recipientsEmail) {
        recipientsEmail = Arrays.stream(recipientsEmail).distinct().toArray(String[]::new);
        for (String recipientEmail : recipientsEmail) {
            User recipientUser;
            if (userRepository.countByEmail(recipientEmail) == 0) {
                recipientUser = userService.createUserWithEmail(recipientEmail);
            } else {
                recipientUser = userRepository.findByEmail(recipientEmail).get(0);
            }
            if(liveWorkflowStep.getId() != null) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return;
                    }
                }
            }
            Recipient recipient = recipientService.createRecipient(liveWorkflowStep.getId(), recipientUser);
            recipientRepository.save(recipient);
            liveWorkflowStep.getRecipients().add(recipient);
        }
    }

    public Long toggleAllSignToCompleteForWorkflowStep(LiveWorkflowStep liveWorkflowStep) {
        if(liveWorkflowStep.getAllSignToComplete()) {
            liveWorkflowStep.setAllSignToComplete(false);
        } else {
            liveWorkflowStep.setAllSignToComplete(true);
        }
        return liveWorkflowStep.getId();
    }

    public void addNewStepToSignBook(SignType signType, Boolean allSignToComplete, String[] recipientsEmail, SignBook signBook) throws EsupSignatureUserException {
        logger.info("add new workflow step to signBook " + signBook.getName() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = createWorkflowStep(allSignToComplete, signType, recipientsEmail);
        signBook.getLiveWorkflow().getWorkflowSteps().add(liveWorkflowStep);
    }

    public Long setSignTypeForWorkflowStep(SignType signType, LiveWorkflowStep liveWorkflowStep) {
        liveWorkflowStep.setSignType(signType);
        return liveWorkflowStep.getId();
    }

    public boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
        for(Recipient recipient : liveWorkflowStep.getRecipients()) {
            if(!recipient.getSigned()) {
                return false;
            }
        }
        return true;
    }
}
