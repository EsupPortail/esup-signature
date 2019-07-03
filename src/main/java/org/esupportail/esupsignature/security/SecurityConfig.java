package org.esupportail.esupsignature.security;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

public interface SecurityConfig {
	String getName();
	String getLoginUrl();
	AuthenticationEntryPoint getAuthenticationEntryPoint();
	GenericFilterBean getAuthenticationProcessingFilter();

}
