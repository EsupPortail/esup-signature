package org.esupportail.esupsignature.config;

import javax.annotation.Resource;

import org.esupportail.esupsignature.security.CasWebSecurityConfigurerAdapter;
import org.esupportail.esupsignature.security.DatabaseUserDetailsService;
import org.esupportail.esupsignature.security.ShibWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
	
	@Value("${security.filter}")
	private String filter;

	@Resource
	private DatabaseUserDetailsService databaseUserDetailsService;
	
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
	public SwitchUserFilter switchUserProcessingFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		switchUserFilter.setUsernameParameter("j_username");
		switchUserFilter.setUserDetailsService(databaseUserDetailsService);
		switchUserFilter.setSwitchUserUrl("/j_spring_security_switch_user");
		switchUserFilter.setExitUserUrl("/j_spring_security_logout");
		switchUserFilter.setTargetUrl("/");
		return switchUserFilter;
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
