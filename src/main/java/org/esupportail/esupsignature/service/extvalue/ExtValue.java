package org.esupportail.esupsignature.service.extvalue;

import org.esupportail.esupsignature.entity.User;

import java.util.Map;

public interface ExtValue {
	String getName();
	Map<String, Object> initValues(User user);
	String getValueByName(String name, User user);

}
