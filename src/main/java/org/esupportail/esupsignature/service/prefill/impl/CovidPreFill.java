package org.esupportail.esupsignature.service.prefill.impl;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.extvalue.ExtValueService;
import org.esupportail.esupsignature.service.prefill.PreFill;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
						if (ldapValues.containsKey("supannEntiteAffectationPrincipale")) {
							field.setDefaultValue(extLdapValue.search("organizationalUnit", ldapValues.get("supannEntiteAffectationPrincipale").toString(), extValueName).get(0).get("value").toString());
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
