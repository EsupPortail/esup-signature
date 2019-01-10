package org.esupportail.esupsignature.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

public class AuthorizeRequestsHelper {

	public static void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		http.authorizeRequests()
		.antMatchers("/login/impersonate").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/manager/", "/manager/**").access("hasRole('ROLE_ADMIN') or hasRole('ROLE_ADMIN')")
		.antMatchers("/nfc-ws/**").access("hasIpAddress('127.0.0.1') or hasIpAddress('Esup-NFC-Tag IP Address')")
		.antMatchers("/login/**").access("isAuthenticated()")
		.antMatchers("/**").access("permitAll");
	}

}
