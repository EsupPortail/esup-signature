package org.esupportail.esupsignature.service.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

public interface SecurityService {
	String getTitle();
	String getLoginUrl();
	String getLogoutUrl();
	AuthenticationEntryPoint getAuthenticationEntryPoint();
	GenericFilterBean getAuthenticationProcessingFilter();
	UserDetailsService getUserDetailsService();

}
