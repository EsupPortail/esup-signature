package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.dto.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class HttpSessionsListenerService {

    Logger logger = LoggerFactory.getLogger(HttpSessionsListenerService.class);

    Map<String, HttpSession> sessions = new HashMap<>();

    @EventListener
    public void onHttpSessionCreatedEvent(HttpSessionCreatedEvent event) {
        String id = event.getSession().getId();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String remoteIp = request.getRemoteAddr();
        String originRequestUri = request.getRequestURI();
        Date createdDate = new Date();
        HttpSession session = new HttpSession();
        session.setSessionId(id);
        session.setRemoteIp(remoteIp);
        session.setCreatedDate(createdDate);
        session.setOriginRequestUri(originRequestUri);
        sessions.put(id, session);
        logger.info("Session created: " + id);
    }

    @EventListener
    public void onHttpSessionDestroyedEvent(HttpSessionDestroyedEvent event) {
        sessions.remove(event.getSession().getId());
    }

    public Map<String, HttpSession> getSessions() {
        return sessions;
    }

}
