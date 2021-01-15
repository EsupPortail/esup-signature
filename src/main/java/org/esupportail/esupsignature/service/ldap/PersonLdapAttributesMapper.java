package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;


public class PersonLdapAttributesMapper implements AttributesMapper<PersonLdap> {

    public PersonLdap mapFromAttributes(Attributes attrs) throws NamingException {
        PersonLdap person = new PersonLdap();
        person.setCn((String)attrs.get("cn").get());
        Attribute sn = attrs.get("sn");
        if (sn != null){
            person.setSn((String) sn.get());
        }
        Attribute givenName = attrs.get("givenName");
        if (givenName != null){
            person.setGivenName((String) givenName.get());
        }
        Attribute mail = attrs.get("mail");
        if (mail != null){
            person.setMail((String) mail.get());
        }
        Attribute eduPersonPrincipalName = attrs.get("eduPersonPrincipalName");
        if (eduPersonPrincipalName != null){
            person.setEduPersonPrincipalName((String) eduPersonPrincipalName.get());
        }
        return person;
    }
}
