package org.esupportail.esupsignature.service.interfaces.prefill;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class PreFillService {

	@Resource
	private List<PreFill> preFillValues;

	public List<PreFill> getPreFillValues() {
		return preFillValues;
	}

	public PreFill getPreFillServiceByName(String name) {
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getName().equals(name)) {
				return preFillValue;
			}
		}
		return null;
	}
	
	public List<Field> getPreFilledFieldsByServiceName(String name, List<Field> fields, User user, SignRequest signRequest) {
		if(name == null || name.isEmpty()) {
			name = "default";
		}
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getName().equals(name)) {
				return preFillValue.preFillFields(fields, user, signRequest);
			}
		}
		return fields;
	}
	
	public List<Field> getPreFillByType(String className, List<Field> fields, User user, SignRequest signRequest) {
		for(PreFill preFillValue : preFillValues ) {
			if(preFillValue.getClass().getSimpleName().equals(className)) {
				return preFillValue.preFillFields(fields, user, signRequest);
			}
		}
		return null;
	}
	
}
