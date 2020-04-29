package org.esupportail.esupsignature.service.extvalue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class LdapExtValue implements ExtValue {

	@Resource
	private UserService userService;

	@Autowired(required = false)
	private LdapPersonService ldapPersonService;
	
	@Override
	public String getValueByName(String name, User user) {
		if(initValues(user).get(name) != null){
			if (name.equals("schacDateOfBirth")) {
				String schacDateOfBirth = initValues(user).get(name).toString();
				DateFormat originalFormat = new SimpleDateFormat("yyyyMMdd");
				DateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");
				try {
					Date date = originalFormat.parse(schacDateOfBirth);
					return targetFormat.format(date);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			return initValues(user).get(name).toString();
		} else {
			return "";
		}
	}

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		if(user != null) {
			PersonLdap personLdap = userService.findPersonLdapByUser(user);
			if (personLdap != null && values.size() == 0) {
				ObjectMapper oMapper = new ObjectMapper();
				values.putAll(oMapper.convertValue(personLdap, Map.class));
				if(values.containsKey("postalAddress") && values.get("postalAddress") != null) {
					String postalAddress = values.get("postalAddress").toString();
					if (postalAddress != null) {
						values.put("postalAddress", postalAddress.replaceAll("\\$", " \n"));
					}
				}
				if(personLdap.getSupannEntiteAffectationPrincipale() != null && ldapPersonService != null) {
					OrganizationalUnitLdap organizationalUnitLdap = ldapPersonService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
					if (organizationalUnitLdap != null) {
						values.put("organizationalUnit-postalAddress", organizationalUnitLdap.getPostalAddress());
						values.put("organizationalUnit-description", organizationalUnitLdap.getDescription());
					}
				}
			}
		}
		return values;
	}
	
}
