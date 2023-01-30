package org.esupportail.esupsignature.service.utils.logging;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class MDCFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if(SecurityContextHolder.getContext().getAuthentication() != null) {
                String name = SecurityContextHolder.getContext().getAuthentication().getName();
                MDC.put("userId", name);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
