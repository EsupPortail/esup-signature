package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.List;

public class PersonLdapAttributesMapper implements AttributesMapper<PersonLdap> {

    @Override
    public PersonLdap mapFromAttributes(Attributes attributes) throws NamingException {
        PersonLdap person = new PersonLdap();
        person.setUid(getStringAttribute(attributes, "uid"));
        person.setCn(getStringAttribute(attributes, "cn"));
        person.setSn(getStringAttribute(attributes, "sn"));
        person.setGivenName(getStringAttribute(attributes, "givenName"));
        person.setDisplayName(getStringAttribute(attributes, "displayName"));
        person.setSchacDateOfBirth(getStringAttribute(attributes, "schacDateOfBirth"));
        person.setSchacPlaceOfBirth(getStringAttribute(attributes, "schacPlaceOfBirth"));
        person.setMail(getStringAttribute(attributes, "mail"));
        person.setMd5UserPassword(getStringAttribute(attributes, "md5UserPassword"));
        person.setCryptUserPassword(getStringAttribute(attributes, "cryptUserPassword"));
        person.setShaUserPassword(getStringAttribute(attributes, "shaUserPassword"));
        person.setEduPersonAffiliation(getStringListAttribute(attributes, "eduPersonAffiliation"));
        person.setEduPersonPrimaryAffiliation(getStringAttribute(attributes, "eduPersonPrimaryAffiliation"));
        person.setEduPersonPrincipalName(getStringAttribute(attributes, "eduPersonPrincipalName"));
        person.setMailDrop(getStringAttribute(attributes, "mailDrop"));
        person.setMailHost(getStringAttribute(attributes, "mailHost"));
        person.setSambaSID(getStringAttribute(attributes, "sambaSID"));
        person.setSambaPrimaryGroupSID(getStringAttribute(attributes, "sambaPrimaryGroupSID"));
        person.setSambaPwdLastSet(getStringAttribute(attributes, "sambaPwdLastSet"));
        person.setSambaLMPassword(getStringAttribute(attributes, "sambaLMPassword"));
        person.setSambaNTPassword(getStringAttribute(attributes, "sambaNTPassword"));
        person.setSambaAcctFlags(getStringAttribute(attributes, "sambaAcctFlags"));
        person.setHomeDirectory(getStringAttribute(attributes, "homeDirectory"));
        person.setUidNumber(getStringAttribute(attributes, "uidNumber"));
        person.setGidNumber(getStringAttribute(attributes, "gidNumber"));
        person.setPostalAddress(getStringAttribute(attributes, "postalAddress"));
        person.setFacsimileTelephoneNumber(getStringAttribute(attributes, "facsimileTelephoneNumber"));
        person.setTelephoneNumber(getStringAttribute(attributes, "telephoneNumber"));
        person.setSupannCivilite(getStringAttribute(attributes, "supannCivilite"));
        person.setSupannListeRouge(getStringAttribute(attributes, "supannListeRouge"));
        person.setSupannEtablissement(getStringAttribute(attributes, "supannEtablissement"));
        person.setSupannEntiteAffectation(getStringAttribute(attributes, "supannEntiteAffectation"));
        person.setSupannEntiteAffectationPrincipale(getStringAttribute(attributes, "supannEntiteAffectationPrincipale"));
        person.setSupannEmpId(getStringAttribute(attributes, "supannEmpId"));
        person.setSupannEmpCorps(getStringAttribute(attributes, "supannEmpCorps"));
        person.setSupannActivite(getStringAttribute(attributes, "supannActivite"));
        person.setSupannAutreTelephone(getStringAttribute(attributes, "supannAutreTelephone"));
        person.setSupannCodeINE(getStringAttribute(attributes, "supannCodeINE"));
        person.setSupannEtuId(getStringAttribute(attributes, "supannEtuId"));
        person.setSupannEtuEtape(getStringAttribute(attributes, "supannEtuEtape"));
        person.setSupannEtuAnneeInscription(getStringAttribute(attributes, "supannEtuAnneeInscription"));
        person.setSupannEtuSecteurDisciplinaire(getStringAttribute(attributes, "supannEtuSecteurDisciplinaire"));
        person.setSupannEtuDiplome(getStringAttribute(attributes, "supannEtuDiplome"));
        person.setSupannEtuTypeDiplome(getStringAttribute(attributes, "supannEtuTypeDiplome"));
        person.setSupannEtuCursusAnnee(getStringListAttribute(attributes, "supannEtuCursusAnnee"));
        person.setSupannParrainDN(getStringAttribute(attributes, "supannParrainDN"));
        person.setSupannMailPerso(getStringAttribute(attributes, "supannMailPerso"));
        person.setSupannAliasLogin(getStringAttribute(attributes, "supannAliasLogin"));
        person.setSupannRefId(getStringListAttribute(attributes, "supannRefId"));
        person.setSupannRoleGenerique(getStringAttribute(attributes, "supannRoleGenerique"));
        person.setSupannAutreMail(getStringAttribute(attributes, "supannAutreMail"));
        person.setMailuserquota(getLongAttribute(attributes, "mailuserquota"));
        person.setTitle(getStringAttribute(attributes, "title"));

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

    private Long getLongAttribute(Attributes attributes, String attributeName) throws NamingException {
        Attribute attribute = attributes.get(attributeName);
        if(attribute != null) {
            return Long.parseLong((String) attribute.get());
        } else {
            return -1L;
        }
    }
}