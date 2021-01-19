package org.esupportail.esupsignature.service.interfaces.extvalue.impl;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.interfaces.extvalue.ExtValue;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DonutsExtValue implements ExtValue {

	@Override
	public String getValueByName(String name, User user) {
		return initValues(user).get(name).toString();
	}

	@Override
	public List<Map<String, Object>> search(String searchType, String searchString, String searchReturn) {
		return null;
	}

	@Override
	public String getName() {
		return "donuts";
	}

	@Override
	public List<String> getTypes() {
		return new ArrayList<>(Arrays.asList("default"));
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		values.put("address", "");
		return values;
	}
	
}
