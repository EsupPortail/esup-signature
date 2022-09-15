package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipientService {

    @Resource
    private RecipientRepository recipientRepository;

    @Resource
    private WebUtilsService webUtilsService;

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

}