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
	
	private Integer width;
	
	private Integer height;

	private Integer page;

	private Boolean required;

	private String stepNumbers = "0";

	private String extValue;

	private String eppnEditRight;

	transient String defaultValue;
	
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

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
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

	public String getStepNumbers() {
		return stepNumbers;
	}

	public void setStepNumbers(String stepNumbers) {
		this.stepNumbers = stepNumbers;
	}

	public String getExtValue() {
		return extValue;
	}

	public void setExtValue(String extValue) {
		this.extValue = extValue;
	}

	public String getEppnEditRight() {
		return eppnEditRight;
	}

	public void setEppnEditRight(String eppnEditRight) {
		this.eppnEditRight = eppnEditRight;
	}
}
