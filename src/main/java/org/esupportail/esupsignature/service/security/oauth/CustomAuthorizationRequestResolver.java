package org.esupportail.esupsignature.service.security.oauth;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    private final String acr;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String acr) {
        this.defaultAuthorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        this.acr = acr;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve(request);
        if(authorizationRequest != null) {
            return customAuthorizationRequest(authorizationRequest, request);
        }
        return null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = this.defaultAuthorizationRequestResolver.resolve( request, clientRegistrationId);
        if(authorizationRequest != null) {
            return customAuthorizationRequest(authorizationRequest, request);
        }
        return null;
    }

    private OAuth2AuthorizationRequest customAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
        Map<String, Object> additionalParameters = new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("acr_values", acr);
        return OAuth2AuthorizationRequest.from(authorizationRequest).additionalParameters(additionalParameters).build();
    }
}
