package org.esupportail.esupsignature.security.oauth;

import org.esupportail.esupsignature.security.SecurityConfig;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class OAuthSecurityConfigImpl implements SecurityConfig {
	
	/*

	public ClientRegistrationRepository clientRegistrationRepository() {
        String clientId = "295837101524-b9kj77m2kp30ahr01kk5abaprr9r3h12.apps.googleusercontent.com";
		String clientSecret = "OPeG_a0fifx1r5qcN5RONL_o";
    	ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder("google")
		        .clientId(clientId)
		        .clientSecret(clientSecret)
		        .scope("profile", "email")
		        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
		        .build();

        return new InMemoryClientRegistrationRepository(Arrays.asList(registration));
    }
*/
	
	public String getLoginUrl() {
		return "/login-oauth";
	}
	
	public LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint("/login");
	}
	
	public OAuth2LoginAuthenticationFilter getAuthenticationProcessingFilter() {
		return null;
	}

}
