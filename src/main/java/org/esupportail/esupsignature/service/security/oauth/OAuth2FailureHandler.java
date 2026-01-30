package org.esupportail.esupsignature.service.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (!exception.getMessage().contains("access_denied")) {
            logger.info(exception.getMessage());
        } else {
            logger.warn(exception.getMessage(), exception);
        }
        request.getSession().setAttribute("errorMsg", exception.getMessage());
        response.sendRedirect("/otp-access/oauth2");
    }
}