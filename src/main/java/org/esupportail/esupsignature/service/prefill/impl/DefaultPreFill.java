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
				if(field.getExtValueServiceName().equals("ldap")) {
					if(ldapValues.containsKey(field.getExtValueReturn())) {
						if(field.getExtValueReturn().equals("schacDateOfBirth")) {
							field.setDefaultValue(extLdapValue.getValueByName("schacDateOfBirth", user));
						} else {
							field.setDefaultValue((String) ldapValues.get(field.getExtValueReturn().trim()));
						}
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