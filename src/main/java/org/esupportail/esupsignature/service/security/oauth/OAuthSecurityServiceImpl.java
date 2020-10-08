package org.esupportail.esupsignature.service.security.oauth;

import org.esupportail.esupsignature.service.security.SecurityService;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class OAuthSecurityServiceImpl implements SecurityService {
	
	@Resource
	private OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler;
	
	@Resource
	private RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;

	@Resource
	private ClientRegistrationRepository clientRegistrationRepository;

	@Override
	public String getTitle() {
		return "France Connect";
	}

	@Override
	public String getLoginUrl() {
		return "/login/oauth2entry";
	}

	@Override
	public String getLogoutUrl() {
		return "";
	}

	@Override
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/google");
	}

	@Override
	public OAuth2LoginAuthenticationFilter getAuthenticationProcessingFilter() {
		OAuth2LoginAuthenticationFilter auth2LoginAuthenticationFilter = new OAuth2LoginAuthenticationFilter(clientRegistrationRepository, authorizedClientService(clientRegistrationRepository), OAuth2LoginAuthenticationFilter.DEFAULT_FILTER_PROCESSES_URI);
		auth2LoginAuthenticationFilter.setAuthenticationSuccessHandler(oAuthAuthenticationSuccessHandler);
		auth2LoginAuthenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
		auth2LoginAuthenticationFilter.setAuthorizationRequestRepository(authorizationRequestRepository());
		auth2LoginAuthenticationFilter.setAuthenticationManager(oAuthAuthenticationManager());
		RequestMatcher authenticationNullMatcher = request -> SecurityContextHolder.getContext().getAuthentication() == null;
		auth2LoginAuthenticationFilter.setRequiresAuthenticationRequestMatcher(new AndRequestMatcher(new AntPathRequestMatcher("/login/oauth2/code/google"), authenticationNullMatcher));

		return auth2LoginAuthenticationFilter;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return null;
	}
	/* A GARDER POUR MULTIPLE AUTH OU FRANCE CONNECT
	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
        String clientId = "295837101524-b9kj77m2kp30ahr01kk5abaprr9r3h12.apps.googleusercontent.com";
		String clientSecret = "OPeG_a0fifx1r5qcN5RONL_o";
    	ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
		        .clientId(clientId)
		        .clientSecret(clientSecret)
		        .scope("profile", "email")
		        .redirectUriTemplate("http://dsi-7.univ-rouen.fr:8080/login/oauth2/code/google")
		        .build();

        return new InMemoryClientRegistrationRepository(Arrays.asList(registration));
    }
    */

	public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
		HttpSessionOAuth2AuthorizationRequestRepository repository = new HttpSessionOAuth2AuthorizationRequestRepository();
		return repository; 
	}

    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
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
