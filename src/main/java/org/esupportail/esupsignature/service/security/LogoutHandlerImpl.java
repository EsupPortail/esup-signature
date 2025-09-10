package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class LogoutHandlerImpl implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogoutHandlerImpl.class);

    private final GlobalProperties globalProperties;
    private final SessionRegistry sessionRegistry;
    private final List<SecurityService> securityServices;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public LogoutHandlerImpl(GlobalProperties globalProperties, SessionRegistry sessionRegistry, List<SecurityService> securityServices) {
        this.globalProperties = globalProperties;
        this.sessionRegistry = sessionRegistry;
        this.securityServices = securityServices;
    }

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
                String state = UUID.randomUUID().toString();
                ResponseCookie stateCookie = ResponseCookie.from("logout_state", state)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .build();
                httpServletResponse.addHeader("Set-Cookie", stateCookie.toString());
                for (SecurityService securityService : securityServices) {
                    if (securityService.getCode().equals(securityServiceName)) {
                        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser && securityService instanceof OidcOtpSecurityService) {
                            String idToken = oidcUser.getIdToken().getTokenValue();
                            String endSessionEndpoint = ((OidcOtpSecurityService) securityService).getLogoutUrl();
                            String redirectUri = globalProperties.getRootUrl().replaceAll("/+$", "") +
                                    "/" + securityService.getLoggedOutUrl().replaceAll("^/+", "");
                            String logoutUrl = endSessionEndpoint +
                                    "?id_token_hint=" + idToken +
                                    "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
                            redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, logoutUrl);
                            return;
                        }
                    } else {
                        if (securityService.getCode().equalsIgnoreCase(securityServiceName + "SecurityServiceImpl")) {
                            String redirectUri = globalProperties.getRootUrl() + "/logged-out";
                            String logoutUrl = securityService.getLoggedOutUrl()
                                    + "?service=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                            redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, logoutUrl);
                            return;
                        }
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
