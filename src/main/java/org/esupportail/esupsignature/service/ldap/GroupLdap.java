package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.List;

@Entry(objectClasses = {"groupOfNames"})
public final class GroupLdap {

	@Id
	private Name dn;
	private @Attribute(name = "cn") String cn;
	private @Attribute(name = "description") String description;
	private @Attribute(name = "owner") String owner;
	private @Attribute(name = "supannGroupeAdminDN") String supannGroupeAdminDN;
	private @Attribute(name = "member") List<String> member;

	public Name getDn() {
		return dn;
	}

	public void setDn(Name dn) {
		this.dn = dn;
	}

	public String getCn() {
		return cn;
	}

	public void setCn(String cn) {
		this.cn = cn;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getSupannGroupeAdminDN() {
		return supannGroupeAdminDN;
	}

	public void setSupannGroupeAdminDN(String supannGroupeAdminDN) {
		this.supannGroupeAdminDN = supannGroupeAdminDN;
	}

	public List<String> getMember() {
		return member;
	}

	public void setMember(List<String> member) {
		this.member = member;
	}
}
	
	
