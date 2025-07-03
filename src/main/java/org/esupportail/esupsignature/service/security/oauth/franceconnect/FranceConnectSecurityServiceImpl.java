package org.esupportail.esupsignature.service.security.oauth.franceconnect;

import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Order(4)
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.franceconnect.client-id")
public class FranceConnectSecurityServiceImpl implements OidcOtpSecurityService {

	private final ClientRegistrationRepository clientRegistrationRepository;

    public FranceConnectSecurityServiceImpl(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
	public String getTitle() {
		return "FranceConnect";
	}

	@Override
	public String getDescription() {
		return """
            J’utilise FranceConnect pour créer mon compte ou me connecter.
            FranceConnect est la solution proposée par l’État pour sécuriser et simplifier la connexion aux services publics en ligne.
            <br>
            <b>L'adresse email FranceConnect doit être la même que celle qui a reçu le lien de signature.</b>
            """;
	}

	@Override
	public String getCode() {
		return "franceconnect";
	}

	@Override
	public String getLoginUrl() {
		return "/login/franceconnectentry";
	}

	@Override
	public String getLogoutUrl() {
		ClientRegistration registration = clientRegistrationRepository
				.findByRegistrationId(getCode());

		if (registration != null) {
			return registration.getProviderDetails().getConfigurationMetadata()
					.getOrDefault("end_session_endpoint", "")
					.toString();
		}
		return "";
	}

	@Override
	public ExternalAuth getExternalAuth() {
		return ExternalAuth.franceconnect;
	}

	@Override
	public String getLoggedOutUrl() {
		return "/logged-out";
	}

	@Override
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/franceconnect");
	}

	@Override
	public OAuth2LoginAuthenticationFilter getAuthenticationProcessingFilter() {
		return null;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return null;
	}

	
    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

	@Override
	public SignatureAlgorithm getSignatureAlgorithm() {
		return SignatureAlgorithm.RS256;
	}

	@Override
	public Map<String, Object> getAdditionalAuthorizationParameters() {
		Map<String, Object> params = new HashMap<>();
		params.put("acr_values", "eidas1");
		return params;
	}


}
