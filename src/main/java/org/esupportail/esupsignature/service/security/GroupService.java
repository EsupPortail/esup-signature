package org.esupportail.esupsignature.service.security;

import java.util.List;
import java.util.Map;

public interface GroupService {

	List<Map.Entry<String, String>> getAllGroups(String search);
	List<String> getGroups(String eppn);
	List<String> getMembers(String groupName);

}