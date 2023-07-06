package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

public class AliasLdapAttributesMapper implements AttributesMapper<AliasLdap> {

    @Override
    public AliasLdap mapFromAttributes(Attributes attributes) throws NamingException {
        AliasLdap alias = new AliasLdap();
        alias.setCn(getStringAttribute(attributes, "cn"));
        alias.setMail(getStringAttribute(attributes, "mail"));
        alias.setRfc822MailMember(getStringAttribute(attributes, "rfc822MailMember"));
        return alias;
    }

    private String getStringAttribute(Attributes attributes, String attributeName) throws NamingException {
        Attribute attribute = attributes.get(attributeName);
        if(attribute != null) {
            return (String) attribute.get();
        } else {
            return "";
        }
    }
}