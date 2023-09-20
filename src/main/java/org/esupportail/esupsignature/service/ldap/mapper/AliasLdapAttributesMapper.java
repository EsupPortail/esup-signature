package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

public class AliasLdapAttributesMapper implements AttributesMapper<AliasLdap> {

    @Override
    public AliasLdap mapFromAttributes(Attributes attributes) throws NamingException {
        AliasLdap alias = new AliasLdap();
        alias.setCn(getStringAttribute(attributes, "cn"));
        alias.setMail(getStringAttribute(attributes, "mail"));
        alias.setRfc822MailMember(getStringListAttribute(attributes, "rfc822MailMember"));
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

    private List<String> getStringListAttribute(Attributes attributes, String attributeName) throws NamingException {
        List<String> values = new ArrayList<>();
        Attribute attribute = attributes.get(attributeName);
        if (attribute != null) {
            NamingEnumeration<?> attributeValues = attribute.getAll();
            while (attributeValues.hasMore()) {
                String value = (String) attributeValues.next();
                values.add(value);
            }
        }
        return values;
    }
}