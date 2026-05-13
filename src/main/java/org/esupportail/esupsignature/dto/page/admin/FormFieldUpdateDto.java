package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.entity.enums.FieldType;

import java.util.List;

public class FormFieldUpdateDto {

    private Long id;
    private String description;
    private FieldType fieldType;
    private Boolean required;
    private Boolean favorisable;
    private Boolean readOnly;
    private Boolean prefill;
    private Boolean search;
    private String valueServiceName;
    private String valueType;
    private String valueReturn;
    private Boolean stepZero;
    private List<Long> workflowStepsIds;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Boolean getFavorisable() { return favorisable; }
    public void setFavorisable(Boolean favorisable) { this.favorisable = favorisable; }
    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
    public Boolean getPrefill() { return prefill; }
    public void setPrefill(Boolean prefill) { this.prefill = prefill; }
    public Boolean getSearch() { return search; }
    public void setSearch(Boolean search) { this.search = search; }
    public String getValueServiceName() { return valueServiceName; }
    public void setValueServiceName(String valueServiceName) { this.valueServiceName = valueServiceName; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public String getValueReturn() { return valueReturn; }
    public void setValueReturn(String valueReturn) { this.valueReturn = valueReturn; }
    public Boolean getStepZero() { return stepZero; }
    public void setStepZero(Boolean stepZero) { this.stepZero = stepZero; }
    public List<Long> getWorkflowStepsIds() { return workflowStepsIds; }
    public void setWorkflowStepsIds(List<Long> workflowStepsIds) { this.workflowStepsIds = workflowStepsIds; }

    public Long id() { return id; }
    public String description() { return description; }
    public FieldType fieldType() { return fieldType; }
    public Boolean required() { return required; }
    public Boolean favorisable() { return favorisable; }
    public Boolean readOnly() { return readOnly; }
    public Boolean prefill() { return prefill; }
    public Boolean search() { return search; }
    public String valueServiceName() { return valueServiceName; }
    public String valueType() { return valueType; }
    public String valueReturn() { return valueReturn; }
    public Boolean stepZero() { return stepZero; }
    public List<Long> workflowStepsIds() { return workflowStepsIds; }
}
