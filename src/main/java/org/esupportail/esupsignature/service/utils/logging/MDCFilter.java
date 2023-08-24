package org.esupportail.esupsignature.service.utils.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

//@Component
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
