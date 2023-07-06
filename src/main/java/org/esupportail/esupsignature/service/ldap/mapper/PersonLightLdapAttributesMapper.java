package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

public class PersonLightLdapAttributesMapper implements AttributesMapper<PersonLightLdap> {
    @Override
    public PersonLightLdap mapFromAttributes(Attributes attributes) throws NamingException {
        PersonLightLdap person = new PersonLightLdap();
        person.setUid(getStringAttribute(attributes,"uid"));
        person.setCn(getStringAttribute(attributes,"cn"));
        person.setSn(getStringAttribute(attributes,"sn"));
        person.setGivenName(getStringAttribute(attributes, "givenName"));
        person.setDisplayName(getStringAttribute(attributes,"displayName"));
        person.setMail(getStringAttribute(attributes, "mail"));
        person.setEduPersonPrincipalName(getStringAttribute(attributes,"eduPersonPrincipalName"));
        return person;
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