package org.esupportail.esupsignature.service.security;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;

import java.util.List;
import java.util.Map;

public interface GroupService {

	List<Map.Entry<String, String>> getAllGroups(String search);
	List<String> getGroups(String userName);
	List<String> getMembers(String groupName) throws EsupSignatureRuntimeException;

}