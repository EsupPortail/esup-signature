package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    @Resource
    private RecipientRepository recipientRepository;

    @Resource
    private WebUtilsService webUtilsService;

    @Resource
    private UserListService userListService;

    @Resource
    private UserService userService;

    public Recipient createRecipient(User user) {
        Recipient recipient = new Recipient();
        recipient.setUser(user);
        recipientRepository.save(recipient);
        return recipient;
    }

    public boolean needSign(List<Recipient> recipients, String userEppn) {
        List<Recipient> recipients1 = recipients.stream().filter(recipient -> recipient.getUser().getEppn().equals(userEppn)).collect(Collectors.toList());
        if(recipients1.size() > 0 && !recipients1.get(0).getSigned()) {
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

    public List<String> getCompleteRecipientList(List<String> recipientEmails) {
        List<User> users = new ArrayList<>();
        if (recipientEmails != null && recipientEmails.size() > 0) {
            for (String recipientEmail : recipientEmails) {
                List<String> groupList = userListService.getUsersEmailFromList(recipientEmail);
                if(groupList.isEmpty()) {
                    users.add(userService.getUserByEmail(recipientEmail));
                } else {
                    for(String email : groupList) {
                        users.add(userService.getUserByEmail(email));
                    }
                }
            }
        }
        return users.stream().filter(Objects::nonNull).map(User::getEmail).collect(Collectors.toList());
    }

}