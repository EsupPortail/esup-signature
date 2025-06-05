package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.security.OidcSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FranceConnectSecurityServiceImpl implements OidcSecurityService {

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
	public String getLoginUrl() {
		return "/login/oauth2entry";
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

    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

	public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
		HttpSessionOAuth2AuthorizationRequestRepository repository = new HttpSessionOAuth2AuthorizationRequestRepository();
		return repository;
	}
	
    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

	public AuthenticationManager oAuthAuthenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<>();
		authenticatedAuthenticationProviders.add(auth2LoginAuthenticationProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
	}
	
	public OAuth2LoginAuthenticationProvider auth2LoginAuthenticationProvider() {
		OAuth2LoginAuthenticationProvider auth2LoginAuthenticationProvider = new OAuth2LoginAuthenticationProvider(new RestClientAuthorizationCodeTokenResponseClient(), new DefaultOAuth2UserService());
		return auth2LoginAuthenticationProvider ;
	}

	@Override
	public JwtDecoder getJwtDecoder() {
		ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("franceconnect");
		SecretKeySpec key = new SecretKeySpec(registration.getClientSecret().getBytes(StandardCharsets.UTF_8), "HS256");
		return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
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
