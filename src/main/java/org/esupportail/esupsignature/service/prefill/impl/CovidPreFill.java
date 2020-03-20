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

	@Resource
	private ExtValueService extValueService;

	@Override
	public String getName() {
		return name;
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
			if(field.getName().split("_")[0].equals("extvalue")) {
				if(field.getName().split("_")[1].equals("ldap")) {	
					String extValueName = field.getName().split("_")[2];
					if(ldapValues.containsKey(extValueName)) {
						field.setDefaultValue((String) ldapValues.get(extValueName));
					}
				} else if(field.getName().split("_")[1].equals("default")) {
					String extValueName = field.getName().split("_")[2];
					if(defaultValues.containsKey(extValueName)) {
						field.setDefaultValue((String) defaultValues.get(extValueName));
					}
				}
			}
			filledFields.add(field);
		}
		return filledFields;
	}

	
}
