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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class DonutsExtValue implements ExtValue {

	@Resource
	private UserService userService;

	@Resource
	private LdapPersonService ldapPersonService;
	
	@Override
	public String getValueByName(String name, User user) {
		return initValues(user).get(name).toString();
	}

	@Override
	public String getName() {
		return "donuts";
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		values.put("address", "");
		return values;
	}
	
}
