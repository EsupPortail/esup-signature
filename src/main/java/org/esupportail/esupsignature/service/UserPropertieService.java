package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UserPropertieService {

    @Resource
    private UserPropertieRepository userPropertieRepository;

    @Resource
    private UserService userService;

    public void createUserPropertieFromMails(User user, List<String> recipientEmails) {
        for (String recipientEmail : recipientEmails) {
            User favoriteUser = userService.getUserByEmail(recipientEmail);
            createUserPropertie(user, favoriteUser);
        }
    }

    public void createUserPropertie(User user, User favoriteUser) {
        UserPropertie userPropertie = getUserProperties(user.getEppn());
        if (userPropertie == null) {
            addPropertie(user, favoriteUser);
        } else {
            userPropertie.getFavorites().put(favoriteUser, new Date());
            userPropertieRepository.save(userPropertie);
        }
    }

    private void addPropertie(User user, User favoriteUser) {
        UserPropertie userPropertie = new UserPropertie();
        userPropertie.getFavorites().put(favoriteUser, new Date());
        userPropertie.setUser(user);
        userPropertieRepository.save(userPropertie);
    }

    public List<User> getFavoritesEmails(String userEppn) {
        UserPropertie userPropertie = getUserProperties(userEppn);
        List<User> favoriteUsers = new ArrayList<>();
        for (int i = 0 ; i < 5 ; i++) {
            Date mostRecentDate = new Date(0);
            User bestFavoriteUser = null;
            for (User user : userPropertie.getFavorites().keySet()) {
                if (userPropertie.getFavorites().get(user).after(mostRecentDate)) {
                    mostRecentDate = userPropertie.getFavorites().get(user);
                    bestFavoriteUser = user;
                }
            }
            favoriteUsers.add(bestFavoriteUser);
            userPropertie.getFavorites().remove(bestFavoriteUser);
        }
        return favoriteUsers;
    }

    public UserPropertie getUserProperties(String userEppn) {
        return userPropertieRepository.findByUserEppn(userEppn);
    }

    public UserPropertie  getUserPropertiesByUserEppn(String userEppn) {
        return userPropertieRepository.findByUserEppn(userEppn);
    }

    public void delete(UserPropertie userPropertie) {
        userPropertieRepository.delete(userPropertie);
    }
}
