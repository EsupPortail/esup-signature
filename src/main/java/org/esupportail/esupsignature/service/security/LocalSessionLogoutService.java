package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class LocalSessionLogoutService {

    private final JdbcIndexedSessionRepository sessionRepository;
    private final SessionRegistry sessionRegistry;
    private final SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();

    public LocalSessionLogoutService(JdbcIndexedSessionRepository sessionRepository, SessionRegistry sessionRegistry) {
        this.sessionRepository = sessionRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public void logoutCurrentSession(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : null;

        securityContextLogoutHandler.logout(request, response, authentication);

        logoutSessionById(sessionId);

        SecurityContextHolder.clearContext();
    }

    public void logoutSessionById(String sessionId) {
        if (sessionId == null) {
            return;
        }

        SessionInformation sessionInformation = sessionRegistry.getSessionInformation(sessionId);
        if (sessionInformation != null) {
            sessionInformation.expireNow();
        }

        sessionRepository.deleteById(sessionId);
    }
}
