package org.esupportail.esupsignature.dto.view.admin;

import java.util.List;

public record AdminFormListViewDto(
        String workflowRole,
        Boolean activeVersion,
        List<Long> selectedTagIds,
        List<TagDto> allTags,
        List<String> roles,
        List<WorkflowOptionDto> workflowTypes,
        List<PreFillOptionDto> preFillTypes,
        List<RowDto> forms
) {

    public record RowDto(
            Long id,
            String name,
            String title,
            List<TagDto> tags,
            boolean featured,
            WorkflowOptionDto workflow,
            boolean activeVersion,
            boolean hideButton,
            boolean deleted,
            boolean publicUsage,
            List<String> roles
    ) {
    }

    public record WorkflowOptionDto(
            Long id,
            String description
    ) {
    }

    public record PreFillOptionDto(
            String name,
            String description
    ) {
    }

    public record TagDto(
            Long id,
            String name,
            String color
    ) {
    }
}


