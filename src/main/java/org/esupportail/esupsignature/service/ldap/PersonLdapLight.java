package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(objectClasses = {"top", "nisMailAlias", "person", "inetOrgPerson", "supannPerson"})
public final class PersonLdapLight {

	@Id
	private Name dn;
	private @Attribute(name = "uid") String uid;
	private @Attribute(name = "cn") String cn;
	private @Attribute(name = "sn") String sn;
	private @Attribute(name = "givenName") String givenName;
	private @Attribute(name = "displayName") String displayName;
	private @Attribute(name = "mail") String mail;
	private @Attribute(name = "eduPersonPrincipalName") String eduPersonPrincipalName;

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getCn() {
		return cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getEduPersonPrincipalName() {
		return eduPersonPrincipalName;
	}

	public void setEduPersonPrincipalName(String eduPersonPrincipalName) {
		this.eduPersonPrincipalName = eduPersonPrincipalName;
	}
}
	
	
