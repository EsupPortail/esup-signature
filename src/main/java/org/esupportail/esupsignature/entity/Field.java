package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.FieldType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Field {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Version
    private Integer version;
	
	private String name;
	
	private String label;
	
	private Integer fillOrder;
	
	private Integer topPos;
	
	private Integer leftPos;

	private Integer page;

	private Boolean required = false;

	private Boolean readOnly = false;

	private String stepNumbers = "0";

	private String extValueServiceName;

	private String extValueType;

	private String extValueReturn;

	transient String defaultValue;

	private String searchServiceName;

	private String searchType;

	private String searchReturn;

	@Enumerated(EnumType.STRING)
    private FieldType type;
	
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

	public String getStepNumbers() {
		return stepNumbers;
	}

	public void setStepNumbers(String stepNumbers) {
		this.stepNumbers = stepNumbers;
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

}
