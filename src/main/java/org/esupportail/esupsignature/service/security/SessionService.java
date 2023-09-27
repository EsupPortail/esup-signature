package org.esupportail.esupsignature.service.security;

import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class SessionService {

    @Resource
    private SessionRepository<Session> sessionRepository;

    public Session getSessionById(String id) {
        return sessionRepository.findById(id);
    }

    public void deleteSessionById(String id) {
        sessionRepository.deleteById(id);
    }

}
