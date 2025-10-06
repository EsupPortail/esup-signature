package org.esupportail.esupsignature.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.cas.CasJwtDecoder;
import org.esupportail.esupsignature.config.security.cas.CasProperties;
import org.esupportail.esupsignature.config.security.jwt.CustomJwtAuthenticationConverter;
import org.esupportail.esupsignature.config.security.jwt.MdcUsernameFilter;
import org.esupportail.esupsignature.config.security.otp.OtpAuthenticationProvider;
import org.esupportail.esupsignature.config.security.shib.DevShibProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.service.security.IndexEntryPoint;
import org.esupportail.esupsignature.service.security.LogoutHandlerImpl;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.CustomAuthorizationRequestResolver;
import org.esupportail.esupsignature.service.security.oauth.OAuth2FailureHandler;
import org.esupportail.esupsignature.service.security.oauth.OAuthAuthenticationSuccessHandler;
import org.esupportail.esupsignature.service.security.oauth.ValidatingOAuth2UserService;
import org.esupportail.esupsignature.service.security.oauth.franceconnect.FranceConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.proconnect.ProConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.DevShibRequestFilter;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.su.SuAuthenticationSuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.ExceptionMappingAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.switchuser.SwitchUserFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity(debug = false)
@EnableMethodSecurity(
		securedEnabled = true,
		jsr250Enabled = true)
@EnableConfigurationProperties({WebSecurityProperties.class, ShibProperties.class, CasProperties.class, DevShibProperties.class})
public class WebSecurityConfig {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

	private final String apiKey = "SomeKey1234567890";

	private final GlobalProperties globalProperties;
	private final OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler;
	private final WebSecurityProperties webSecurityProperties;
	private final ClientRegistrationRepository clientRegistrationRepository;
	private final List<SecurityService> securityServices;
	private final RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SessionRegistryImpl sessionRegistry;
	private final LogoutHandlerImpl logoutHandler;
	private final CasJwtDecoder casJwtDecoder;
	private DevShibRequestFilter devShibRequestFilter;

