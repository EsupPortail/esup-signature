package org.esupportail.esupsignature.service.extvalue;

import org.esupportail.esupsignature.entity.User;

import java.util.Map;

public interface ExtValue {

	public String getName();
	public String getValueByNameAndUser(String name, User user);
	public Map<String, Object> getAllValuesByUser(User user);
}
