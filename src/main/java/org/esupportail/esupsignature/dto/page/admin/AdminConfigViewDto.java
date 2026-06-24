package org.esupportail.esupsignature.dto.page.admin;

import java.util.Map;

public class AdminConfigViewDto {

    private Map<String, String> mappingFiltersGroups;
    private Map<String, String> mappingGroupsRoles;
    private Map<String, String> groupMappingSpel;
    private Boolean hideAutoSign;

    public Map<String, String> getMappingFiltersGroups() {
        return mappingFiltersGroups;
    }

    public void setMappingFiltersGroups(Map<String, String> mappingFiltersGroups) {
        this.mappingFiltersGroups = mappingFiltersGroups;
    }

    public Map<String, String> getMappingGroupsRoles() {
        return mappingGroupsRoles;
    }

    public void setMappingGroupsRoles(Map<String, String> mappingGroupsRoles) {
        this.mappingGroupsRoles = mappingGroupsRoles;
    }

    public Map<String, String> getGroupMappingSpel() {
        return groupMappingSpel;
    }

    public void setGroupMappingSpel(Map<String, String> groupMappingSpel) {
        this.groupMappingSpel = groupMappingSpel;
    }

    public Boolean getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(Boolean hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }

    public Map<String, String> mappingFiltersGroups() {
        return mappingFiltersGroups;
    }

    public Map<String, String> mappingGroupsRoles() {
        return mappingGroupsRoles;
    }

    public Map<String, String> groupMappingSpel() {
        return groupMappingSpel;
    }

    public Boolean hideAutoSign() {
        return hideAutoSign;
    }
}
