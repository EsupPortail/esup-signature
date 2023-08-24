package org.esupportail.esupsignature.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class LogoutHandlerImpl implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogoutHandlerImpl.class);

    @Resource
    private SessionRegistry sessionRegistry;

    @Resource
    List<SecurityService> securityServices;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {

        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if(authentication != null && authentication.getPrincipal().equals(principal)) {
                for(SessionInformation sessionInformation : sessionRegistry.getAllSessions(principal, false)) {
                    sessionInformation.expireNow();
                }
            }
        }
        try {
            if(httpServletRequest.getSession().getAttribute("securityServiceName") != null) {
                String securityServiceName = httpServletRequest.getSession().getAttribute("securityServiceName").toString();
                for (SecurityService securityService : securityServices) {
                    if (securityService.getClass().getSimpleName().equals(securityServiceName)) {
                        redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, securityService.getLogoutUrl());
                    }
                }
            } else {
                redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/logged-out");
            }
        } catch (IOException e) {
            logger.error("error on logout", e);
        }
    }

}
