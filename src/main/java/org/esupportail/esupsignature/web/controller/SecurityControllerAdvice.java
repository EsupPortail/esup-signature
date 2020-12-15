package org.esupportail.esupsignature.web.controller;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller"})
public class SecurityControllerAdvice {

    @Resource
    private UserService userService;

    @ModelAttribute(value = "user", binding = false)
    public User getUser(HttpSession httpSession) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String eppn = auth.getName();
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = (String) httpSession.getAttribute("suEppn");
            }
            return userService.getUserByEppn(eppn);
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUser", binding = false)
    public User getAuthUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            String eppn = auth.getName();
            return userService.getUserByEppn(eppn);
        } else {
            return null;
        }
    }
}
