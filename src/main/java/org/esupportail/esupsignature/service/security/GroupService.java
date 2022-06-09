package org.esupportail.esupsignature.service.security;

import java.util.List;

public interface GroupService {

	List<String> getAllGroups(String search);
	List<String> getGroups(String eppn);
	List<String> getMembers(String groupName);

}