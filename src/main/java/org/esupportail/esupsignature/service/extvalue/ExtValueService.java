package org.esupportail.esupsignature.service.extvalue;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ExtValueService {

	@Resource
	private List<ExtValue> extValues;

	public List<ExtValue> getExtValues() {
		return extValues;
	}

	public ExtValue getExtValueServiceByName(String name) {
		for(ExtValue extValue : extValues ) {
			if(extValue.getName().equals(name)) {
				return extValue;
			}
		}
		return null;
	}
	
	public ExtValue getExtValueServiceByClassName(String className) {
		for(ExtValue extValue : extValues ) {
			if(extValue.getClass().getSimpleName().equals(className)) {
				return extValue;
			}
		}
		return null;
	}
	
}
