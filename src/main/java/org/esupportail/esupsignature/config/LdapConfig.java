package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {

	@Bean
	public LdapContextSource ldapContextSource() {
		LdapContextSource contextSource = new LdapContextSource();
		contextSource.setUrl("ldap://ldap.univ-rouen.fr");
		contextSource.setBase("dc=univ-rouen,dc=fr");
		contextSource.setUserDn("cn=consult,dc=univ-rouen,dc=fr");
		contextSource.setPassword("iletoqp");
		contextSource.afterPropertiesSet();
		return contextSource;
	}
	
	@Bean
	public LdapTemplate ldapTemplate() {
		return new LdapTemplate(ldapContextSource());
	}
	
	@Bean
	public PersonLdapDao personLdapDao() {
		PersonLdapDao personLdapDao = new PersonLdapDao();
		personLdapDao.setLdapTemplate(ldapTemplate());
		return personLdapDao;
	}
	
}
