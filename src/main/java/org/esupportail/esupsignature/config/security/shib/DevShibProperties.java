package org.esupportail.esupsignature.config.security.shib;

public class DevShibProperties {

	private Boolean enable = false;
	
	private String eppn = "testju@esup-portail.org";
	
	private String mail = "justin.test@esup-portail.org";
	
	private String sn = "Test";
	
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
