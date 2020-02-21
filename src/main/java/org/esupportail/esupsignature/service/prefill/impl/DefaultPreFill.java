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

@Component
public class DefaultPreFill implements PreFill {

	private String name = "default";

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
		for(Field field : fields) {
			if(field.getName().startsWith("extvalue")) {
				String extValueType = field.getName().split("_")[1];
				String extValueName = field.getName().split("_")[2];
				ExtValue extValue = extValueService.getExtValueServiceByName(extValueType);
				field.setDefaultValue(extValue.getValueByNameAndUser(extValueName, user));
			}
			filledFields.add(field);
		}
		return filledFields;
	}

}