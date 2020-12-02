package org.esupportail.esupsignature.service.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Group2UserRoleService {

	protected GroupService groupService;

	protected Map<String, String> mappingGroupesRoles;

	private String prefix;

	public void setGroupService(GroupService groupService) {
		this.groupService = groupService;
	}

	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Set<String> getRoles(String eppn) {
		Set<String> roles = new HashSet<>();
		for(String groupName : groupService.getGroups(eppn)) {
			if (mappingGroupesRoles.containsKey(groupName)) {
				String role = mappingGroupesRoles.get(groupName);
				roles.add(role);
			} else if (groupName.contains(prefix)) {
				roles.add(groupName);
			}
		}
		return roles;
	}
	
}