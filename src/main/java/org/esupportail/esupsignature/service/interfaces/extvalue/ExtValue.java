package org.esupportail.esupsignature.service.interfaces.extvalue;

import org.esupportail.esupsignature.entity.User;

import java.util.List;
import java.util.Map;

public interface ExtValue {
	String getName();
	Map<String, Object> initValues(User user);
	String getValueByName(String name, User user);
	List<Map<String, Object>> search(String searchType, String searchString, String searchReturn);

}
