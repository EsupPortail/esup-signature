package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.repository.UserPropertieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    public List<String> getFavoritesEmails(String userEppn) {
        List<String> favoriteUserEmails = new ArrayList<>();
        UserPropertie userPropertie = getUserProperties(userEppn);
        if(userPropertie != null) {
            Map<User, Date> favorites = userPropertie.getFavorites();
            if (favorites.size() > 0) {
                List<Map.Entry<User, Date>> entrySet = new ArrayList<>(favorites.entrySet());
                entrySet.sort(Map.Entry.<User, Date>comparingByValue().reversed());
                for (int i = 0; i < Math.min(entrySet.size(), 5); i++) {
                    favoriteUserEmails.add(entrySet.get(i).getKey().getEmail());
                }
            }
        }
        return favoriteUserEmails;
    }

    public UserPropertie getUserProperties(String userEppn) {
        return userPropertieRepository.findByUserEppn(userEppn);
    }

    public UserPropertie  getUserPropertiesByUserEppn(String userEppn) {
        return userPropertieRepository.findByUserEppn(userEppn);
    }

    @Transactional
    public void delete(Long id) {
        UserPropertie userPropertie = getById(id);
        userPropertieRepository.delete(userPropertie);
    }

    @Transactional
    public void delete(String authUserEppn, Long id) {
        UserPropertie userPropertie = getUserPropertiesByUserEppn(authUserEppn);
        User user = userService.getById(id);
        userPropertie.getFavorites().remove(user);
    }
}
