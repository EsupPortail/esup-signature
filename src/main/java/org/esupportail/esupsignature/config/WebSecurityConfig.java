package org.esupportail.esupsignature.config;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.esupportail.esupsignature.security.AuthorizeRequestsHelper;
import org.esupportail.esupsignature.security.SecurityConfig;
import org.esupportail.esupsignature.security.cas.CasSecurityConfigImpl;
import org.esupportail.esupsignature.security.oauth.OAuthSecurityConfigImpl;
import org.esupportail.esupsignature.security.shib.ShibSecurityConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity(debug = false)
@ConfigurationProperties(prefix="security")
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        jsr250Enabled = true,
        prePostEnabled = true
)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	boolean enableCas;
	boolean enableShib;
	boolean enableOAuth;
	String[] nfcWsAccessAuthorizeIps;
	
	public void setEnableCas(boolean enableCas) {
		this.enableCas = enableCas;
	}

	public void setEnableShib(boolean enableShib) {
		this.enableShib = enableShib;
	}

	public void setEnableOAuth(boolean enableOAuth) {
		this.enableOAuth = enableOAuth;
	}
	
	public void setNfcWsAccessAuthorizeIps(String[] nfcWsAccessAuthorizeIps) {
		this.nfcWsAccessAuthorizeIps = nfcWsAccessAuthorizeIps;
	}

	private List<SecurityConfig> securityConfigs = new ArrayList<SecurityConfig>();
	
	/*
	@Bean
	@Override
	public AuthenticationManager authenticationManagerBean() throws Exception {
		return super.authenticationManagerBean();
	}
	*/
	@Autowired 
	private ApplicationContext applicationContext; 
	
	@PostConstruct
	public void init() {
		AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
	    if(enableCas) {
			securityConfigs.add(beanFactory.createBean(CasSecurityConfigImpl.class));
	    }
	    if(enableShib) {
	    	securityConfigs.add(beanFactory.createBean(ShibSecurityConfigImpl.class));
	    }
	    if(enableOAuth) {
	    	securityConfigs.add(beanFactory.createBean(OAuthSecurityConfigImpl.class));
	    }
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		AuthorizeRequestsHelper.setAuthorizeRequests(http, nfcWsAccessAuthorizeIps);
		for(SecurityConfig securityConfig : securityConfigs) {
			http.antMatcher("/**").authorizeRequests().antMatchers(securityConfig.getLoginUrl()).authenticated();
			http.exceptionHandling().defaultAuthenticationEntryPointFor(securityConfig.getAuthenticationEntryPoint(), new AntPathRequestMatcher(securityConfig.getLoginUrl()));
			if(securityConfig.getAuthenticationProcessingFilter() != null) {
				http.addFilterBefore(securityConfig.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			}
		}
		
		if(enableOAuth) {
			http.oauth2Client();
		}

		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy());
		http.csrf().disable();
		http.headers().frameOptions().sameOrigin();
		http.cors().disable();
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
		RegisterSessionAuthenticationStrategy authenticationStrategy = new RegisterSessionAuthenticationStrategy(
				sessionRegistry());
		return authenticationStrategy;
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
	    return new HttpSessionEventPublisher();
	}
	
}