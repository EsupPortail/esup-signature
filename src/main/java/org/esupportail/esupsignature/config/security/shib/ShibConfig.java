package org.esupportail.esupsignature.config.security.shib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esupportail.esupsignature.service.security.Group2UserRoleService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.SpelGroupService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.ShibAuthenticatedUserDetailsService;
import org.esupportail.esupsignature.service.security.shib.ShibAuthenticationSuccessHandler;
import org.esupportail.esupsignature.service.security.shib.ShibRequestHeaderAuthenticationFilter;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import javax.annotation.Resource;

@Configuration
@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
@EnableConfigurationProperties(ShibProperties.class)
public class ShibConfig {

	@Bean
	public ShibSecurityServiceImpl shibSecurityServiceImpl() {
		return new ShibSecurityServiceImpl();
	}
	
}
