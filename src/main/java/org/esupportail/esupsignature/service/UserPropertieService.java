package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

    public void createUserPropertieFromMails(User user, List<String> recipientEmails) {
        for (String recipientEmail : recipientEmails) {
            User favoriteUser = userService.getUserByEmail(recipientEmail);
            createUserPropertie(user, favoriteUser);
        }
    }

    public void createUserPropertie(User user, User favoriteUser) {
        List<UserPropertie> userProperties = getUserProperties(user.getEppn());
        if (userProperties == null || userProperties.size() == 0) {
            addPropertie(user, favoriteUser);
        } else {
            for(UserPropertie userPropertie : userProperties) {
                if(!userPropertie.getFavorites().containsKey(favoriteUser)) {
                    userPropertie.getFavorites().put(favoriteUser, new Date());
                    userPropertieRepository.save(userPropertie);
                }
            }
        }
    }

    private void addPropertie(User user, User favoriteUser) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.getFavorites().put(favoriteUser, new Date());
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
    }

    public List<String> getFavoritesEmails(String userEppn) {
        List<UserPropertie> userProperties = getUserProperties(userEppn);
        Map<User, Date> favorites = new HashMap<>();
        for(UserPropertie userPropertie : userProperties) {
            favorites.putAll(userPropertie.getFavorites());
        }
        List<Map.Entry<User, Date>> entrySet = new ArrayList<>(favorites.entrySet());
        entrySet.sort(Map.Entry.<User, Date>comparingByValue().reversed());
        return entrySet.stream().map(Map.Entry::getKey).map(User::getEmail).limit(5).collect(Collectors.toList());
    }

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
}
