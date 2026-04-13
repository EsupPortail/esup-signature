package org.esupportail.esupsignature.dto.view.admin;

import java.util.Map;

public record AdminConfigViewDto(
        Map<String, String> mappingFiltersGroups,
        Map<String, String> mappingGroupsRoles,
        Map<String, String> groupMappingSpel,
        Boolean hideAutoSign
) {
}
