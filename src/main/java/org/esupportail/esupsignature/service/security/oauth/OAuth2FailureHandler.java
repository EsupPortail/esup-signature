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
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");
        String state = request.getParameter("state");
        logger.warn("OAuth2/OIDC error received - error: {}, description: {}, state: {}",
                error, errorDescription, state);
        StringBuilder redirectUrl = new StringBuilder("/otp-access/oauth2?");
        if (error != null && !error.isEmpty()) {
            redirectUrl.append("error=").append(java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (errorDescription != null && !errorDescription.isEmpty()) {
            if (redirectUrl.toString().contains("=")) {
                redirectUrl.append("&");
            }
            redirectUrl.append("error_description=").append(java.net.URLEncoder.encode(errorDescription, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (state != null && !state.isEmpty()) {
            if (redirectUrl.toString().contains("=")) {
                redirectUrl.append("&");
            }
            redirectUrl.append("state=").append(java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8));
        }
        response.sendRedirect(redirectUrl.toString());
    }
}