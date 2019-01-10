package org.esupportail.esupsignature.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

public class AuthorizeRequestsHelper {

	public static void setAuthorizeRequests(HttpSecurity http, String[] nfcWsAccessAuthorizeIps) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		for (String ip : nfcWsAccessAuthorizeIps) {
			http.authorizeRequests().antMatchers("/nfc-ws/**").access("hasIpAddress('"+ ip +"')");			
		}
		http.authorizeRequests()
		.antMatchers("/login/impersonate").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/manager/", "/manager/**").access("hasRole('ROLE_ADMIN') or hasRole('ROLE_ADMIN')")
		.antMatchers("/login/**").access("isAuthenticated()")
		.antMatchers("/**").access("permitAll");
		
	}

}
