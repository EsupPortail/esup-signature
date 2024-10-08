package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class LiveWorkflowStepService {

    private static final Logger logger = LoggerFactory.getLogger(LiveWorkflowStepService.class);

    @Resource
    private LiveWorkflowStepRepository liveWorkflowStepRepository;

    @Resource
    private RecipientService recipientService;

    @Resource
    private UserService userService;

    @Resource
    private SignTypeService signTypeService;

    @Resource
    private SignService signService;

    public LiveWorkflowStep createLiveWorkflowStep(SignBook signBook, WorkflowStep workflowStep, WorkflowStepDto step) {
        LiveWorkflowStep liveWorkflowStep = new LiveWorkflowStep();
        liveWorkflowStep.setWorkflowStep(workflowStep);
        liveWorkflowStep.setRepeatable(Objects.requireNonNullElse(step.getRepeatable(), false));
        liveWorkflowStep.setMultiSign(Objects.requireNonNullElse(step.getMultiSign(), true));
        liveWorkflowStep.setAutoSign(Objects.requireNonNullElse(step.getAutoSign(), false));
        liveWorkflowStep.setAllSignToComplete(Objects.requireNonNullElse(step.getAllSignToComplete(), false));
        if(step.getSignType() == null) {
            int minLevel = 2;
            if(signService.isSigned(signBook, null)) {
                minLevel = 3;
            }
            if(liveWorkflowStep.getSignType() == null || liveWorkflowStep.getSignType().getValue() < minLevel) {
                liveWorkflowStep.setSignType(signTypeService.getLessSignType(minLevel));
            }
        } else {
            liveWorkflowStep.setSignType(step.getSignType());
        }
        liveWorkflowStep.setRepeatableSignType(step.getRepeatableSignType());
        addRecipientsToWorkflowStep(signBook, liveWorkflowStep, step.getRecipients());
        liveWorkflowStepRepository.save(liveWorkflowStep);
        return liveWorkflowStep;
    }

    public void addRecipientsToWorkflowStep(SignBook signBook, LiveWorkflowStep liveWorkflowStep,  List<RecipientWsDto> recipients) {
        List<String> recipientsEmails = recipientService.getCompleteRecipientList(recipients);
        for (String recipientEmail : recipientsEmails) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if(recipientUser != null && recipientUser.getUserType().equals(UserType.external) && recipients != null) {
                Optional<RecipientWsDto> optionalRecipientWsDto = recipients.stream().filter(recipientWsDto1 -> recipientWsDto1.getEmail().equals(recipientEmail)).findFirst();
                if(optionalRecipientWsDto.isPresent()) {
                    RecipientWsDto recipientWsDto = optionalRecipientWsDto.get();
                    recipientUser.setName(recipientWsDto.getName());
                    recipientUser.setFirstname(recipientWsDto.getFirstName());
                    if(StringUtils.hasText(recipientWsDto.getPhone())) {
                        userService.updatePhone(recipientUser.getEppn(), recipientWsDto.getPhone());
                    }
                    recipientUser.setForceSms(recipientWsDto.getForceSms() != null && recipientWsDto.getForceSms());

                }
            }
            if(liveWorkflowStep.getId() != null) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return;
                    }
                }
            }
            if(recipientUser != null) {
                Recipient recipient = recipientService.createRecipient(recipientUser);
                addRecipient(liveWorkflowStep, recipient);
                if(signBook.getTeam().stream().noneMatch(u -> u.getId().equals(recipientUser.getId()))) {
                    signBook.getTeam().add(recipientUser);
                }
            }
        }
        if(liveWorkflowStep.getRecipients().isEmpty() && !liveWorkflowStep.getAutoSign()) throw new EsupSignatureRuntimeException("recipients must not be empty");
    }

    public void addRecipient(LiveWorkflowStep liveWorkflowStep, Recipient recipient) {
        if(liveWorkflowStep.getRecipients().stream().noneMatch(r -> r.getUser().getEmail().equals(recipient.getUser().getEmail()))) {
            liveWorkflowStep.getRecipients().add(recipient);
        }
    }

    public void delete(LiveWorkflowStep liveWorkflowStep) {
        liveWorkflowStepRepository.delete(liveWorkflowStep);
    }

    public void delete(Long id) {
        Optional<LiveWorkflowStep> liveWorkflowStep = liveWorkflowStepRepository.findById(id);
        liveWorkflowStep.ifPresent(workflowStep -> liveWorkflowStepRepository.delete(workflowStep));
    }

    public List<LiveWorkflowStep> getLiveWorkflowStepByWorkflowStep(WorkflowStep workflowStep) {
        return liveWorkflowStepRepository.findByWorkflowStep(workflowStep);
    }

}