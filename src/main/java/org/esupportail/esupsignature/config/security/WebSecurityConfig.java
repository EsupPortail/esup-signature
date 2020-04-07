package org.esupportail.esupsignature.config.security;

import org.esupportail.esupsignature.service.security.AuthorizeRequestsHelper;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.LogoutHandlerImpl;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.OAuthSecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ldap.LdapProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.annotation.Resource;
import javax.naming.ldap.LdapContext;
import java.util.List;

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

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		AuthorizeRequestsHelper.setAuthorizeRequests(http, webSecurityProperties.getWsAccessAuthorizeIps());
		for(SecurityService securityService : securityServices) {
			http.antMatcher("/**").authorizeRequests().antMatchers(securityService.getLoginUrl()).authenticated();
			http.exceptionHandling().defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), new AntPathRequestMatcher(securityService.getLoginUrl()));
			http.addFilterBefore(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			if(securityService.getClass().equals(OAuthSecurityServiceImpl.class)) {
				http.oauth2Client();
			}
		}
		http.logout()
				.logoutRequestMatcher(
						new AntPathRequestMatcher("/logout")
				)
				.addLogoutHandler(logoutHandler())
				.logoutSuccessUrl("/login").permitAll();
		http.sessionManagement().sessionAuthenticationStrategy(sessionAuthenticationStrategy()).maximumSessions(5).sessionRegistry(sessionRegistry());
		http.csrf().ignoringAntMatchers("/ws/**");
		http.csrf().ignoringAntMatchers("/user/nexu-sign/**");
		http.headers().frameOptions().sameOrigin();
		http.headers().disable();
	}

	@Bean
	public LogoutHandlerImpl logoutHandler() {
		return new LogoutHandlerImpl();
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

	@Bean
	@ConditionalOnProperty(value = "spring.ldap.base")
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		for(SecurityService securityService : securityServices) {
			if(securityService instanceof CasSecurityServiceImpl) {
				switchUserFilter.setUserDetailsService(securityService.getUserDetailsService());
			}
		}
		switchUserFilter.setSwitchUserUrl("/admin/su-login");
		//switchUserFilter.setSwitchFailureUrl("/error");
		switchUserFilter.setExitUserUrl("/su-logout");
		switchUserFilter.setTargetUrl("/");
		return switchUserFilter;
	}
}