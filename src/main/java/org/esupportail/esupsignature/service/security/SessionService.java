package org.esupportail.esupsignature.service.security;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SessionService {

    @Resource
    private SessionRepository<Session> sessionRepository;

    @Resource
    @Qualifier("sessionRegistry")
    private SessionRegistry sessionRegistry;

    @PersistenceContext
    private EntityManager entityManager;

    public int countSessions() {
        return entityManager.createNativeQuery("select session_id from spring_session where last_access_time < expiry_time and principal_name is not null").getResultList().size();
    }

    public List<String> getSessionIds() {
        return entityManager.createNativeQuery("select session_id from spring_session").getResultList();
    }

    public List<String> getSessionPrincipalNames(String sessionId) {
        return entityManager.createNativeQuery("select principal_name from spring_session where session_id = '" + sessionId + "'").getResultList();
    }

    public Session getSessionById(String id) {
        return sessionRepository.findById(id);
    }

    public void deleteSessionById(String id) {
        Session session = getSessionById(id);
        if(session != null) {
            sessionRepository.deleteById(session.getId());
        }
    }

    public Map<String, List<Session>> getAllSessionsListMap() {
        Map<String, List<Session>> allSessions = new HashMap<>();
        List<String> sessionIds = getSessionIds();
        for(String sessionId : sessionIds) {
            List<Session> sessions = new ArrayList<>();
            Session session = getSessionById(sessionId);
            if(session != null) {
                SessionInformation sessionInformation = sessionRegistry.getSessionInformation(sessionId);
                sessions.add(session);
                if(sessionInformation != null && sessionInformation.getPrincipal() instanceof LdapUserDetailsImpl ldapUserDetails) {
                    if(!allSessions.containsKey(ldapUserDetails.getUsername())) {
                        allSessions.put(ldapUserDetails.getUsername(), sessions);
                    } else {
                        allSessions.get(ldapUserDetails.getUsername()).addAll(sessions);
                    }
                } else {
                    List<String> userNames = getSessionPrincipalNames(sessionId);
                    if(userNames.get(0) != null) {
                        if(!allSessions.containsKey(userNames.get(0))) {
                            allSessions.put(userNames.get(0), sessions);
                        } else {
                            allSessions.get(userNames.get(0)).addAll(sessions);
                        }
                    }
                }
            }
        }
        return allSessions;
    }


    public long getSessionsSize(List<Session> sessions) {
        long sessionSize = 0;
        for(Session session : sessions) {
            if (session != null) {
                for (String attr : session.getAttributeNames()) {
                    sessionSize += session.getAttribute(attr).toString().getBytes().length;
                }
            }
        }
        return sessionSize;
    }

}
