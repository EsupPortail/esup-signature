package org.esupportail.esupsignature.config.security.cas;

import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base", "security.cas.service"})
@EnableConfigurationProperties(CasProperties.class)
public class CasConfig {

	private static final Logger logger = LoggerFactory.getLogger(CasConfig.class);

	private LdapContextSource ldapContextSource;

	@Autowired(required = false)
	public void setLdapContextSource(LdapContextSource ldapContextSource) {
		this.ldapContextSource = ldapContextSource;
	}

	@Bean
	public CasSecurityServiceImpl CasSecurityServiceImpl() {
		if(ldapContextSource!= null && ldapContextSource.getUserDn() != null) {
			return new CasSecurityServiceImpl();
		} else {
			logger.error("cas config found without needed ldap config, cas security will be disabled");
			return null;
		}
	}

}
