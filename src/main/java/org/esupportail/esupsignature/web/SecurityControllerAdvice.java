package org.esupportail.esupsignature.web;

import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp", "org.esupportail.esupsignature.web.wssecure"})
public class SecurityControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    @Resource
    private UserService userService;

    @ModelAttribute(value = "userEppn")
    public String getUserEppn(HttpSession httpSession) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = userService.tryGetEppnFromLdap(auth);
            if (httpSession.getAttribute("suEppn") != null) {
                eppn = httpSession.getAttribute("suEppn").toString();
            }
            logger.debug("userEppn used is : " + eppn);
            return eppn;
        } else {
            return null;
        }
    }

    @ModelAttribute(value = "authUserEppn")
    public String getAuthUserEppn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !auth.getName().equals("anonymousUser")) {
            String eppn = userService.tryGetEppnFromLdap(auth);
            logger.debug("authUserEppn used is : " + eppn);
            return eppn;
        } else {
            return null;
        }
    }

}
