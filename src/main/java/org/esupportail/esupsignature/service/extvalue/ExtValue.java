package org.esupportail.esupsignature.service.extvalue;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SearchType;

import java.util.List;
import java.util.Map;

public interface ExtValue {
	String getName();
	Map<String, Object> initValues(User user);
	String getValueByName(String name, User user);
	List<Map<String, Object>> search(SearchType searchType, String searchString, String searchReturn);

}
