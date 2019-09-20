package org.esupportail.esupsignature.config.security;

import java.util.List;

import org.esupportail.esupsignature.security.AuthorizeRequestsHelper;
import org.esupportail.esupsignature.security.SecurityService;
import org.esupportail.esupsignature.security.oauth.OAuthSecurityServiceImpl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.Resource;

@Configuration
@EnableWebSecurity(debug = false)
@EnableConfigurationProperties(WebSecurityProperties.class)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	private WebSecurityProperties webSecurityProperties;

	public WebSecurityConfig(WebSecurityProperties webSecurityProperties) {
		this.webSecurityProperties = webSecurityProperties;
	}

	@Resource
	List<SecurityService> securityServices;

	/*
	@Autowired
    private AutowireCapableBeanFactory beanFactory;
	
	@Bean
	public List<SecurityConfig> securityConfigs() {
		List<SecurityConfig> securityConfigs = new ArrayList<>();
		if(webSecurityProperties.isEnableCas()) {
	    	securityConfigs.add(beanFactory.createBean(CasConfig.class));
	    }
	    if(webSecurityProperties.isEnableShib()) {
	    	securityConfigs.add(beanFactory.createBean(ShibSecurityConfigImpl.class));
	    }
	    if(webSecurityProperties.isEnableOAuth()) {
	    	securityConfigs.add(beanFactory.createBean(OAuthSecurityConfigImpl.class));
	    }
	    return securityConfigs;
	}
*/
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		AuthorizeRequestsHelper.setAuthorizeRequests(http, webSecurityProperties.getNfcWsAccessAuthorizeIps());
		for(SecurityService securityService : securityServices) {
			http.antMatcher("/**").authorizeRequests().antMatchers(securityService.getLoginUrl()).authenticated();
			http.exceptionHandling().defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), new AntPathRequestMatcher(securityService.getLoginUrl()));
			http.addFilterBefore(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			if(securityService.getClass().equals(OAuthSecurityServiceImpl.class)) {
				http.oauth2Client();
			}
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

	
}