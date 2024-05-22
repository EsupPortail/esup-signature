package org.esupportail.esupsignature.service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;

public class IndexEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public IndexEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                         AuthenticationException authException) throws IOException {
        if(httpServletRequest.getRequestURI().contains("/ws/")) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            String redirectUrl = buildRedirectUrlToLoginPage(httpServletRequest, httpServletResponse, authException);
            this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, redirectUrl);
        }
    }
}
