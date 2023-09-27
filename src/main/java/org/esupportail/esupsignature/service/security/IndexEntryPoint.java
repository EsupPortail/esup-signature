package org.esupportail.esupsignature.service.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IndexEntryPoint extends LoginUrlAuthenticationEntryPoint {

    public IndexEntryPoint(String loginFormUrl) {
        super(loginFormUrl);
    }

    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                         AuthenticationException authException) throws IOException, ServletException {
        String redirectUrl = buildRedirectUrlToLoginPage(httpServletRequest, httpServletResponse, authException);
        this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, redirectUrl);
    }

}
