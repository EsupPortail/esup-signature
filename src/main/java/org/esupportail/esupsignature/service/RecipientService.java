package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    private static final Logger logger = LoggerFactory.getLogger(RecipientService.class);

    @Resource
    private RecipientRepository recipientRepository;

    @Resource
    private WebUtilsService webUtilsService;

    @Resource
    private UserListService userListService;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    public Recipient createRecipient(User user) {
        Recipient recipient = new Recipient();
        recipient.setUser(user);
        recipientRepository.save(recipient);
        return recipient;
    }

    public boolean needSign(List<Recipient> recipients, String userEppn) {
        List<Recipient> recipients1 = recipients.stream().filter(recipient -> recipient.getUser().getEppn().equals(userEppn)).toList();
        if(!recipients1.isEmpty() && !recipients1.get(0).getSigned()) {
            return true;
        }
        return false;
    }

    @Transactional
    public void validateRecipient(SignRequest signRequest, String userEppn) {
        Recipient validateRecipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().getEppn().equals(userEppn)).findFirst().get();
        signRequest.getRecipientHasSigned().get(validateRecipient).setActionType(ActionType.signed);
        signRequest.getRecipientHasSigned().get(validateRecipient).setUserIp(webUtilsService.getClientIp());
        signRequest.getRecipientHasSigned().get(validateRecipient).setDate(new Date());
        validateRecipient.setSigned(true);
    }

    public long recipientsContainsUser(List<Recipient> recipients, String userEppn) {
        return recipients.stream().filter(recipient -> recipient.getUser().getEppn().equals(userEppn)).count();
    }

    public void anonymze(User user, User anonymous) {
        List<Recipient> recipients = recipientRepository.findByUser(user);
        for (Recipient recipient : recipients) {
            recipient.setUser(anonymous);
        }
    }

    public List<String> getCompleteRecipientList(List<RecipientWsDto> recipients) {
        List<User> users = new ArrayList<>();
        if (recipients != null && !recipients.isEmpty()) {
            for (RecipientWsDto recipient : recipients) {
                List<String> groupList = userListService.getUsersEmailFromList(recipient.getEmail());
                if(groupList.isEmpty()) {
                    users.add(userService.getUserByEmail(recipient.getEmail()));
                } else {
                    for(String email : groupList) {
                        users.add(userService.getUserByEmail(email));
                    }
                }
            }
        }
        return users.stream().filter(Objects::nonNull).map(User::getEmail).collect(Collectors.toList());
    }

    @Transactional
    public List<WorkflowStepDto> convertRecipientEmailsToRecipientWsDto(List<String> recipientEmails) {
        List<WorkflowStepDto> workflowStepDtos = new ArrayList<>();
        List<RecipientWsDto> recipientWsDtos = new ArrayList<>();
        if (recipientEmails != null && !recipientEmails.isEmpty()) {
            for (String recipientEmail : recipientEmails) {
                String[] userStrings = recipientEmail.split("\\*");
                if(userStrings.length < 2) {
                    recipientWsDtos.add(new RecipientWsDto(recipientEmail));
                    continue;
                }
                String userEmail = userStrings[1];
                for(String realUserEmail : getCompleteRecipientList(Collections.singletonList(new RecipientWsDto(userEmail)))) {
                    RecipientWsDto recipientWsDto = new RecipientWsDto();
                    recipientWsDto.setStep(Integer.parseInt(userStrings[0]));
                    recipientWsDto.setEmail(realUserEmail);
                    User user = userService.getUserByEmail(realUserEmail);
                    if(user.getUserType().equals(UserType.external) && userStrings.length > 2) {
                        user.setPhone(userStrings[2]);
                    }
                    recipientWsDtos.add(recipientWsDto);
                }
            }
        }
        for(RecipientWsDto recipient : recipientWsDtos) {
            if(workflowStepDtos.size() < recipient.getStep()) {
                WorkflowStepDto workflowStepDto = new WorkflowStepDto();
                workflowStepDto.getRecipients().add(recipient);
                workflowStepDtos.add(workflowStepDto);
            } else {
                workflowStepDtos.get(recipient.getStep()-1).getRecipients().add(recipient);
            }
        }
        return workflowStepDtos;
    }

    public List<WorkflowStepDto> convertRecipientJsonStringToRecipientWsDto(String recipientsJsonString) {
        try {
            return Arrays.asList(objectMapper.readValue(recipientsJsonString, WorkflowStepDto[].class));
        } catch (JsonProcessingException e) {
            logger.warn("error parsing recipientsJsonString : " + recipientsJsonString, e);
            throw new EsupSignatureRuntimeException("error parsing recipientsJsonString : " + recipientsJsonString);
        }
    }
}
