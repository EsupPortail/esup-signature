package org.esupportail.esupsignature.service.security;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class Group2UserRoleService {

	protected GroupService groupService;

	protected Map<String, String> mappingGroupesRoles;

	private String groupPrefixRoleName;

	public void setGroupService(GroupService groupService) {
		this.groupService = groupService;
	}

	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}

	public void setGroupPrefixRoleName(String groupPrefixRoleName) {
		this.groupPrefixRoleName = groupPrefixRoleName;
	}

	public Set<String> getRoles(String userName) {
		Set<String> roles = new HashSet<>();
		for(String groupName : groupService.getGroups(userName)) {
			if (mappingGroupesRoles.containsKey(groupName)) {
				String role = mappingGroupesRoles.get(groupName);
				roles.add(role);
			} else if (groupName.contains(groupPrefixRoleName)) {
				roles.add(groupName);
			}
		}
		return roles;
	}
	
}