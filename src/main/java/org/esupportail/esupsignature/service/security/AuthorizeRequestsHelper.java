package org.esupportail.esupsignature.service.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;

public class AuthorizeRequestsHelper {

	public static void setAuthorizeRequests(HttpSecurity http, String[] wsAccessAuthorizeIps) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		String hasIpAddresses = "";
		int nbIps = 0;
		for (String ip : wsAccessAuthorizeIps) {
			nbIps++;
			hasIpAddresses += "hasIpAddress('"+ ip +"')";
			if(nbIps < wsAccessAuthorizeIps.length) {
				hasIpAddresses += " or ";
			}
		}
		http.authorizeRequests().antMatchers("/ws/**").access(hasIpAddresses);
		http.authorizeRequests()
		.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
		.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_MANAGER') or hasRole('ROLE_ADMIN')")
		.antMatchers("/user/", "/user/**").authenticated()
		.antMatchers("/webjars/**").permitAll();

//		http.authorizeRequests()
//		.antMatchers("/", "/**").authenticated()
//		.antMatchers("/webjars/**").permitAll();
	}

}
