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
    public static final String AFTER_OAUTH_FAILURE_REDIRECT = "after_oauth_failure_redirect";
    private static final String ACCESS_DENIED = "access_denied";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String error = request.getParameter("error");
        String errorDescription = request.getParameter("error_description");
        String state = request.getParameter("state");

        if (ACCESS_DENIED.equals(error)) {
            logger.info("OAuth2/OIDC authentication cancelled by user - description: {}, state: {}",
                    errorDescription, state);
            String failureRedirect = getSafeFailureRedirect(request);
            if (failureRedirect != null) {
                request.getSession().removeAttribute(AFTER_OAUTH_FAILURE_REDIRECT);
                request.getSession().removeAttribute("after_oauth_redirect");
                response.sendRedirect(failureRedirect);
                return;
            }
        }

        logger.error("OAuth2 authentication failure", exception);
        logger.warn("OAuth2/OIDC error received - error: {}, description: {}, state: {}",
                error, errorDescription, state);

        StringBuilder redirectUrl = new StringBuilder("/otp-access/oauth2?");
        boolean hasParam = false;

        if (error != null && !error.isEmpty()) {
            redirectUrl.append("error=").append(java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            hasParam = true;
        }
        if (errorDescription != null && !errorDescription.isEmpty()) {
            if (hasParam) {
                redirectUrl.append("&");
            }
            redirectUrl.append("error_description=").append(java.net.URLEncoder.encode(errorDescription, java.nio.charset.StandardCharsets.UTF_8));
            hasParam = true;
        }
        if (exception.getMessage() != null && !exception.getMessage().isEmpty()) {
            if (hasParam) {
                redirectUrl.append("&");
            }
            redirectUrl.append("internal_error=").append(java.net.URLEncoder.encode(exception.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
            hasParam = true;
        }
        if (state != null && !state.isEmpty()) {
            if (hasParam) {
                redirectUrl.append("&");
            }
            redirectUrl.append("state=").append(java.net.URLEncoder.encode(state, java.nio.charset.StandardCharsets.UTF_8));
        }

        response.sendRedirect(redirectUrl.toString());
    }

    private String getSafeFailureRedirect(HttpServletRequest request) {
        Object redirect = request.getSession().getAttribute(AFTER_OAUTH_FAILURE_REDIRECT);
        if (redirect == null) {
            return null;
        }
        String redirectUrl = redirect.toString();
        if (redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")) {
            return redirectUrl + (redirectUrl.contains("?") ? "&" : "?") + "oauth2_cancelled=true";
        }
        logger.warn("Ignoring unsafe OAuth2 failure redirect URL: {}", redirectUrl);
        return null;
    }
}
