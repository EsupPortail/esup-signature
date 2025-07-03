package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

public interface SecurityService {
	String getTitle();
	String getCode();
	String getDescription();
	String getLoginUrl();
	String getLoggedOutUrl();
	AuthenticationEntryPoint getAuthenticationEntryPoint();
	GenericFilterBean getAuthenticationProcessingFilter();
	UserDetailsService getUserDetailsService();

}
