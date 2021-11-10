package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;


public class AliasLdapAttributesMapper implements AttributesMapper<AliasLdap> {

    public AliasLdap mapFromAttributes(Attributes attrs) throws NamingException {
        if(attrs.get("mail") != null) {
            AliasLdap aliasLdap = new AliasLdap();
            aliasLdap.setCn(attrs.get("cn").get().toString());
            aliasLdap.setMailAlias(attrs.get("mail").get(0).toString());
            return aliasLdap;
        } else {
            return null;
        }
    }
}
