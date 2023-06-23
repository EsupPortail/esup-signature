package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;

import java.util.List;
import java.util.Map;

public interface GroupService {

	List<Map.Entry<String, String>> getAllGroupsStartWith(String search);
	List<String> getGroupsOfUser(String userName);
	List<String> getMembers(String groupName) throws EsupSignatureRuntimeException;

}