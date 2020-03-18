package org.esupportail.esupsignature.service.security;

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
		.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/manager/", "/manager/**").access("hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
		.antMatchers("/user/", "/user/**").authenticated()
		.antMatchers("/webjars/**").permitAll();

//		http.authorizeRequests()
//		.antMatchers("/", "/**").authenticated()
//		.antMatchers("/webjars/**").permitAll();
	}

}
