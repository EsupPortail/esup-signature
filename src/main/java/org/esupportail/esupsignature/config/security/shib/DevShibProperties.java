package org.esupportail.esupsignature.config.security.shib;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.shib.dev")
public class DevShibProperties {

	/**
	 * If true, each session will use user attributes defined here ; use it only for development/test purposes !
	 */
	private Boolean enable = false;
	
	/**
	 * Eppn (user id) used for each sessions.
	 */
	private String eppn = "testju@esup-portail.org";
	
	/**
	 * Email address used for each sessions.
	 */
	private String mail = "justin.test@esup-portail.org";
	
	
	/**
	 * Family name used for each sessions.
	 */
	private String sn = "Test";
	
	/**
	 * First name used for each sessions.
	 */
	private String givenName = "Justin";

	public Boolean getEnable() {
		return enable;
	}

	public void setEnable(Boolean enable) {
		this.enable = enable;
	}

	public String getEppn() {
		return eppn;
	}

	public void setEppn(String eppn) {
		this.eppn = eppn;
	}

	public String getMail() {
		return mail;
	}
	
	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getSn() {
		return sn;
	}

	public void setSn(String sn) {
		this.sn = sn;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	
}
