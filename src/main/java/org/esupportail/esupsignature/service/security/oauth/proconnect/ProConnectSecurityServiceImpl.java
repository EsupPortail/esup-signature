package org.esupportail.esupsignature.service.security.oauth.proconnect;

import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.GenericFilterBean;

import java.util.HashMap;
import java.util.Map;

@Service
@Order(3)
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.proconnect.client-id")
public class ProConnectSecurityServiceImpl implements OidcOtpSecurityService {

    @Override
    public String getTitle() {
        return "ProConnect";
    }

    @Override
    public String getDescription() {
        return """
                Se connecter avec ProConnect.
                ProConnect est le moyen d'authentification commun des services numériques de l'État pour les professionnels.
                <br>
                <b>L'adresse email FranceConnect doit être la même que celle qui a reçu le lien de signature.</b>
            """;
    }

    @Override
    public String getCode() {
        return "proconnect";
    }

    @Override
    public String getLoginUrl() {
        return "/login/proconnectentry";
    }



    @Override
    public String getLoggedOutUrl() {
        return "/logged-out";
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/proconnect");
    }

    @Override
    public GenericFilterBean getAuthenticationProcessingFilter() {
        return null;
    }

    @Override
    public UserDetailsService getUserDetailsService() {
        return null;
    }

    @Override
    public String getLogoutUrl() {
        return "";
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return SignatureAlgorithm.RS256;
    }

    @Override
    public Map<String, Object> getAdditionalAuthorizationParameters() {
        Map<String, Object> params = new HashMap<>();
//        params.put("acr_values", List.of("eidas1"));
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