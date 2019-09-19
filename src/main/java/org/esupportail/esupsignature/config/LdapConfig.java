package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
public class LdapConfig {

	@Bean
	public PersonLdapDao personLdapDao() {
		PersonLdapDao personLdapDao = new PersonLdapDao();
		return personLdapDao;
	}
	
}
