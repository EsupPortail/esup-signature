package org.esupportail.esupsignature.config.security.shib;

import org.esupportail.esupsignature.service.security.DevSecurityFilter;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.shib", name = "principal-request-header")
@EnableConfigurationProperties(ShibProperties.class)
public class ShibConfig {

	@Bean
	public ShibSecurityServiceImpl shibSecurityServiceImpl() {
		return new ShibSecurityServiceImpl();
	}
	
	@Bean
	@ConditionalOnProperty(prefix = "security.shib.dev", name = "enable", havingValue = "true")
	public DevSecurityFilter devClientRequestFilter() {
		return new DevClientRequestFilter();
	}

}
