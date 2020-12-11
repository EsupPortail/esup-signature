package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base"})
@Component
public class LdapExtValue implements ExtValue {

	private static final Logger logger = LoggerFactory.getLogger(LdapExtValue.class);

	@Resource
	private UserService userService;

	@Autowired
	private ObjectProvider<LdapPersonService> ldapPersonService;

	@Resource
	private LdapOrganizationalUnitService ldapOrganizationalUnitService;

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public String getValueByName(String name, User user) {
		Object value = initValues(user).get(name);
		if(value != null){
			if (name.equals("schacDateOfBirth")) {
				String schacDateOfBirth = value.toString();
				if(!schacDateOfBirth.isEmpty()) {
					DateFormat originalFormat = new SimpleDateFormat("yyyyMMdd");
					DateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");
					try {
						Date date = originalFormat.parse(schacDateOfBirth);
						value = targetFormat.format(date);
					} catch (ParseException e) {
						logger.warn("unable to parse date " + name);
					}
				}
			}
			return value.toString();
		} else {
			return "";
		}
	}

	@Override
	public List<Map<String, Object>> search(String searchType, String searchString, String searchReturn) {
		List<Map<String, Object>> mapList = new ArrayList<>();
		String separator = " - ";
		String[] returnValues = searchReturn.split(";");
		String[] returnTypes = searchType.split(";");
		if(!returnValues[0].isEmpty()) {
			String methodName = "get" + returnValues[0].substring(0, 1).toUpperCase() + returnValues[0].substring(1);
			if (returnTypes[0].equals("person")) {
				List<PersonLdap> personLdaps = ldapPersonService.getIfAvailable().search(searchString);
				for (PersonLdap personLdap : personLdaps) {
					Map<String, Object> stringObjectMap = new HashMap<>();
					try {
						StringBuilder result = new StringBuilder(PersonLdap.class.getMethod(methodName, null).invoke(personLdap).toString());
						if (returnTypes.length > 1) {
							for (int i = 1; i < returnTypes.length; i++) {
								String methodNameNext = "get" + returnValues[i].substring(0, 1).toUpperCase() + returnValues[i].substring(1);
								if (returnTypes[i].equals("person")) {
									result.append(separator).append(PersonLdap.class.getMethod(methodNameNext, null).invoke(personLdap).toString());
								} else if (returnTypes[i].equals("organizationalUnit")) {
									Object ou = PersonLdap.class.getMethod(methodNameNext).invoke(personLdap);
									if (ou != null) {
										List<Map<String, Object>> resultMap = getMapsOfOU(ou.toString(), "getDescription");
										if (resultMap.size() > 0) {
											result.append(separator).append(resultMap.get(0).get("value"));
										}
									}
								}
							}
						}
						stringObjectMap.put("value", result.toString());
						stringObjectMap.put("text", result.toString());
						mapList.add(stringObjectMap);
					} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
						logger.error("error on get ldap search return attribut : " + returnValues[0], e);
					}
				}
			} else if (returnTypes[0].equals("organizationalUnit")) {
				List<Map<String, Object>> resultMap = getMapsOfOU(searchString, methodName);
				mapList.addAll(resultMap);
			}
		}
		return mapList;
	}

	@NotNull
	private List<Map<String, Object>> getMapsOfOU(String searchString, String name) {
		List<Map<String, Object>> resultMap = new ArrayList<>();
		List<OrganizationalUnitLdap> organizationalUnitLdaps = ldapOrganizationalUnitService.getOrganizationalUnitLdaps(searchString);
		for(OrganizationalUnitLdap organizationalUnitLdap : organizationalUnitLdaps) {
			Map<String, Object> stringObjectMap = new HashMap<>();
			try {
				stringObjectMap.put("value", OrganizationalUnitLdap.class.getMethod(name, null).invoke(organizationalUnitLdap));
			} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				logger.error("error on get ldap search return attribut : " + name, e);
			}
			stringObjectMap.put("text", organizationalUnitLdap.getDescription());
			resultMap.add(stringObjectMap);
		}
		return resultMap;
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
					OrganizationalUnitLdap organizationalUnitLdap = ldapOrganizationalUnitService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
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
