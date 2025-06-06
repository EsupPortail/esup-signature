package org.esupportail.esupsignature.service.security.oauth.franceconnect;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.HashMap;
import java.util.Map;

public class FranceConnectSecurityServiceImpl implements OidcOtpSecurityService {

	private final WebSecurityProperties webSecurityProperties;
	private final ClientRegistrationRepository clientRegistrationRepository;

	public FranceConnectSecurityServiceImpl(WebSecurityProperties webSecurityProperties, @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) {
        this.webSecurityProperties = webSecurityProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

	@Override
	public String getTitle() {
		return "FranceConnect";
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
		return webSecurityProperties.getFranceConnectLogoutEndpoint();
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
	public JwtDecoder getJwtDecoder() {
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("franceconnect");
		String jwkSetUri = registration.getProviderDetails().getJwkSetUri();
		return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
				.jwsAlgorithm(SignatureAlgorithm.RS256)
				.build();
	}

	@Override
	public Map<String, Object> getAdditionalAuthorizationParameters() {
		Map<String, Object> params = new HashMap<>();
		params.put("acr_values", "eidas1");
		return params;
	}

	@Bean
	@Conditional(ClientsConfiguredCondition.class)
	public JwtDecoderFactory<ClientRegistration> franceConnectJwtDecoderFactory() {
		final JwtDecoder decoder = getJwtDecoder();
		return context -> decoder;
	}

}
