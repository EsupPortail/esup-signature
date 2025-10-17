package org.esupportail.esupsignature.web;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = {"org.esupportail.esupsignature.web.controller", "org.esupportail.esupsignature.web.otp", "org.esupportail.esupsignature.web.wssecure"})
public class SecurityControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(SecurityControllerAdvice.class);

    private final UserService userService;

    public SecurityControllerAdvice(UserService userService) {
        this.userService = userService;
    }

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
                logger.debug("auth name founded : " + auth.getName());
                eppn = userService.tryGetEppnFromLdap(auth);
                assert httpSession != null;
                if (!StringUtils.hasText((String) httpSession.getAttribute("userEppn")) && StringUtils.hasText(eppn)) {
                    httpSession.setAttribute("userEppn", eppn);
                }
            } else {
                logger.debug("no auth name founded");
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
                logger.debug("auth name founded : " + auth.getName());
                eppn = userService.tryGetEppnFromLdap(auth);
                assert httpSession != null;
                if (!StringUtils.hasText((String) httpSession.getAttribute("authUserEppn")) && StringUtils.hasText(eppn)) {
                    httpSession.setAttribute("authUserEppn", eppn);
                }
            } else {
                logger.debug("no auth name founded");
            }
        }
        logger.debug("authUserEppn used is : " + eppn);
        return eppn;
    }

}
