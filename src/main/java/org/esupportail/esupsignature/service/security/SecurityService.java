package org.esupportail.esupsignature.service.security;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.GenericFilterBean;

public interface SecurityService {
	String getName();
	String getLoginUrl();
	AuthenticationEntryPoint getAuthenticationEntryPoint();
	GenericFilterBean getAuthenticationProcessingFilter();

}
