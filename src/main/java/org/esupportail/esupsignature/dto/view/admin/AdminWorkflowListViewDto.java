package org.esupportail.esupsignature.dto.view.admin;

import java.util.List;

public record AdminWorkflowListViewDto(
        String workflowRole,
        String displayWorkflowType,
        List<Long> selectedTagIds,
        List<TagDto> allTags,
        List<String> roles,
        List<RowDto> workflows
) {

    public record RowDto(
            Long id,
            String description,
            List<TagDto> tags,
            boolean featured,
            boolean publicUsage,
            List<String> roles,
            String createByEppn,
            List<StepDto> workflowSteps,
            boolean documentsSourceUriPresent,
            boolean fromCode
    ) {
    }

    public record TagDto(
            Long id,
            String name,
            String color
    ) {
    }

    public record StepDto(
            int index,
            List<UserDto> users,
            boolean changeable,
            boolean autoSign
    ) {
    }

    public record UserDto(
            String firstname,
            String name
    ) {
    }
}


