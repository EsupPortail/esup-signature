package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class LiveWorkflowStepService {

    private static final Logger logger = LoggerFactory.getLogger(LiveWorkflowStepService.class);

    private final LiveWorkflowStepRepository liveWorkflowStepRepository;

    private final RecipientService recipientService;

    private final UserService userService;

    private final SignTypeService signTypeService;

    private final SignService signService;

    private final ActionService actionService;

    public LiveWorkflowStepService(LiveWorkflowStepRepository liveWorkflowStepRepository, RecipientService recipientService, UserService userService, SignTypeService signTypeService, SignService signService, ActionService actionService) {
        this.liveWorkflowStepRepository = liveWorkflowStepRepository;
        this.recipientService = recipientService;
        this.userService = userService;
        this.signTypeService = signTypeService;
        this.signService = signService;
        this.actionService = actionService;
    }

    public LiveWorkflowStep getById(Long liveWorkflowStepId) {
        return liveWorkflowStepRepository.findById(liveWorkflowStepId).orElse(null);
    }

    public LiveWorkflowStep createLiveWorkflowStep(SignBook signBook, WorkflowStep workflowStep, WorkflowStepDto step) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(step);
            logger.debug("create LiveWorkflowStep with : " + jsonString);
        } catch (JsonProcessingException e) {
            logger.warn(e.getMessage(), e);
        }
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

    public void replaceRecipientsToWorkflowStep(SignBook signBook, LiveWorkflowStep liveWorkflowStep, List<RecipientWsDto> recipientWsDtos) throws EsupSignatureException {
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().indexOf(liveWorkflowStep) + 1 < signBook.getLiveWorkflow().getCurrentStepNumber()) {
            throw new EsupSignatureException("Impossible de modifier les destinataires d'une étape déjà passée");
        }
        liveWorkflowStep.getRecipients().clear();
        Map<Recipient, Action> recipientsToReAdd = new HashMap<>();
        List<Recipient> recipients = addRecipientsToWorkflowStep(signBook, liveWorkflowStep, recipientWsDtos);
        liveWorkflowStepRepository.save(liveWorkflowStep);
        if(signBook.getLiveWorkflow().getCurrentStep().equals(liveWorkflowStep)) {
            for(SignRequest signRequest : signBook.getSignRequests()) {
                for(Map.Entry<Recipient, Action> recipientActionEntry : signRequest.getRecipientHasSigned().entrySet()) {
                    if(!recipientActionEntry.getValue().getActionType().equals(ActionType.none)) {
                        recipientsToReAdd.put(recipientActionEntry.getKey(), recipientActionEntry.getValue());
                    }
                }
                signRequest.getRecipientHasSigned().clear();
                for(Recipient recipient : recipients) {
                    signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
                }
                signRequest.getRecipientHasSigned().putAll(recipientsToReAdd);
            }
        }
        liveWorkflowStep.getRecipients().addAll(recipientsToReAdd.keySet());

    }

    public List<Recipient> addRecipientsToWorkflowStep(SignBook signBook, LiveWorkflowStep liveWorkflowStep, List<RecipientWsDto> recipientWsDtos) {
        List<String> recipientsEmails = recipientService.getAllRecipientsEmails(recipientWsDtos);
        List<Recipient> recipients = new ArrayList<>();
        for (String recipientEmail : recipientsEmails) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if(recipientUser != null && recipientUser.getUserType().equals(UserType.external) && recipientWsDtos != null) {
                Optional<RecipientWsDto> optionalRecipientWsDto = recipientWsDtos.stream().filter(recipientWsDto1 -> recipientWsDto1.getEmail().equals(recipientEmail)).findFirst();
                if(optionalRecipientWsDto.isPresent()) {
                    RecipientWsDto recipientWsDto = optionalRecipientWsDto.get();
                    if(StringUtils.hasText(recipientWsDto.getName())) {
                        recipientUser.setName(recipientWsDto.getName());
                    }
                    if(StringUtils.hasText(recipientWsDto.getFirstName())) {
                        recipientUser.setFirstname(recipientWsDto.getFirstName());
                    }
                    if(StringUtils.hasText(recipientWsDto.getPhone())) {
                        userService.updatePhone(recipientUser.getEppn(), recipientWsDto.getPhone());
                    }
                    recipientUser.setForceSms(recipientWsDto.getForceSms() != null && recipientWsDto.getForceSms());

                }
            }
            if(liveWorkflowStep.getId() != null) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().equals(recipientUser)) {
                        return recipients;
                    }
                }
            }
            if(recipientUser != null) {
                Recipient recipient = recipientService.createRecipient(recipientUser);
                addRecipient(liveWorkflowStep, recipient);
                if(signBook.getTeam().stream().noneMatch(u -> u.getId().equals(recipientUser.getId()))) {
                    signBook.getTeam().add(recipientUser);
                }
                recipients.add(recipient);
            }
        }
        if(liveWorkflowStep.getRecipients().isEmpty() && !liveWorkflowStep.getAutoSign()) throw new EsupSignatureRuntimeException("recipients must not be empty");
        return recipients;
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