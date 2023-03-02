package org.esupportail.esupsignature.service.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.lang.reflect.Field;
import java.util.Enumeration;


public class PersonLdapAttributesMapper implements AttributesMapper<PersonLdap> {

    private static final Logger logger = LoggerFactory.getLogger(PersonLdapAttributesMapper.class);

    public PersonLdap mapFromAttributes(Attributes attrs) throws NamingException {
        PersonLdap person = new PersonLdap();
        for (Enumeration<String> attrsIDs = attrs.getIDs(); attrsIDs.hasMoreElements() ; ) {
            String attrID = attrsIDs.nextElement();
            Attribute attribute = attrs.get(attrID);
            if(attribute != null) {
                try {
                    Field field = PersonLdap.class.getDeclaredField(attrID);
                    field.setAccessible(true);
                    field.set(person, attribute.get().toString());
                    field.setAccessible(false);
                } catch (Exception e) {
                    logger.debug(e.getMessage());
                }
            }
        }
        return person;
    }
}
