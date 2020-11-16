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

    public void createUserPropertie(User user, WorkflowStep workflowStep, List<User> users) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndWorkflowStep(user, workflowStep);
        if (userProperties.size() == 0) {
            addPropertie(user, users, workflowStep);
        } else {
            int nbUpdated = 0;
            for(UserPropertie userPropertie : userProperties) {
                if(userPropertie.getUsers().containsAll(users)) {
                    List<User> favoritesEmails = getFavoritesEmails(user, workflowStep);
                    favoritesEmails.removeAll(userPropertie.getUsers());
                    if(favoritesEmails.size() > 0) {
                        userPropertie.setScore(userPropertie.getScore() + 1);
                    }
                    nbUpdated++;
                }
                userPropertieRepository.save(userPropertie);
            }
            if(nbUpdated == 0) {
                addPropertie(user, users, workflowStep);
            }
        }
    }

    private void addPropertie(User user, List<User> users, WorkflowStep workflowStep) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.setWorkflowStep(workflowStep);
        userPropertie.setScore(1);
        userPropertie.getUsers().addAll(users);
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
    }

    public void createTargetPropertie(User user, WorkflowStep workflowStep, String targetEmail) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndTargetEmailAndWorkflowStep(user, targetEmail, workflowStep);
        if (userProperties.size() == 0) {
            UserPropertie userPropertie = new UserPropertie();
            userPropertie.setWorkflowStep(workflowStep);
            userPropertie.setTargetEmail(targetEmail);
            userPropertie.setUser(user);
            userPropertieRepository.save(userPropertie);
        } else {
            UserPropertie userPropertie = userProperties.get(0);
            userPropertie.setScore(userPropertie.getScore() + 1);
            userPropertieRepository.save(userPropertie);
        }
    }

    public List<User> getFavoritesEmails(User user, WorkflowStep workflowStep) {
        List<UserPropertie> userProperties = userPropertieRepository.findByUserAndWorkflowStep(user, workflowStep);
        List<User> favoriteUsers = new ArrayList<>();
        int bestScore = 0;
        for (UserPropertie userPropertie : userProperties) {
            if(userPropertie.getScore() > bestScore) {
                favoriteUsers.clear();
                favoriteUsers.addAll(userPropertie.getUsers());
                bestScore = userPropertie.getScore();
            }
        }
        return favoriteUsers;
    }

}
