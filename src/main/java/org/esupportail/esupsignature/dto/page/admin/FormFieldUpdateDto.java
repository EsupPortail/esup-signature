package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.entity.enums.FieldType;

import java.util.List;

public record FormFieldUpdateDto(
    Long id,
    String description,
    FieldType fieldType,
    Boolean required,
    Boolean favorisable,
    Boolean readOnly,
    Boolean prefill,
    Boolean search,
    String valueServiceName,
    String valueType,
    String valueReturn,
    Boolean stepZero,
    List<Long> workflowStepsIds
) {
}

