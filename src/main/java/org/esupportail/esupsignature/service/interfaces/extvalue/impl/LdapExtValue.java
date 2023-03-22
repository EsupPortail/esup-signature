package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@ConditionalOnProperty({"spring.ldap.base"})
@Component
public class LdapExtValue implements ExtValue {

	private static final Logger logger = LoggerFactory.getLogger(LdapExtValue.class);

	@Resource
	private UserService userService;

	@Resource
	private LdapPersonService ldapPersonService;

	@Resource
	private LdapOrganizationalUnitService ldapOrganizationalUnitService;

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public String getValueByName(String name, User user, SignRequest signRequest) {
		Object value = initValues(user, signRequest).get(name);
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
				List<PersonLdap> personLdaps = ldapPersonService.search(searchString);
				for (PersonLdap personLdap : personLdaps) {
					try {
						StringBuilder result = new StringBuilder(PersonLdap.class.getMethod(methodName).invoke(personLdap).toString());
						if (returnTypes.length > 1) {
							for (int i = 1; i < returnTypes.length; i++) {
								String methodNameNext = "get" + returnValues[i].substring(0, 1).toUpperCase() + returnValues[i].substring(1);
								if (returnTypes[i].equals("person")) {
									result.append(separator).append(PersonLdap.class.getMethod(methodNameNext).invoke(personLdap).toString());
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
						if(result.toString().toLowerCase().contains(searchString.toLowerCase(Locale.ROOT)) && mapList.stream().noneMatch(stringObjectMap1 -> stringObjectMap1.containsKey(result.toString()) || stringObjectMap1.containsValue(result.toString()))) {
							Map<String, Object> stringObjectMap = new HashMap<>();
							stringObjectMap.put("value", result.toString());
							stringObjectMap.put("text", result.toString());
							mapList.add(stringObjectMap);
						}
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

	private List<Map<String, Object>> getMapsOfOU(String searchString, String name) {
		List<Map<String, Object>> resultMap = new ArrayList<>();
		List<OrganizationalUnitLdap> organizationalUnitLdaps = ldapOrganizationalUnitService.getOrganizationalUnitLdaps(searchString);
		for(OrganizationalUnitLdap organizationalUnitLdap : organizationalUnitLdaps) {
			Map<String, Object> stringObjectMap = new HashMap<>();
			try {
				stringObjectMap.put("value", OrganizationalUnitLdap.class.getMethod(name).invoke(organizationalUnitLdap));
			} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				logger.error("error on get ldap search return attribut : " + name, e);
			}
			stringObjectMap.put("text", organizationalUnitLdap.getDescription());
			resultMap.add(stringObjectMap);
		}
		return resultMap;
	}

	@Override
	public Map<String, Object> initValues(User user, SignRequest signRequest) {
		Map<String, Object> values = new HashMap<>();
		if(user != null && user.getEppn() != null) {
			PersonLdap personLdap = userService.findPersonLdapByUser(user);
			if (personLdap != null) {
				ObjectMapper oMapper = new ObjectMapper();
				TypeReference<Map<String, Object>> type = new TypeReference<>(){};
				values.putAll(oMapper.convertValue(personLdap, type));
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