package org.esupportail.esupsignature.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.security.DatabaseUserDetailsService;
import org.esupportail.esupsignature.security.ShibAuthenticatedUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Resource
	private DatabaseUserDetailsService databaseUserDetailsService;

	@Autowired
	private ShibAuthenticatedUserDetailsService authUserDetailsService;
	
	@Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(preauthAuthProvider());
    }
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.logout().logoutSuccessUrl("/").permitAll();
		http.authorizeRequests()
				.antMatchers("/j_spring_security_switch_user").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/admin/", "/admin/**").access("hasRole('ROLE_ADMIN')")
				.antMatchers("/manager/", "/manager/**").access("hasRole('ROLE_ADMIN') or hasRole('ROLE_ADMIN')")
				.antMatchers("/nfc-ws/**").access("hasIpAddress('127.0.0.1') or hasIpAddress('Esup-NFC-Tag IP Address')")
				.antMatchers("/**").access("permitAll");
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling().accessDeniedHandler(accessDeniedHandlerImpl);
		http.exceptionHandling().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/"));
		http.addFilterBefore(shibFilter(), AbstractPreAuthenticatedProcessingFilter.class);
		http.addFilterBefore(switchUserProcessingFilter(), SwitchUserFilter.class);
		http.addFilterBefore(concurrencyFilter(), ConcurrentSessionFilter.class);
		http.sessionManagement().sessionAuthenticationStrategy(sas());
		http.csrf().disable();
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		List<AuthenticationProvider> authenticatedAuthenticationProviders = new ArrayList<AuthenticationProvider>();
		authenticatedAuthenticationProviders.add(preauthAuthProvider());
		AuthenticationManager authenticationManager = new ProviderManager(authenticatedAuthenticationProviders);
		return authenticationManager;
		
	}
	
	@Bean
	public RequestHeaderAuthenticationFilter shibFilter() throws Exception {
		RequestHeaderAuthenticationFilter authenticationFilter = new RequestHeaderAuthenticationFilter();
		authenticationFilter.setPrincipalRequestHeader("REMOTE_USER");
		authenticationFilter.setCredentialsRequestHeader("MEMBER");
		authenticationFilter.setAuthenticationManager(authenticationManager());
		authenticationFilter.setExceptionIfHeaderMissing(false);
		return authenticationFilter;
	}
	
	@Bean
	public PreAuthenticatedAuthenticationProvider preauthAuthProvider() {
		PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
		authenticationProvider.setPreAuthenticatedUserDetailsService(authUserDetailsService);
		return authenticationProvider;
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
	public SessionRegistryImpl sessionRegistry() {
		return new SessionRegistryImpl();
	}
	
	@Bean
	public ConcurrentSessionFilter concurrencyFilter() {
		ConcurrentSessionFilter concurrentSessionFilter = new ConcurrentSessionFilter(sessionRegistry());
		return concurrentSessionFilter;
	}
	
	@Bean
	public RegisterSessionAuthenticationStrategy sas() {
		RegisterSessionAuthenticationStrategy authenticationStrategy = new RegisterSessionAuthenticationStrategy(sessionRegistry());
		return authenticationStrategy;
	}
}
