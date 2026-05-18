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
                .get();

        if ("franceconnect".equals(registrationId)) {
            additionalParameters.remove("code_challenge");
            additionalParameters.remove("code_challenge_method");
            attributes.remove("org.springframework.security.oauth2.client.endpoint.code_verifier");
            attributes.remove("code_challenge");
            attributes.remove("code_challenge_method");
        }
        additionalParameters.putAll(currentSecurityService.getAdditionalAuthorizationParameters());

        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(authorizationRequest.getAuthorizationUri())
                .clientId(authorizationRequest.getClientId())
                .redirectUri(authorizationRequest.getRedirectUri())
                .scopes(authorizationRequest.getScopes())
                .state(authorizationRequest.getState())
                .attributes(attributes)
                .additionalParameters(additionalParameters)
                .build();
        return oAuth2AuthorizationRequest;
    }
}
