package org.esupportail.esupsignature.service.extvalue.impl;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class LdapExtValue implements ExtValue {

	@Resource
	private UserService userService;
	
	@Override
	public String getValueByNameAndUser(String name, User user) {
		PersonLdap personLdap = userService.getPersonLdapFromUser(user);
		return personLdap.getValueByFieldName(name);
	}

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public Map<String, String> getAllValuesByUser(User user) {
		PersonLdap personLdap = userService.getPersonLdapFromUser(user);
		return personLdap.getAllValues();
	}
	
}
