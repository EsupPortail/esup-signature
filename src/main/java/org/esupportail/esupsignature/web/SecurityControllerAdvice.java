package org.esupportail.esupsignature.web;

import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp", "org.esupportail.esupsignature.web.wssecure"})
public class SecurityControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    @Resource
    private UserService userService;

    @ModelAttribute(value = "userEppn")
    public String getUserEppn(HttpSession httpSession) {
        String eppn = null;
        if(httpSession != null && httpSession.getAttribute("userEppn") != null) {
            eppn = httpSession.getAttribute("userEppn").toString();
        }
        if (httpSession != null && httpSession.getAttribute("suEppn") != null) {
            eppn = httpSession.getAttribute("suEppn").toString();
        }
        if(eppn == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && !auth.getName().equals("anonymousUser")) {
                eppn = userService.tryGetEppnFromLdap(auth);
                assert httpSession != null;
                httpSession.setAttribute("userEppn", eppn);
            }
        }
        logger.debug("userEppn used is : " + eppn);
        return eppn;
    }

    @ModelAttribute(value = "authUserEppn")
    public String getAuthUserEppn(HttpSession httpSession) {
        String eppn = null;
        if(httpSession != null && httpSession.getAttribute("authUserEppn") != null) {
            eppn = httpSession.getAttribute("authUserEppn").toString();
        }
        if(eppn == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && !auth.getName().equals("anonymousUser")) {
                eppn = userService.tryGetEppnFromLdap(auth);
                assert httpSession != null;
                httpSession.setAttribute("authUserEppn", eppn);
            }
        }
        logger.debug("authUserEppn used is : " + eppn);
        return eppn;
    }

}
