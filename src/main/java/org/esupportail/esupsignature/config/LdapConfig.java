package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;

@Configuration
@ConditionalOnClass(ContextSource.class)
public class LdapConfig {

	@Bean
	public PersonLdapDao personLdapDao() {
		PersonLdapDao personLdapDao = new PersonLdapDao();
		return personLdapDao;
	}
	
}
