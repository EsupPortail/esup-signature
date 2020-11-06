package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class LiveWorkflowService {

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

    public LiveWorkflowStep createWorkflowStep(String name, String parentType, Long parentId, Boolean allSignToComplete, SignType signType, String... recipientEmails) throws EsupSignatureUserException {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        if(name != null) {
            liveWorkflowStep.setName(name);
        }
        liveWorkflowStep.setParentType(parentType);
        liveWorkflowStep.setParentId(parentId);
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
            recipient.setParentId(liveWorkflowStep.getId());
            recipient.setParentType("workflow");
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
