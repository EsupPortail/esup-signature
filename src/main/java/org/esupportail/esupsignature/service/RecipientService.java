package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    @Resource
    private RecipientRepository recipientRepository;

    public Recipient createRecipient(Long parentId, User user) {
        Recipient recipient = new Recipient();
        recipient.setParentId(parentId);
        recipient.setUser(user);
        recipientRepository.save(recipient);
        return recipient;
    }

    public Recipient findRecipientByUser(List<Recipient> recipients, User user) {
        return recipients.stream().filter(recipient -> recipient.getUser().equals(user)).collect(Collectors.toList()).get(0);
    }

    public void validateRecipient(List<Recipient> recipients, User user) {
        Recipient validateRecipient = recipients.stream().filter(recipient -> recipient.getUser().equals(user)).collect(Collectors.toList()).get(0);
        validateRecipient.setSigned(true);
        recipientRepository.save(validateRecipient);
    }

    public long checkFalseRecipients(List<Recipient> recipients) {
        return recipients.stream().filter(recipient -> !recipient.getSigned()).count();
    }

    public long recipientsContainsUser(List<Recipient> recipients, User user) {
        return recipients.stream().filter(recipient -> recipient.getUser().equals(user)).count();
    }

}
