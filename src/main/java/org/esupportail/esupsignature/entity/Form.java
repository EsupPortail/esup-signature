package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form")
public class Form {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Column(unique=true)
	private String title;

	private String name;

	@Size(max = 500)
	private String description;

	private Integer version;

	private String manager;
	
	private String workflowType;
	
	private String preFillType;

	private String role;

	@ElementCollection(targetClass= ShareType.class)
	private List<ShareType> authorizedShareTypes = new ArrayList<>();

	private Boolean publicUsage = false;

	private Boolean pdfDisplay = true;

	private Boolean needName = false;

	private Boolean activeVersion = false;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

    private String targetUri;    
	
	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private Document document = new Document();

	@OrderColumn
	@OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
	private List<Field> fields = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public String getManager() {
		return manager;
	}

	public void setManager(String manager) {
		this.manager = manager;
	}

	public String getWorkflowType() {
		return workflowType;
	}

	public void setWorkflowType(String workflowType) {
		this.workflowType = workflowType;
	}

	public String getPreFillType() {
		return preFillType;
	}

	public void setPreFillType(String preFillType) {
		this.preFillType = preFillType;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public List<ShareType> getAuthorizedShareTypes() {
		return authorizedShareTypes;
	}

	public void setAuthorizedShareTypes(List<ShareType> authorizedShareTypes) {
		this.authorizedShareTypes = authorizedShareTypes;
	}

	public Boolean getPublicUsage() {
		return publicUsage;
	}

	public void setPublicUsage(Boolean publicUsage) {
		this.publicUsage = publicUsage;
	}

	public Boolean getPdfDisplay() {
		return pdfDisplay;
	}

	public void setPdfDisplay(Boolean pdfDisplay) {
		this.pdfDisplay = pdfDisplay;
	}

	public Boolean getActiveVersion() {
		return activeVersion;
	}

	public void setActiveVersion(Boolean activeVersion) {
		this.activeVersion = activeVersion;
	}

	public DocumentIOType getTargetType() {
		return targetType;
	}

	public void setTargetType(DocumentIOType targetType) {
		this.targetType = targetType;
	}

	public String getTargetUri() {
		return targetUri;
	}

	public void setTargetUri(String targetUri) {
		this.targetUri = targetUri;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

	public Boolean getNeedName() {
		return needName;
	}

	public void setNeedName(Boolean needName) {
		this.needName = needName;
	}
}
