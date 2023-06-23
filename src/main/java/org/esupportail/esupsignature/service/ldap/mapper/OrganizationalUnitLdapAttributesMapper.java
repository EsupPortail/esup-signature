package org.esupportail.esupsignature.service.ldap.mapper;

import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

public class OrganizationalUnitLdapAttributesMapper implements AttributesMapper<OrganizationalUnitLdap> {

    @Override
    public OrganizationalUnitLdap mapFromAttributes(Attributes attributes) throws NamingException {
        OrganizationalUnitLdap organizationalUnit = new OrganizationalUnitLdap();
        organizationalUnit.setPostalAddress(getStringAttribute(attributes, "postalAddress"));
        organizationalUnit.setSupannCodeEntiteParent(getStringAttribute(attributes, "supannCodeEntiteParent"));
        organizationalUnit.setSupannCodeEntite(getStringAttribute(attributes, "supannCodeEntite"));
        organizationalUnit.setSupannTypeEntite(getStringAttribute(attributes, "supannTypeEntite"));
        organizationalUnit.setDescription(getStringAttribute(attributes, "description"));
        return organizationalUnit;
    }

    private String getStringAttribute(Attributes attributes, String attributeName) throws NamingException {
        return (String) attributes.get(attributeName).get();
    }
}
