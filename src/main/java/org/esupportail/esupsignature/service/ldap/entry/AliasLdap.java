package org.esupportail.esupsignature.service.ldap.entry;

import org.springframework.ldap.odm.annotations.Attribute;

import javax.naming.Name;

public final class AliasLdap {

	private Name dn;
	private @Attribute(name = "cn") String cn;
	private @Attribute(name = "mail") String mail;
	private @Attribute(name = "rfc822MailMember") String rfc822MailMember;

	public String getCn() {
		return cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getRfc822MailMember() {
		return rfc822MailMember;
	}

	public void setRfc822MailMember(String rfc822MailMember) {
		this.rfc822MailMember = rfc822MailMember;
	}
}
	
	
