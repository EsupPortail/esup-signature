package org.esupportail.esupsignature.web.controller;

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

    @ModelAttribute(value = "userEppn")
    public String getUserEppn(HttpSession httpSession) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = auth.getName();
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = (String) httpSession.getAttribute("suEppn");
            }
            return userService.buildEppn(eppn);
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUserEppn")
    public String getAuthUserEppn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            return userService.buildEppn(auth.getName());
        } else {
            return null;
        }
    }
}