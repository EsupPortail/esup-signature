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
				if(field.getExtValueServiceName().equals("ldap") && field.getExtValueType() != null && !field.getExtValueType().isEmpty()) {
					StringBuilder result = new StringBuilder();
					String separator = " - ";
					String[] returnValues = field.getExtValueReturn().split(";");
					for(String returnValue : returnValues) {
						returnValue = returnValue.trim();
						if (ldapValues.containsKey(returnValue)) {
							if (returnValue.equals("schacDateOfBirth")) {
								result.append(extLdapValue.getValueByName("schacDateOfBirth", user));
							} else if (returnValue.equals("supannEntiteAffectationPrincipale")) {
								List<Map<String, Object>> ouList = extLdapValue.search("organizationalUnit", (String) ldapValues.get(returnValue.trim()), "description");
								if(ouList.size() > 0) {
									result.append(ouList.get(0).get("value"));
								}
							} else {
								result.append((String) ldapValues.get(returnValue.trim()));
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
						field.setDefaultValue((String) defaultValues.get(field.getExtValueReturn()));
					}
				}
			}
			filledFields.add(field);
		}
		return filledFields;
	}

}