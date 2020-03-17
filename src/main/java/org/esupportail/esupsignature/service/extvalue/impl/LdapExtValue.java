package org.esupportail.esupsignature.service.extvalue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@Component
public class LdapExtValue implements ExtValue {

	@Resource
	private UserService userService;

	@Resource
	private LdapPersonService ldapPersonService;
	
	@Override
	public String getValueByNameAndUser(String name, User user) {
		PersonLdap personLdap = userService.getPersonLdapFromUser(user);
		try {
			return personLdap.getClass().getDeclaredField(name).get(personLdap).toString();
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public Map<String, Object> getAllValuesByUser(User user) {
		PersonLdap personLdap = userService.getPersonLdap(user);
		if(personLdap != null) {
			OrganizationalUnitLdap organizationalUnitLdap = ldapPersonService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
			if(organizationalUnitLdap != null) {
				ObjectMapper oMapper = new ObjectMapper();
				Map<String, Object> values = oMapper.convertValue(personLdap, Map.class);
				values.put("postalAddress", organizationalUnitLdap.getPostalAddress());
				return values;
			}
		}
		return new HashMap<>();
	}
	
}
