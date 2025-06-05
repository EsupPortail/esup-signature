package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.service.security.OidcSecurityService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

import java.util.HashMap;
import java.util.Map;

public class ProConnectSecurityServiceImpl implements OidcSecurityService {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public ProConnectSecurityServiceImpl(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }


    @Override
    public String getTitle() {
        return "ProConnect";
    }

    @Override
    public String getLoginUrl() {
        return "/login/proconnectentry";
    }

    @Override
    public String getLogoutUrl() {
        return "/logout";
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/proconnect");
    }

    @Override
    public GenericFilterBean getAuthenticationProcessingFilter() {
        return null; // Laisse Spring gérer le filtre OIDC
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return null; // Utilise celui par défaut ou injecte un spécifique si besoin
    }

    @Override
    public JwtDecoder getJwtDecoder() {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("proconnect");
        String jwkSetUri = registration.getProviderDetails().getJwkSetUri();
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Override
    public Map<String, Object> getAdditionalAuthorizationParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("claims", """
                            {
                                "id_token":
                                {
                                    "auth_time":
                                    {
                                        "essential":true
                                    },
                                    "amr":
                                    {
                                         "essential": true
                                    }
                                }
                            }
                            """);
        return params;
    }
}