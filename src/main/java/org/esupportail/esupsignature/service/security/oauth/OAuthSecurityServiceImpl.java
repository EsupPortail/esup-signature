package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.web.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class OAuthSecurityServiceImpl implements SecurityService {

	@Resource
	private WebSecurityProperties webSecurityProperties;

	@Resource
	private OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler;
	
	public OAuthAuthenticationSuccessHandler getoAuthAuthenticationSuccessHandler() {
		return oAuthAuthenticationSuccessHandler;
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
		OAuth2LoginAuthenticationProvider auth2LoginAuthenticationProvider = new OAuth2LoginAuthenticationProvider(new DefaultAuthorizationCodeTokenResponseClient(), new DefaultOAuth2UserService());
		return auth2LoginAuthenticationProvider ;
	}
	
	
	
}
