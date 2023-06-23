package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

public class PersonLightLdapAttributesMapper implements AttributesMapper<PersonLightLdap> {
    @Override
    public PersonLightLdap mapFromAttributes(Attributes attributes) throws NamingException {
        PersonLightLdap person = new PersonLightLdap();
        person.setUid(attributes.get("uid").get().toString());
        person.setCn(attributes.get("cn").get().toString());
        person.setSn(attributes.get("sn").get().toString());
        person.setGivenName(attributes.get("givenName").get().toString());
        person.setDisplayName(attributes.get("displayName").get().toString());
        person.setMail(attributes.get("mail").get().toString());
        person.setEduPersonPrincipalName(attributes.get("eduPersonPrincipalName").get().toString());
        return person;
    }
}