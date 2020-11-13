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

    public void createUserPropertie(User user, int step, String userEmail,  Workflow workflow) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndWorkflowName(user, step, workflow.getName());
        if (userProperties.size() == 0) {
            addPropertie(user, step, userEmail, workflow);
        } else {
            int nbUpdated = 0;
            for(UserPropertie userPropertie : userProperties) {
                if(userPropertie.getRecipients().contains(userEmail)) {
                    List<String> favoritesEmails = getFavoritesEmails(user, step, workflow);
                    favoritesEmails.removeAll(userPropertie.getRecipients());
                    if(favoritesEmails.size() > 0) {
                        userPropertie.setScore(userPropertie.getScore() + 1);
                    }
                    nbUpdated++;
                }
                userPropertieRepository.save(userPropertie);
            }
            if(nbUpdated == 0) {
                addPropertie(user, step, userEmail, workflow);
            }
        }
    }

    private void addPropertie(User user, int step, String userEmail, Workflow workflow) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.setStep(step);
        userPropertie.setWorkflowName(workflow.getName());
        userPropertie.setScore(1);
        userPropertie.getRecipients().add(userEmail);
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
    }

    public void createTargetPropertie(User user, String targetEmail, Workflow workflow) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndTargetEmailAndWorkflowName(user, targetEmail, workflow.getName());
        if (userProperties.size() == 0) {
            UserPropertie userPropertie = new UserPropertie();
            userPropertie.setStep(0);
            userPropertie.setWorkflowName(workflow.getName());
            userPropertie.setTargetEmail(targetEmail);
            userPropertie.setUser(user);
            userPropertieRepository.save(userPropertie);
        } else {
            UserPropertie userPropertie = userProperties.get(0);
            userPropertie.setScore(userPropertie.getScore() + 1);
            userPropertieRepository.save(userPropertie);
        }
    }

    public List<String> getFavoritesEmails(User user, int step, Workflow workflow) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndStepAndWorkflowName(user, step, workflow.getName());
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
