package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.FieldType;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Field {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
    private Integer version;
	
	private String name;

	private String description;

	private String label;
	
	private Integer fillOrder;
	
	private Integer topPos;
	
	private Integer leftPos;

	private Integer page;

	private Boolean required = false;

	private Boolean readOnly = false;

	@Transient
	private Boolean editable = false;

	@ManyToMany
	private List<WorkflowStep> workflowSteps = new ArrayList<>();

	private Boolean stepZero = true;

	private String extValueServiceName;

	private String extValueType;

	private String extValueReturn;

	transient String defaultValue;

	private String searchServiceName;

	private String searchType;

	private String searchReturn;

	@Enumerated(EnumType.STRING)
    private FieldType type;

	private Boolean favorisable = false;
	
	transient List<String> defaultValues = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public FieldType getType() {
		return type;
	}

	public void setType(FieldType type) {
		this.type = type;
	}

	public List<String> getDefaultValues() {
		return defaultValues;
	}

	public void setDefaultValues(List<String> defaultValues) {
		this.defaultValues = defaultValues;
	}

	public Integer getFillOrder() {
		return fillOrder;
	}

	public void setFillOrder(Integer fillOrder) {
		this.fillOrder = fillOrder;
	}

	public Integer getTopPos() {
		return topPos;
	}

	public void setTopPos(Integer top) {
		this.topPos = top;
	}

	public Integer getLeftPos() {
		return leftPos;
	}

	public void setLeftPos(Integer left) {
		this.leftPos = left;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Boolean getRequired() {
		return required;
	}

	public void setRequired(Boolean required) {
		this.required = required;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean disabled) {
		this.readOnly = disabled;
	}

	public List<WorkflowStep> getWorkflowSteps() {
		return workflowSteps;
	}

	public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
		this.workflowSteps = workflowSteps;
	}

	public Boolean getStepZero() {
		return stepZero;
	}

	public void setStepZero(Boolean stepZero) {
		this.stepZero = stepZero;
	}

	public String getExtValueServiceName() {
		return extValueServiceName;
	}

	public void setExtValueServiceName(String extValueServiceName) {
		this.extValueServiceName = extValueServiceName;
	}

	public String getExtValueType() {
		return extValueType;
	}

	public void setExtValueType(String extValueType) {
		this.extValueType = extValueType;
	}

	public String getExtValueReturn() {
		return extValueReturn;
	}

	public void setExtValueReturn(String extValueReturn) {
		this.extValueReturn = extValueReturn;
	}

	public String getSearchServiceName() {
		return searchServiceName;
	}

	public void setSearchServiceName(String serviceName) {
		this.searchServiceName = serviceName;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getSearchReturn() {
		return searchReturn;
	}

	public void setSearchReturn(String searchReturn) {
		this.searchReturn = searchReturn;
	}

	public Boolean getFavorisable() {
		return favorisable;
	}

	public void setFavorisable(Boolean favorisable) {
		this.favorisable = favorisable;
	}

	public Boolean getEditable() {
		return editable;
	}

	public void setEditable(Boolean editable) {
		this.editable = editable;
	}
}
