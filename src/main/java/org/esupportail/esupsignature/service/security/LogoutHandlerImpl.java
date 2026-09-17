package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.service.security.oauth.OidcUserSecurityServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class LogoutHandlerImpl implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogoutHandlerImpl.class);

    private final GlobalProperties globalProperties;
    private final SessionRegistry sessionRegistry;
    private final List<SecurityService> securityServices;
    private final OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver;
    private final Environment environment;

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public LogoutHandlerImpl(GlobalProperties globalProperties, SessionRegistry sessionRegistry, List<SecurityService> securityServices, OidcUserSecurityServiceResolver oidcUserSecurityServiceResolver, Environment environment) {
        this.globalProperties = globalProperties;
        this.sessionRegistry = sessionRegistry;
        this.securityServices = securityServices;
        this.oidcUserSecurityServiceResolver = oidcUserSecurityServiceResolver;
        this.environment = environment;
    }

    @Override
    public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        boolean isOtpUser = authentication != null
                && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_OTP"::equals);
        logger.info("Logout started for user: {}, isOtpUser: {}", authentication != null ? authentication.getName() : "anonymous", isOtpUser);
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if(authentication != null && authentication.getPrincipal().equals(principal)) {
                for(SessionInformation sessionInformation : sessionRegistry.getAllSessions(principal, false)) {
                    logger.debug("Expiring session: {}", sessionInformation.getSessionId());
                    sessionInformation.expireNow();
                }
            }
        }
        try {
            if (isOtpUser) {
                logger.debug("Setting logout_user_type cookie for OTP user");
                ResponseCookie logoutTypeCookie = ResponseCookie.from("logout_user_type", "otp")
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .build();
                httpServletResponse.addHeader("Set-Cookie", logoutTypeCookie.toString());
            }
            HttpSession session = httpServletRequest.getSession(false);
            if(session != null && session.getAttribute("securityServiceName") != null) {
                String securityServiceName = session.getAttribute("securityServiceName").toString();
                logger.info("Security service name found in session: {}", securityServiceName);
                String state = UUID.randomUUID().toString();
                ResponseCookie stateCookie = ResponseCookie.from("logout_state", state)
                        .httpOnly(true)
                        .secure(false)
                        .path("/")
                        .sameSite("Lax")
                        .build();
                httpServletResponse.addHeader("Set-Cookie", stateCookie.toString());
                for (SecurityService securityService : getSecurityServices()) {
                    if (securityService.getCode().equals(securityServiceName)) {
                        logger.debug("Matching security service found: {}", securityService.getCode());
                        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser && securityService instanceof OidcSecurityService oidcSecurityService) {
                            String idToken = oidcUser.getIdToken().getTokenValue();
                            String endSessionEndpoint = oidcSecurityService.getLogoutUrl();
                            String redirectUri = globalProperties.getRootUrl().replaceAll("/+$", "") +
                                    "/" + securityService.getLoggedOutUrl().replaceAll("^/+", "");
                            String logoutUrl = endSessionEndpoint +
                                    "?id_token_hint=" + idToken +
                                    "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                                    "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
                            if(List.of(environment.getActiveProfiles()).contains("dev")) {
                                logger.info("Redirecting to OIDC logout: {}", logoutUrl);
                            } else {
                                logger.info("Redirecting to OIDC logout for security service: {}", securityService.getCode());
                            }
                            redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, logoutUrl);
                            return;
                        }
                    } else {
                        if (securityService.getCode().equalsIgnoreCase(securityServiceName + "SecurityServiceImpl")) {
                            logger.debug("Matching security service found (with suffix): {}", securityService.getCode());
                            String redirectUri = globalProperties.getRootUrl() + "/logged-out";
                            String logoutUrl = securityService.getLoggedOutUrl()
                                    + "?service=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                            logger.info("Redirecting to service logout for security service: {}", securityService.getCode());
                            redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, logoutUrl);
                            return;
                        }
                    }
                }
                logger.warn("No matching security service found for {}", securityServiceName);
            } else {
                logger.info("No security service name in session, redirecting to /logged-out");
                redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/logged-out");
            }
        } catch (IOException e) {
            logger.error("error on logout", e);
        }
    }

    private List<SecurityService> getSecurityServices() {
        List<SecurityService> services = new ArrayList<>(securityServices);
        services.addAll(oidcUserSecurityServiceResolver.getConfiguredServices());
        return services;
    }

}
