package org.esupportail.esupsignature.ldap;

import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(base = "ou=structures", objectClasses = {"organizationalUnit"})
public final class OrganizationalUnitLdap {

    @Id
    private Name dn;
    private String postalAddress;
    private String supannCodeEntite;

    public String getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }

    public String getSupannCodeEntite() {
        return supannCodeEntite;
    }

    public void setSupannCodeEntite(String supannCodeEntite) {
        this.supannCodeEntite = supannCodeEntite;
    }
}

