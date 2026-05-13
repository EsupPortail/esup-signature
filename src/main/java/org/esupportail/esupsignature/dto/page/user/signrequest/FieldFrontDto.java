package org.esupportail.esupsignature.dto.page.user.signrequest;

import java.util.List;

public class FieldFrontDto {

    private Long id;
    private String name;
    private String description;
    private Integer page;
    private Boolean required;
    private Boolean readOnly;
    private Boolean editable;
    private List<Integer> workflowSteps;
    private String defaultValue;
    private String searchServiceName;
    private String searchType;
    private String searchReturn;
    private String type;
    private Boolean favorisable;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Boolean getReadOnly() { return readOnly; }
    public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public List<Integer> getWorkflowSteps() { return workflowSteps; }
    public void setWorkflowSteps(List<Integer> workflowSteps) { this.workflowSteps = workflowSteps; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getSearchServiceName() { return searchServiceName; }
    public void setSearchServiceName(String searchServiceName) { this.searchServiceName = searchServiceName; }
    public String getSearchType() { return searchType; }
    public void setSearchType(String searchType) { this.searchType = searchType; }
    public String getSearchReturn() { return searchReturn; }
    public void setSearchReturn(String searchReturn) { this.searchReturn = searchReturn; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getFavorisable() { return favorisable; }
    public void setFavorisable(Boolean favorisable) { this.favorisable = favorisable; }

    public Long id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public Integer page() { return page; }
    public Boolean required() { return required; }
    public Boolean readOnly() { return readOnly; }
    public Boolean editable() { return editable; }
    public List<Integer> workflowSteps() { return workflowSteps; }
    public String defaultValue() { return defaultValue; }
    public String searchServiceName() { return searchServiceName; }
    public String searchType() { return searchType; }
    public String searchReturn() { return searchReturn; }
    public String type() { return type; }
    public Boolean favorisable() { return favorisable; }
}
