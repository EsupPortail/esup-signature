package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.List;

@Entry(objectClasses = {"nisMailAlias"}, base = "ou=aliases-list")
public final class AliasLdap {

    @Id
    private Name dn;
    private @Attribute(name = "mail") String mailAlias;
    private @Attribute(name = "rfc822MailMember")
    List<String> memberMails;
    private @Attribute(name = "cn") String cn;

    public String getMailAlias() {
        return mailAlias;
    }

    public void setMailAlias(String mailAlias) {
        this.mailAlias = mailAlias;
    }

    public List<String> getMemberMails() {
        return memberMails;
    }

    public void setMemberMails(List<String> memberMails) {
        this.memberMails = memberMails;
    }

    public String getCn() {
        return cn;
    }

    public void setCn(String cn) {
        this.cn = cn;
    }
}
