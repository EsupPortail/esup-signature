package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.repository.RecipientRepository;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserPropertieService {

    @Resource
    private UserPropertieRepository userPropertieRepository;

    public void createUserPropertie(User user, int step, WorkflowStep workflowStep, Form form) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndForm(user, step, form);
        if (userProperties.size() == 0) {
            addPropertie(user, step, workflowStep, form);
        } else {
            int nbUpdated = 0;
            for(UserPropertie userPropertie : userProperties) {
                List<String> recipientEmails = new ArrayList<>();
                for(User oneUser : workflowStep.getUsers()) {
                    recipientEmails.add(oneUser.getEmail());
//                    recipientRepository.save(recipient);
                }
                if(userPropertie.getRecipients().containsAll(recipientEmails)) {
                    List<String> favoritesEmails = getFavoritesEmails(user, step, form);
                    favoritesEmails.removeAll(userPropertie.getRecipients());
                    if(favoritesEmails.size() > 0) {
                        userPropertie.setScore(userPropertie.getScore() + 1);
                    }
                    nbUpdated++;
                }
                userPropertieRepository.save(userPropertie);
            }
            if(nbUpdated == 0) {
                addPropertie(user, step, workflowStep, form);
            }
        }
    }

    private void addPropertie(User user, int step, WorkflowStep workflowStep, Form form) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.setStep(step);
        userPropertie.setForm(form);
        userPropertie.setScore(1);
        for(User oneUser : workflowStep.getUsers()) {
            userPropertie.getRecipients().add(oneUser.getEmail());
        }
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
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

    public List<String> getFavoritesEmails(User user, int step, Form form) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndForm(user, step, form);
        List<String> favoriteRecipients = new ArrayList<>();
        int bestScore = 0;
        for (UserPropertie userPropertie : userProperties) {
            if(userPropertie.getScore() > bestScore) {
                favoriteRecipients.clear();
                favoriteRecipients.addAll(userPropertie.getRecipients());
                bestScore = userPropertie.getScore();
            }
        }
        return favoriteRecipients;
    }

}
