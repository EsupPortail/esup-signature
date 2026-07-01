package org.esupportail.esupsignature.service.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String CODE_CHALLENGE = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String CODE_VERIFIER = "code_verifier";

    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    private final List<OidcOtpSecurityService> securityServices;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, List<OidcOtpSecurityService> securityServices) {
        this.defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        this.securityServices = securityServices;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request);
        if(authorizationRequest != null) {
            return customAuthorizationRequest(authorizationRequest);
        }
        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request, clientRegistrationId);
        if(authorizationRequest != null) {
            return customAuthorizationRequest(authorizationRequest);
        }
        return null;
    }

    private OAuth2AuthorizationRequest customAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        Map<String, Object> additionalParameters = new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        Map<String, Object> attributes = new LinkedHashMap<>(authorizationRequest.getAttributes());

        String registrationId = authorizationRequest.getAttributes().get("registration_id").toString();
        OidcOtpSecurityService currentSecurityService = securityServices.stream()
                .filter(s -> s.getCode().equals(registrationId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No security service configured for registration id " + registrationId));

        if ("franceconnect".equals(registrationId)) {
            additionalParameters.remove(CODE_CHALLENGE);
            additionalParameters.remove(CODE_CHALLENGE_METHOD);
            additionalParameters.remove(CODE_VERIFIER);
            attributes.remove(CODE_VERIFIER);
            attributes.remove(CODE_CHALLENGE);
            attributes.remove(CODE_CHALLENGE_METHOD);
            attributes.remove("org.springframework.security.oauth2.client.endpoint.code_verifier");
        }
        additionalParameters.putAll(currentSecurityService.getAdditionalAuthorizationParameters());

        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(authorizationRequest.getAuthorizationUri())
                .clientId(authorizationRequest.getClientId())
                .redirectUri(authorizationRequest.getRedirectUri())
                .scopes(authorizationRequest.getScopes())
                .state(authorizationRequest.getState())
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
    }
}
