package org.esupportail.esupsignature.config.security.cas;

import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "security.cas", name = "service")
@EnableConfigurationProperties(CasProperties.class)
public class CasConfig {

	private CasProperties casProperties;

	public CasConfig(CasProperties casProperties) {
		this.casProperties = casProperties;
	}

	@Bean
	public CasSecurityServiceImpl CasSecurityServiceImpl() {
		return new CasSecurityServiceImpl(casProperties.getUrl(), casProperties.getService(), casProperties.getKey());
	}

}
