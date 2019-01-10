package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.security.cas.CasWebSecurityConfigurerAdapter;
import org.esupportail.esupsignature.security.shib.ShibWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
	
	@Value("${security.filter}")
	private String filter;
	
	@Bean
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		switch (filter) {
		case "SHIB" :
			return new ShibWebSecurityConfigurerAdapter();
		case "CAS" :
			return new CasWebSecurityConfigurerAdapter();
		default :
			return null;
		}
	}
	
	@Bean
	public SessionRegistryImpl sessionRegistry() {
		return new SessionRegistryImpl();
	}
	
	@Bean
	public ConcurrentSessionFilter concurrencyFilter() {
		ConcurrentSessionFilter concurrentSessionFilter = new ConcurrentSessionFilter(sessionRegistry());
		return concurrentSessionFilter;
	}
	
	@Bean
	public RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy() {
		RegisterSessionAuthenticationStrategy authenticationStrategy = new RegisterSessionAuthenticationStrategy(sessionRegistry());
		return authenticationStrategy;
	}
}
