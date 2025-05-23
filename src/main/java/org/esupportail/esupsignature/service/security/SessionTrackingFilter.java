package org.esupportail.esupsignature.service.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.dto.view.HttpSessionViewDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class SessionTrackingFilter implements Filter {

    Map<String, HttpSessionViewDto> sessions = new HashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req) {
            HttpSession session = req.getSession(false);
            if (session != null && !sessions.containsKey(session.getId())) {
                HttpSessionViewDto dto = new HttpSessionViewDto();
                dto.setSessionId(session.getId());
                dto.setRemoteIp(req.getRemoteAddr());
                dto.setCreatedDate(new Date());
                dto.setOriginRequestUri(req.getRequestURI());
                sessions.put(session.getId(), dto);
            }
        }
        chain.doFilter(request, response);
    }

    public Map<String, HttpSessionViewDto> getSessions() {
        return sessions;
    }
}