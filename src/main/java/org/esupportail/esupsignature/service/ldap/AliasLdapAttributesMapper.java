package org.esupportail.esupsignature.service.ldap;

import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;


public class AliasLdapAttributesMapper implements AttributesMapper<AliasLdap> {

    public AliasLdap mapFromAttributes(Attributes attrs) throws NamingException {
        if(attrs.get("mail") != null) {
            AliasLdap aliasLdap = new AliasLdap();
            aliasLdap.setCn((String) attrs.get("cn").get());
            aliasLdap.setMailAlias((String) attrs.get("mail").get(0));
            return aliasLdap;
        } else {
            return null;
        }
    }
}
