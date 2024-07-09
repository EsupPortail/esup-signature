package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Set;

@Service
public class HttpSessionsListenerService {

    Logger logger = LoggerFactory.getLogger(HttpSessionsListenerService.class);

    Set<String> sessions = new HashSet<>();

    @EventListener
    public void onHttpSessionCreatedEvent(HttpSessionCreatedEvent event) {
        String id = event.getSession().getId();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String remoteIp = request.getRemoteAddr();
        sessions.add(id);
        logger.info("new session : " + id + " from " + remoteIp);
    }

    @EventListener
    public void onHttpSessionDestroyedEvent(HttpSessionDestroyedEvent event) {
        sessions.remove(event.getSession().getId());
    }

    public Set<String> getSessions() {
        return sessions;
    }
}
