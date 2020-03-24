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
import java.util.*;

@Component
public class DefaultExtValue implements ExtValue {

	@Resource
	private UserService userService;

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
		values.put("year", new SimpleDateFormat("YYYY").format(date));
		values.put("signDate", new SimpleDateFormat("dd/MM/YYYY").format(date));
		values.put("date", new SimpleDateFormat("dd/MM/YYYY").format(date));
		values.put("time", new SimpleDateFormat("HH:mm").format(date));
		values.put("dateTime", new SimpleDateFormat("dd/MM/YYYY HH:mm").format(date));
		values.put("currentUser", user.getFirstname() + " " + user.getName());
		values.put("stepUsers", Arrays.asList(""));
		return values;
	}

}
