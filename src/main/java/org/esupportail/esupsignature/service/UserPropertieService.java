package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserPropertieService {

    @Resource
    private UserPropertieRepository userPropertieRepository;

    @Resource
    private RecipientRepository recipientRepository;

    public void createRecipientPropertie(User user, int step, WorkflowStep workflowStep, Form form) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndRecipientsAndForm(user, step, workflowStep.getRecipients(), form);
        if (userProperties.size() == 0) {
            UserPropertie userPropertie = new UserPropertie();
            userPropertie.setStep(step);
            userPropertie.setForm(form);
            for(Recipient recipient : workflowStep.getRecipients()) {
                Recipient newRecipient = new Recipient();
                newRecipient.setUser(user);
                newRecipient.setParentId(recipient.getParentId());
                recipientRepository.save(newRecipient);
                userPropertie.getRecipients().add(newRecipient);
            }
            userPropertie.setUser(user);
            userPropertieRepository.save(userPropertie);
        } else {
            List<UserPropertie> userPropertiesOld = userPropertieRepository.findByUserAndStepAndForm(user, step, form);
            for (UserPropertie userPropertie : userPropertiesOld) {
                userPropertie.setScore(0);
                userPropertieRepository.save(userPropertie);
            }
            UserPropertie userPropertie = userProperties.get(0);
            userPropertie.setScore(userPropertie.getScore() + 1);
            userPropertieRepository.save(userPropertie);
        }
    }

    public void createTargetPropertie(User user, String targetEmail, Form form) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndTargetEmailAndForm(user, targetEmail, form);
        if (userProperties.size() == 0) {
            UserPropertie userPropertie = new UserPropertie();
            userPropertie.setStep(0);
            userPropertie.setForm(form);
            userPropertie.setTargetEmail(targetEmail);
            userPropertie.setUser(user);
            userPropertieRepository.save(userPropertie);
        } else {
            UserPropertie userPropertie = userProperties.get(0);
            userPropertie.setScore(userPropertie.getScore() + 1);
            userPropertieRepository.save(userPropertie);
        }
    }

    public List<Recipient> getFavoritesEmails(User user, int step, Form form) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndForm(user, step, form);
        List<Recipient> favoriteRecipients = new ArrayList<>();
        for (UserPropertie userPropertie : userProperties) {
            favoriteRecipients.addAll(userPropertie.getRecipients());
        }
        return favoriteRecipients;
    }

}
