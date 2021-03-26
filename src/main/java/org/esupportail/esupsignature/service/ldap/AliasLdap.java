package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(objectClasses = {"nisMailAlias"}, base = "ou=aliases-list")
public final class AliasLdap {

    @Id
    private Name dn;
    private @Attribute(name = "mail") String mailAlias;
    private @Attribute(name = "cn") String cn;

    public String getMailAlias() {
        return mailAlias;
    }

    public void setMailAlias(String mailAlias) {
        this.mailAlias = mailAlias;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }
}
