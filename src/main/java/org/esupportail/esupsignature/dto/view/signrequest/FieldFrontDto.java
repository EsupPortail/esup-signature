package org.esupportail.esupsignature.dto.view.signrequest;

import java.util.List;

public record FieldFrontDto(
        Long id,
        String name,
        String description,
        Integer page,
        Boolean required,
        Boolean readOnly,
        Boolean editable,
        List<Integer> workflowSteps,
        String defaultValue,
        String searchServiceName,
        String searchType,
        String searchReturn,
        String type,
        Boolean favorisable
) {}

