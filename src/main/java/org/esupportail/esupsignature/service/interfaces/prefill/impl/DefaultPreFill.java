package org.esupportail.esupsignature.service.interfaces.prefill.impl;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValueService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;

@Component
public class DefaultPreFill implements PreFill {

	private String name = "default";
	private String description = "Pré-remplissage par défaut (données LDAP)";

	@Resource
	private ExtValueService extValueService;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, List<String>> getTypes() {
		Map<String, List<String>> types = new HashMap<>();
		types.put("default", Arrays.asList("system"));
		types.put("ldap", Arrays.asList("person", "organizationalUnit"));
		types.put("rest", Arrays.asList("data"));
		return types;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<Field> preFillFields(List<Field> fields, User user, SignRequest signRequest) {
		List<Field> filledFields = new ArrayList<>();
		PDFont font = PDType1Font.HELVETICA;
		PDResources resources = new PDResources();
		resources.put(COSName.getPDFName("Helvetica"), font);
		Map<String, Object> defaultValues = new HashMap<>();
		ExtValue extDefaultValue = extValueService.getExtValueServiceByName("default");
		if(extDefaultValue != null) {
			defaultValues = extDefaultValue.initValues(user, signRequest);
		}
		Map<String, Object> restValues = new HashMap<>();
		ExtValue extRestValue = extValueService.getExtValueServiceByName("rest");
		if(extRestValue != null) {
			restValues = extRestValue.initValues(user, signRequest);
		}
		ExtValue extLdapValue = extValueService.getExtValueServiceByName("ldap");
		Map<String, Object> ldapValues = new HashMap<>();
		if(extLdapValue != null) {
			 ldapValues = extLdapValue.initValues(user, signRequest);
		}
		for(Field field : fields) {
			if(field.getExtValueServiceName() != null && !field.getExtValueServiceName().isEmpty()) {
				if(field.getExtValueServiceName().equals("ldap") && field.getExtValueType() != null && !field.getExtValueType().isEmpty()) {
					StringBuilder result = new StringBuilder();
					String separator = " - ";
					String[] returnValues = field.getExtValueReturn().split(";");
					for(String returnValue : returnValues) {
						returnValue = returnValue.trim();
						if (ldapValues.containsKey(returnValue)) {
							if (returnValue.equals("schacDateOfBirth")) {
								result.append(extLdapValue.getValueByName("schacDateOfBirth", user, signRequest));
							} else if (returnValue.equals("supannEntiteAffectationPrincipale")) {
								List<Map<String, Object>> ouList = extLdapValue.search("organizationalUnit", ldapValues.get(returnValue.trim()).toString(), "description");
								if(!ouList.isEmpty()) {
									result.append(ouList.get(0).get("value"));
								}
							} else {
								if(ldapValues.get(returnValue.trim()) != null) {
									result.append(ldapValues.get(returnValue.trim()).toString());
								}
							}
							if(returnValues.length > 1) {
								result.append(separator);
							}
						}
					}
					if(returnValues.length > 1) {
						field.setDefaultValue(result.substring(0, result.length() - separator.length()));
					} else {
						field.setDefaultValue(result.toString());
					}
				} else if(field.getExtValueServiceName().equals("default")) {
					if(defaultValues.containsKey(field.getExtValueReturn())) {
						field.setDefaultValue(defaultValues.get(field.getExtValueReturn()).toString());
					}
				} else if(field.getExtValueServiceName().equals("rest")) {
					if(restValues.containsKey(field.getExtValueReturn())) {
						field.setDefaultValue(restValues.get(field.getExtValueReturn()).toString());
					}
				}
			}
			filledFields.add(field);
		}
		return filledFields;
	}

}