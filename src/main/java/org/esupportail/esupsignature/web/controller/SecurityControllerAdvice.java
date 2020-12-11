package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class SecurityControllerAdvice {

    @Resource
    private UserService userService;

    @ModelAttribute(value = "user", binding = false)
    public User getUser() {
        return getCurrentUser();
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        return getUserFromAuthentication();
    }

    private User getUserFromAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String eppn = auth.getName();
            return userService.getUserByEppn(eppn);
        } else {
            return null;
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String eppn = auth.getName();
            if (userService.getSuEppn() != null) {
                eppn = userService.getSuEppn();
            }
            return userService.getUserByEppn(eppn);
        } else {
            return null;
        }
    }


}
