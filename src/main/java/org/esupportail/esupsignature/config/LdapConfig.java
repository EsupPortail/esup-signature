package org.esupportail.esupsignature.config;

import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="ldap")
public class LdapConfig {

	private boolean activeLdap;

	public boolean isActiveLdap() {
		return activeLdap;
	}

	public void setActiveLdap(boolean activeLdap) {
		this.activeLdap = activeLdap;
	}

	@Bean
	public PersonLdapDao personLdapDao() {
		if(activeLdap) {
			PersonLdapDao personLdapDao = new PersonLdapDao();
			return personLdapDao;
		} else {
			return null;
		}
	}
	
}
