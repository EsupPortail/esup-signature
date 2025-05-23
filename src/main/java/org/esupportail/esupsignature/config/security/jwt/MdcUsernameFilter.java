package org.esupportail.esupsignature.config.security.jwt;

import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MdcUsernameFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MdcUsernameFilter.class);

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return SkippingUrlFilter.shouldSkip(request);
    }

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            MDC.put("username", authentication.getName());
            LOGGER.debug("User [{}] => {}", authentication.getName(), authentication.getAuthorities());
        } else {
            MDC.put("username", "anonymous");
            LOGGER.debug("User [anonymous]");
        }

        try {
            assert filterChain != null;
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("username");
        }
    }
}