package org.esupportail.esupsignature.service.prefill;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class PreFillService {

	@Resource
	private List<PreFill> preFillValues;

	public PreFill getPreFillServiceByClassName(String className) {
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getClass().getSimpleName().equals(className)) {
				return preFillValue;
			}
		}
		return null;
	}
	
	public List<Field> getPreFilledFieldsByServiceName(String name, List<Field> fields, User user) {
		if(name == null || name.isEmpty()) {
			name = "default";
		}
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getName().equals(name)) {
				return preFillValue.preFillFields(fields, user);
			}
		}
		return fields;
	}
	
	public List<Field> getPreFillByType(String className, List<Field> fields, User user) {
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getClass().getSimpleName().equals(className)) {
				return preFillValue.preFillFields(fields, user);
			}
		}
		return null;
	}
	
}
