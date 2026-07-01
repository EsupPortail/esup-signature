package org.esupportail.esupsignature.service.security.oauth.azuread;

import org.apereo.cas.client.util.AbstractConfigurationFilter;
import org.esupportail.esupsignature.entity.enums.ExternalAuth;
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
@Order(10)
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.azuread.client-id")
public class AzureAdSecurityServiceImpl implements OidcOtpSecurityService {

    @Override
    public String getTitle() {
        return "Azure Active Directory";
    }

    @Override
    public String getDescription() {
        return """
                Se connecter avec Azure Active Directory.
                Utilisez votre compte Microsoft professionnel ou scolaire.
                <br>
                <b>L'adresse email Azure AD doit être la même que celle qui a reçu le lien de signature.</b>
            """;
    }

    @Override
    public String getCode() {
        return "azuread";
    }

    @Override
    public String getLoginUrl() {
        return "/login/azureadentry";
    }

    @Override
    public String getLoggedOutUrl() {
        return "/logged-out";
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/azuread");
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
        return null;
    }

    @Override
    public ExternalAuth getExternalAuth() {
        return ExternalAuth.azuread;
    }

    @Override
    public SignatureAlgorithm getSignatureAlgorithm() {
        return SignatureAlgorithm.RS256;
    }

    @Override
    public Map<String, Object> getAdditionalAuthorizationParameters() {
        Map<String, Object> params = new HashMap<>();
        return params;
    }

    @Override
    public AbstractConfigurationFilter getSingleSignOutFilter() {
        return null;
    }
}
