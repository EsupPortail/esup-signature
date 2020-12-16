package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    @Resource
    private RecipientRepository recipientRepository;

    public Recipient createRecipient(User user) {
        Recipient recipient = new Recipient();
        recipient.setUser(user);
        recipientRepository.save(recipient);
        return recipient;
    }

    public boolean needSign(List<Recipient> recipients, Long userId) {
        List<Recipient> recipients1 = recipients.stream().filter(recipient -> recipient.getUser().getId().equals(userId)).collect(Collectors.toList());
        if(recipients1.size() > 0 && !recipients1.get(0).getSigned()) {
            return true;
        }
        return false;
    }

    public void validateRecipient(SignRequest signRequest, User user) {
        Recipient validateRecipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().equals(user)).findFirst().get();
        signRequest.getRecipientHasSigned().get(validateRecipient).setActionType(ActionType.signed);
        signRequest.getRecipientHasSigned().get(validateRecipient).setDate(new Date());
    }

    public long recipientsContainsUser(List<Recipient> recipients, User user) {
        return recipients.stream().filter(recipient -> recipient.getUser().equals(user)).count();
    }

}
