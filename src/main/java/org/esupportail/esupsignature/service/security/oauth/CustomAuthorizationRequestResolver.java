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
        String registrationId = authorizationRequest.getAttributes().get("registration_id").toString();
        OidcOtpSecurityService currentSecurityService = securityServices.stream().filter(s -> s.getCode().equals(registrationId)).findFirst().get();
        additionalParameters.putAll(currentSecurityService.getAdditionalAuthorizationParameters());
        String customState = authorizationRequest.getState();
//        String json = "{\"redirect\":\"/otp/\",\"csrf\":\"" + customState + "\"}";
//        customState = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return OAuth2AuthorizationRequest.from(authorizationRequest)
//                .state(customState)
                .additionalParameters(additionalParameters).build();
    }
}
