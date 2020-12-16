package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserPropertieService {

    @Resource
    private UserService  userService;

    @Resource
    private UserPropertieRepository userPropertieRepository;

    public void createUserPropertie(User user, WorkflowStep workflowStep, List<User> users) {
        List<UserPropertie> userProperties = getUserProperties(user.getId(), workflowStep.getId());
        if (userProperties.size() == 0) {
            addPropertie(user, users, workflowStep);
        } else {
            for(UserPropertie userPropertie : userProperties) {
                if(userPropertie.getUsers().containsAll(users)) {
                    userPropertie.setScore(userPropertie.getScore() + 1);
                } else {
                    addPropertie(user, users, workflowStep);
                }
                userPropertieRepository.save(userPropertie);
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<User> getFavoritesEmails(Long userId, Long workflowStepId) {
        List<UserPropertie> userProperties = getUserProperties(userId, workflowStepId);
        List<User> favoriteUsers = new ArrayList<>();
        int bestScore = 0;
        for (UserPropertie userPropertie : userProperties) {
            if (userPropertie.getScore() > bestScore) {
                favoriteUsers.clear();
                favoriteUsers.addAll(userPropertie.getUsers());
                bestScore = userPropertie.getScore();
            }
        }
        return favoriteUsers;
    }

    public List<UserPropertie> getUserProperties(Long userId, Long workflowStepId) {
        return userPropertieRepository.findByUserIdAndWorkflowStepId(userId, workflowStepId);
    }

    public List<User> getFavoriteRecipientEmail(int stepNumber, WorkflowStep workflowStep, List<String> recipientEmails, User user) {
        List<User> users = new ArrayList<>();
        if (recipientEmails != null && recipientEmails.size() > 0) {
            recipientEmails = recipientEmails.stream().filter(r -> r.startsWith(String.valueOf(stepNumber))).collect(Collectors.toList());
            for (String recipientEmail : recipientEmails) {
                String userEmail = recipientEmail.split("\\*")[1];
                users.add(userService.getUserByEmail(userEmail));
            }
        } else {
            List<User> favoritesEmail = getFavoritesEmails(user.getId(), workflowStep.getId());
            users.addAll(favoritesEmail);
        }
        return users;
    }

    public List<UserPropertie> getUserPropertiesByUserId(Long userId) {
        return userPropertieRepository.findByUserId(userId);
    }

    public List<UserPropertie> getByWorkflowStep(WorkflowStep workflowStep) {
        return userPropertieRepository.findByWorkflowStep(workflowStep);
    }

    public void delete(UserPropertie userPropertie) {
        userPropertieRepository.delete(userPropertie);
    }
}