	public WebSecurityConfig(GlobalProperties globalProperties, OAuthAuthenticationSuccessHandler oAuthAuthenticationSuccessHandler, WebSecurityProperties webSecurityProperties, @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository, List<SecurityService> securityServices, RegisterSessionAuthenticationStrategy sessionAuthenticationStrategy, SessionRegistryImpl sessionRegistry, LogoutHandlerImpl logoutHandler, @Autowired(required = false) CasJwtDecoder casJwtDecoder) {
        this.globalProperties = globalProperties;
        this.oAuthAuthenticationSuccessHandler = oAuthAuthenticationSuccessHandler;
        this.webSecurityProperties = webSecurityProperties;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.securityServices = securityServices;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.sessionRegistry = sessionRegistry;
        this.logoutHandler = logoutHandler;
        this.casJwtDecoder = casJwtDecoder;
    }

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.client.provider.cas.issuer-uri")
	public SecurityFilterChain wsJwtSecurityFilter(HttpSecurity http) throws Exception {
		http.securityMatcher("/ws-jwt/**");
		if (StringUtils.hasText(issuerUri)) {
			http.oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver()).jwt(jwt -> jwt.decoder(casJwtDecoder)
					.jwtAuthenticationConverter(new CustomJwtAuthenticationConverter())));
			http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
		} else {
			http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
		}
		http.cors(AbstractHttpConfigurer::disable);
		http.addFilterAfter(new MdcUsernameFilter(), AuthorizationFilter.class);
		return http.build();
	}

	@Value("${spring.security.oauth2.client.provider.cas.issuer-uri:}")
	private String issuerUri;

	@Bean
	public BearerTokenResolver bearerTokenResolver() {
		return new BearerTokenResolver() {
			private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

			@Override
			public String resolve(HttpServletRequest request) {
				String token = defaultResolver.resolve(request);
				if (token != null) return token;
				if (request.getCookies() != null) {
					for (Cookie cookie : request.getCookies()) {
						if ("jwt".equalsIgnoreCase(cookie.getName())) {
							return cookie.getValue();
						}
					}
				}
				return null;
			}
		};
	}

	@Bean
	@Order(4)
	@ConditionalOnProperty(prefix = "security.shib.dev", name = "enable", havingValue = "true")
	public DevShibRequestFilter devClientRequestFilter() {
		devShibRequestFilter = new DevShibRequestFilter();
		return devShibRequestFilter;
	}

	@Bean
	public HttpSessionEventPublisher sessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.sessionManagement(sessionManagement -> sessionManagement.sessionAuthenticationStrategy(sessionAuthenticationStrategy).maximumSessions(5).sessionRegistry(sessionRegistry));
		if(devShibRequestFilter != null) {
			http.addFilterBefore(devShibRequestFilter, OAuth2AuthorizationRequestRedirectFilter.class);
		}
		http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(new IndexEntryPoint("/"), PathPatternRequestMatcher.withDefaults().matcher("/")));
		AccessDeniedHandlerImpl accessDeniedHandlerImpl = new AccessDeniedHandlerImpl();
		accessDeniedHandlerImpl.setErrorPage("/denied");
		http.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedHandler(accessDeniedHandlerImpl));
		if(securityServices.stream().anyMatch(s -> s instanceof OidcOtpSecurityService)) {
			http.oauth2Login(oauth2Login -> oauth2Login.loginPage("/")
				.successHandler(oAuthAuthenticationSuccessHandler)
				.failureHandler(new OAuth2FailureHandler())
				.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.oidcUserService(validatingOAuth2UserService()))
				.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint.authorizationRequestResolver(customAuthorizationRequestResolver())));
		}
		for(SecurityService securityService : securityServices) {
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(securityService.getLoginUrl()).authenticated());
			http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), PathPatternRequestMatcher.withDefaults().matcher(securityService.getLoginUrl())));
			if(securityService.getAuthenticationProcessingFilter() != null) {
			http.addFilterBefore(securityService.getAuthenticationProcessingFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
			}
		}
		if(globalProperties.getEnableSu()) {
			for (SecurityService securityService : securityServices) {
				if (securityService instanceof CasSecurityServiceImpl) {
					switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
				} else if (securityService instanceof ShibSecurityServiceImpl) {
					switchUserFilter().setUserDetailsService(securityService.getUserDetailsService());
					break;
				}
			}
		}
		http.logout(logout -> logout.addLogoutHandler(logoutHandler).invalidateHttpSession(true)
						.logoutUrl("/logout"
						).logoutSuccessUrl("/logged-out"));
		http.csrf(csrf -> csrf.ignoringRequestMatchers(("/resources/**"))
				.ignoringRequestMatchers("/webjars/**")
				.ignoringRequestMatchers("/ws/**")
				.ignoringRequestMatchers("/nexu-sign/**")
				.ignoringRequestMatchers("/log/**")
				.ignoringRequestMatchers("/actuator/**")
				.ignoringRequestMatchers("/h2-console/**"));
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		setAuthorizeRequests(http);
		return http.build();
	}

	@Bean
	public ValidatingOAuth2UserService validatingOAuth2UserService() {
		return new ValidatingOAuth2UserService(securityServices.stream().filter(s -> s instanceof OidcOtpSecurityService).map(s -> (OidcOtpSecurityService)s).toList(), clientRegistrationRepository);
	}

	@Bean
	@ConditionalOnOAuth2ClientRegistrationProperties
	public CustomAuthorizationRequestResolver customAuthorizationRequestResolver() {
		return new CustomAuthorizationRequestResolver(clientRegistrationRepository, securityServices.stream().filter(s -> s instanceof OidcOtpSecurityService).map(s -> (OidcOtpSecurityService)s).toList());
	}

	private void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/logged-out")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/ws/workflows/*/datas/csv"))
				.access(new WebExpressionAuthorizationManager("hasIpAddress('" + webSecurityProperties.getCsvAccessAuthorizeMask() + "')")));
		setIpsAutorizations(http, webSecurityProperties.getWsAccessAuthorizeIps());
		setIpsAutorizations(http, webSecurityProperties.getActuatorsAccessAuthorizeIps());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
				.requestMatchers("/api-docs/**").hasAnyRole("ADMIN")
				.requestMatchers("/swagger-ui/**").hasAnyRole("ADMIN")
				.requestMatchers("/swagger-ui.html").hasAnyRole("ADMIN")
				.requestMatchers("/admin/**").hasAnyRole("ADMIN")
				.requestMatchers("/manager/**").hasAnyRole("MANAGER")
				.requestMatchers("/user/**").hasAnyRole("USER")
				.requestMatchers("/nexu-sign/**").hasAnyRole("USER", "OTP")
				.requestMatchers("/otp/**").hasAnyRole("OTP")
				.requestMatchers("/ws-secure/**").hasAnyRole("USER", "OTP")
				.anyRequest().permitAll());
	}

	private void setIpsAutorizations(HttpSecurity http, String[] authorizeIps) throws Exception {
		StringBuilder hasIpAddresses = new StringBuilder();
		int nbIps = 0;
		if(authorizeIps != null && authorizeIps.length > 0) {
			for (String ip : authorizeIps) {
				nbIps++;
				hasIpAddresses.append("hasIpAddress('").append(ip).append("')");
				if(nbIps < authorizeIps.length) {
					hasIpAddresses.append(" or ");
				}
			}
			String finalHasIpAddresses = hasIpAddresses.toString();
			if(StringUtils.hasText(finalHasIpAddresses)) {
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/ws/**"))
						.access(new WebExpressionAuthorizationManager(finalHasIpAddresses)));
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/actuator/**"))
						.access(new WebExpressionAuthorizationManager(finalHasIpAddresses)));
			} else {
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/ws/**")).denyAll());
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/actuator/**")).denyAll());
			}
//			http.authorizeRequests().requestMatchers("/ws/**").access("hasRole('WS')").and().addFilter(apiKeyFilter());
		} else {
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/ws/**")).denyAll());
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(("/actuator/**")).denyAll());
		}
	}

	@Bean
	@ConditionalOnProperty(value="global.enable-su",havingValue = "true")
	public SwitchUserFilter switchUserFilter() {
		SwitchUserFilter switchUserFilter = new SwitchUserFilter();
		switchUserFilter.setUserDetailsService(new InMemoryUserDetailsManager());
		switchUserFilter.setSwitchUserUrl("/admin/su-login");
		switchUserFilter.setExitUserUrl("/su-logout");
		switchUserFilter.setSuccessHandler(new SuAuthenticationSuccessHandler());
		switchUserFilter.setFailureHandler(new ExceptionMappingAuthenticationFailureHandler());
		return switchUserFilter;
	}

	@Bean
	public AuthenticationManager authenticationManagerBean() {
		return new ProviderManager(List.of(new OtpAuthenticationProvider()));
	}

	@Bean
	public List<ExternalAuth> getExternalAuths(List<OidcOtpSecurityService> securityServices, SmsProperties smsProperties) {
		List<ExternalAuth> externalAuths = new ArrayList<>(List.of(ExternalAuth.values()));
		if(globalProperties.getSmsRequired()) externalAuths.remove(ExternalAuth.open);
		if(securityServices.stream().noneMatch(s -> s instanceof ProConnectSecurityServiceImpl)) externalAuths.remove(ExternalAuth.proconnect);
		if(securityServices.stream().noneMatch(s -> s instanceof FranceConnectSecurityServiceImpl)) externalAuths.remove(ExternalAuth.franceconnect);
		if(BooleanUtils.isFalse(smsProperties.getEnableSms())) externalAuths.remove(ExternalAuth.sms);
		return externalAuths;
	}

}
