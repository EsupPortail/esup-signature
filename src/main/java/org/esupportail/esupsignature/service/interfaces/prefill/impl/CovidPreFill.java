package org.esupportail.esupsignature.service.interfaces.prefill.impl;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValueService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component
public class CovidPreFill implements PreFill {

	private String name = "covid";

	private String description = "Pré-remplissage automatique des demandes COVID";

	@Resource
	private ExtValueService extValueService;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Map<String, List<String>> getTypes() {
		Map<String, List<String>> types = new HashMap<>();
		types.put("ldap", Arrays.asList("person", "organizationalUnit"));
		return types;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<Field> preFillFields(List<Field> fields, User user) {
		List<Field> filledFields = new ArrayList<>();
		PDFont font = PDType1Font.HELVETICA;
		PDResources resources = new PDResources();
		resources.put(COSName.getPDFName("Helvetica"), font);
		ExtValue extDefaultValue = extValueService.getExtValueServiceByName("default");
		Map<String, Object> defaultValues = extDefaultValue.initValues(user);
		ExtValue extLdapValue = extValueService.getExtValueServiceByName("ldap");
		Map<String, Object> ldapValues = extLdapValue.initValues(user);
		for(Field field : fields) {
			if(field.getExtValueServiceName() != null && !field.getExtValueServiceName().isEmpty()) {
				if(field.getExtValueServiceName().equals("ldap")) {
					if(field.getExtValueType().equals("person")) {
						String extValueName = field.getExtValueReturn();
						if (ldapValues.containsKey(extValueName)) {
							if (extValueName.equals("schacDateOfBirth")) {
								field.setDefaultValue(extLdapValue.getValueByName("schacDateOfBirth", user));
							} else {
								field.setDefaultValue((String) ldapValues.get(extValueName));
							}
						}
					} else if(field.getExtValueType().equals("organizationalUnit")) {
						String extValueName = field.getExtValueReturn();
						if (ldapValues.containsKey("supannEntiteAffectationPrincipale") && ldapValues.get("supannEntiteAffectationPrincipale") != null) {
							List<Map<String, Object>> supannEntiteAffectationPrincipalValues = extLdapValue.search("organizationalUnit", ldapValues.get("supannEntiteAffectationPrincipale").toString(), extValueName);
							if(supannEntiteAffectationPrincipalValues.size() > 0) {
								field.setDefaultValue(supannEntiteAffectationPrincipalValues.get(0).get("value").toString());
							}
						}
					}
				} else if(field.getExtValueServiceName().equals("default")) {
					String extValueName = field.getExtValueReturn();
					if(defaultValues.containsKey(extValueName)) {
						field.setDefaultValue((String) defaultValues.get(extValueName));
					}
				}
				if(field.getExtValueReturn().equals("covid(duree)")) {
					field.setDefaultValue("1");
				}
			}
			filledFields.add(field);
		}
		return filledFields;
	}

	
}
