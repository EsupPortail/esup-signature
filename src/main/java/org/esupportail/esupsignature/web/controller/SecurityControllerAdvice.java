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

    @ModelAttribute(value = "userId")

    public Long getUserId(HttpSession httpSession) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = auth.getName();
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = (String) httpSession.getAttribute("suEppn");
            }
            return userService.getUserByEppn(eppn).getId();
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUserId")
    public Long getAuthUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            return userService.getUserByEppn(auth.getName()).getId();
        } else {
            return null;
        }
    }
}
