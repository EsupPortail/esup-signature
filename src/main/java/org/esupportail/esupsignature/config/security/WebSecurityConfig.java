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
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

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

//	@Bean
//	@Order(1)
//	@ConditionalOnProperty({"spring.ldap.base", "security.cas.service"})
//	public CasSecurityServiceImpl casSecurityServiceImpl() {
//		if(ldapContextSource!= null && ldapContextSource.getUserDn() != null) {
//			CasSecurityServiceImpl casSecurityService = new CasSecurityServiceImpl(webSecurityProperties, spelGroupService(), ldapGroupService, casProperties, ldapProperties, );
//			securityServices.add(casSecurityService);
//			return casSecurityService;
//		} else {
//			logger.error("cas config found without needed ldap config, cas security will be disabled");
//			return null;
//		}
//	}
//
//	@Bean
//	@Order(2)
//	@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
//	public ShibSecurityServiceImpl shibSecurityServiceImpl() {
//		ShibSecurityServiceImpl shibSecurityService = new ShibSecurityServiceImpl();
//		securityServices.add(shibSecurityService);
//		return shibSecurityService;
//	}
//
//	@Bean
//	@Order(3)
//	@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.proconnect.client-id")
//	public ProConnectSecurityServiceImpl proConnectSecurityService() {
//		ProConnectSecurityServiceImpl oAuthSecurityService = new ProConnectSecurityServiceImpl();
//		securityServices.add(oAuthSecurityService);
//		return oAuthSecurityService;
//	}
//
//	@Bean
//	@Order(4)
//	@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.franceconnect.client-id")
//	public FranceConnectSecurityServiceImpl franceConnectSecurityService() {
//		FranceConnectSecurityServiceImpl oAuthSecurityService = new FranceConnectSecurityServiceImpl();
//		securityServices.add(oAuthSecurityService);
//		return oAuthSecurityService;
//	}

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
		http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(new IndexEntryPoint("/"), antMatcher("/")));
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
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher(securityService.getLoginUrl())).authenticated());
			http.exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(securityService.getAuthenticationEntryPoint(), antMatcher(securityService.getLoginUrl())));
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
		http.logout(logout -> logout.invalidateHttpSession(true)
						.logoutRequestMatcher(
								antMatcher("/logout")
						).logoutSuccessUrl("/logged-out"));
		http.logout(logout -> logout.addLogoutHandler(logoutHandler)
				.logoutSuccessUrl("/").permitAll());
		http.csrf(csrf -> csrf.ignoringRequestMatchers(antMatcher("/resources/**"))
				.ignoringRequestMatchers(antMatcher("/webjars/**"))
				.ignoringRequestMatchers(antMatcher("/ws/**"))
				.ignoringRequestMatchers(antMatcher("/nexu-sign/**"))
				.ignoringRequestMatchers(antMatcher("/log/**"))
				.ignoringRequestMatchers(antMatcher("/actuator/**"))
				.ignoringRequestMatchers(antMatcher("/h2-console/**")));
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		setAuthorizeRequests(http);
		return http.build();
	}

	@Bean
	public ValidatingOAuth2UserService validatingOAuth2UserService() {
		return new ValidatingOAuth2UserService(securityServices.stream().filter(s -> s instanceof OidcOtpSecurityService).map(s -> (OidcOtpSecurityService)s).toList(), clientRegistrationRepository);
	}

	@Bean
	@Conditional(ClientsConfiguredCondition.class)
	public CustomAuthorizationRequestResolver customAuthorizationRequestResolver() {
		return new CustomAuthorizationRequestResolver(clientRegistrationRepository, securityServices.stream().filter(s -> s instanceof OidcOtpSecurityService).map(s -> (OidcOtpSecurityService)s).toList());
	}

	//	@Bean
//	public APIKeyFilter apiKeyFilter() {
//		APIKeyFilter filter = new APIKeyFilter();
//		filter.setAuthenticationManager(authentication -> {
//			if(authentication.getPrincipal() == null) {
//				throw new BadCredentialsException("Access Denied.");
//			}
//			String apiKey = (String) authentication.getPrincipal();
//			if (authentication.getPrincipal() != null && this.apiKey.equals(apiKey)) {
//				Collection<SimpleGrantedAuthority> oldAuthorities = (Collection<SimpleGrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
//				SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_WS");
//				List<SimpleGrantedAuthority> updatedAuthorities = new ArrayList<>();
//				updatedAuthorities.add(authority);
//				updatedAuthorities.addAll(oldAuthorities);
//				SecurityContextHolder.getContext().setAuthentication(
//						new UsernamePasswordAuthenticationToken(
//								SecurityContextHolder.getContext().getAuthentication().getPrincipal(),
//								SecurityContextHolder.getContext().getAuthentication().getCredentials(),
//								updatedAuthorities));
//				return SecurityContextHolder.getContext().getAuthentication();
//			} else {
//				throw new BadCredentialsException("Access Denied.");
//			}
//		});
//		return filter;
//	}

	private void setAuthorizeRequests(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/logged-out")).permitAll());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/ws/workflows/**/datas/csv"))
				.access(new WebExpressionAuthorizationManager("hasIpAddress('" + webSecurityProperties.getCsvAccessAuthorizeMask() + "')")));
		setIpsAutorizations(http, webSecurityProperties.getWsAccessAuthorizeIps());
		setIpsAutorizations(http, webSecurityProperties.getActuatorsAccessAuthorizeIps());
		http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
				.requestMatchers(antMatcher("/api-docs/**")).hasAnyRole("ADMIN")
				.requestMatchers(antMatcher("/swagger-ui/**")).hasAnyRole("ADMIN")
				.requestMatchers(antMatcher("/swagger-ui.html")).hasAnyRole("ADMIN")
				.requestMatchers(antMatcher("/admin/**")).hasAnyRole("ADMIN")
				.requestMatchers(antMatcher("/manager/**")).hasAnyRole("MANAGER")
				.requestMatchers(antMatcher("/user/**")).hasAnyRole("USER")
				.requestMatchers(antMatcher("/nexu-sign/**")).hasAnyRole("USER", "OTP")
				.requestMatchers(antMatcher("/otp/**")).hasAnyRole("OTP")
				.requestMatchers(antMatcher("/ws-secure/**")).hasAnyRole("USER", "OTP")
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
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/ws/**"))
						.access(new WebExpressionAuthorizationManager(finalHasIpAddresses)));
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/actuator/**"))
						.access(new WebExpressionAuthorizationManager(finalHasIpAddresses)));
			} else {
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/ws/**")).denyAll());
				http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/actuator/**")).denyAll());
			}
//			http.authorizeRequests().requestMatchers("/ws/**").access("hasRole('WS')").and().addFilter(apiKeyFilter());
		} else {
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/ws/**")).denyAll());
			http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests.requestMatchers(antMatcher("/actuator/**")).denyAll());
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
