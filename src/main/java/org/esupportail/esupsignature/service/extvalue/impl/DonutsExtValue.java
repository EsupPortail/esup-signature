package org.esupportail.esupsignature.service.extvalue.impl;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.entity.enums.SearchType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DonutsExtValue implements ExtValue {

	@Override
	public String getValueByName(String name, User user) {
		return initValues(user).get(name).toString();
	}

	@Override
	public List<Map<String, Object>> search(SearchType searchType, String searchString) {
		return null;
	}

	@Override
	public String getName() {
		return "donuts";
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		values.put("address", "");
		return values;
	}
	
}
