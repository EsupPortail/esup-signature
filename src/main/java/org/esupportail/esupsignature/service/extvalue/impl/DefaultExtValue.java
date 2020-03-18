package org.esupportail.esupsignature.service.extvalue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultExtValue implements ExtValue {

	@Override
	public String getValueByName(String name, User user) {
		return initValues(user).get(name).toString();
	}

	@Override
	public String getName() {
		return "default";
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		Date date = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		values.put("day", new SimpleDateFormat("dd").format(date));
		values.put("month", new SimpleDateFormat("MM").format(date));
		return values;
	}

}
