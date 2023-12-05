package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPropertieService {

    @Resource
    private UserPropertieRepository userPropertieRepository;

    @Resource
    private UserService userService;

    public UserPropertie getById(Long id) {
        return userPropertieRepository.findById(id).get();
    }

    @Transactional
    public void createUserPropertieFromMails(User user, List<WorkflowStepDto> steps) {
	        for (RecipientWsDto recipient : steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList()) {
            User favoriteUser = userService.getUserByEmail(recipient.getEmail());
            if(favoriteUser!= null) {
                createUserProperty(user, favoriteUser);
            }
        }
    }

    @Transactional
    public void createUserProperty(User user, User favoriteUser) {
        List<UserPropertie> userProperties = getUserProperties(user.getEppn());
        if (userProperties == null || userProperties.isEmpty()) {
            addProperty(user, favoriteUser);
        } else {
            for(UserPropertie userPropertie : userProperties) {
                if(!userPropertie.getFavorites().containsKey(favoriteUser)) {
                    userPropertie.getFavorites().put(favoriteUser, new Date());
                    userPropertieRepository.save(userPropertie);
                } else {
                    userPropertie.getFavorites().put(favoriteUser, new Date());
                }
            }
        }
    }

    private void addProperty(User user, User favoriteUser) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.getFavorites().put(favoriteUser, new Date());
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
    }

    @Transactional
    public List<User> getFavoritesEmails(String userEppn) {
        List<UserPropertie> userProperties = getUserProperties(userEppn);
        Map<User, Date> favorites = new HashMap<>();
        for(UserPropertie userPropertie : userProperties) {
            favorites.putAll(userPropertie.getFavorites());
        }
        List<Map.Entry<User, Date>> entrySet = new ArrayList<>(favorites.entrySet());
        entrySet.sort(Map.Entry.<User, Date>comparingByValue().reversed());
        return entrySet.stream().map(Map.Entry::getKey).limit(5).collect(Collectors.toList());
    }

    @Transactional
    public List<UserPropertie> getUserProperties(String userEppn) {
        return userPropertieRepository.findByUserEppn(userEppn);
    }

    @Transactional
    public void delete(Long id) {
        UserPropertie userPropertie = getById(id);
        userPropertieRepository.delete(userPropertie);
    }

    @Transactional
    public void delete(String authUserEppn, Long id) {
        List<UserPropertie> userProperties = getUserProperties(authUserEppn);
        for(UserPropertie userPropertie : userProperties) {
            User user = userService.getById(id);
            userPropertie.getFavorites().remove(user);
        }
    }

    @Transactional
    public void deleteAll(String authUserEppn) {
        User user = userService.getByEppn(authUserEppn);
        List<UserPropertie> userProperties2 = userPropertieRepository.findByFavoritesContains(user);
        for(UserPropertie userPropertie : userProperties2) {
            userPropertie.getFavorites().clear();
        }
        userPropertieRepository.deleteAll(userProperties2);
        List<UserPropertie> userProperties = getUserProperties(authUserEppn);
        for(UserPropertie userPropertie : userProperties) {
            userPropertie.getFavorites().clear();
        }
        userPropertieRepository.deleteAll(userProperties);
    }


}
