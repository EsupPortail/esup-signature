package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.DocumentIOType;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class Form {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	private String name;

	@Size(max = 500)
	private String description;

	private Integer version;

	private String manager;
	
	private String workflowType;
	
	private String preFillType;

	private String role;
	
	private Boolean pdfDisplay = true;

	private Integer nbPages = 1;

	private Boolean activeVersion = false;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

    private String targetUri;    
	
	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
    private Document document = new Document();

	@OrderColumn
	@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	private List<Field> fields = new ArrayList<>();

	@ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	private List<Data> datas = new ArrayList<>();

	@Transient
	private Long nbSended;

	@Transient
	private Long nbDraft;

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

	public boolean isPdfDisplay() {
		return pdfDisplay;
	}

	public void setPdfDisplay(boolean pdfDisplay) {
		this.pdfDisplay = pdfDisplay;
	}

	public Integer getNbPages() {
		return nbPages;
	}

	public void setNbPages(Integer nbPages) {
		this.nbPages = nbPages;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getPreFillType() {
		return preFillType;
	}

	public void setPreFillType(String preFillType) {
		this.preFillType = preFillType;
	}

	public Long getNbSended() {
		return nbSended;
	}

	public void setNbSended(Long nbSended) {
		this.nbSended = nbSended;
	}

	public Long getNbDraft() {
		return nbDraft;
	}

	public void setNbDraft(Long nbDraft) {
		this.nbDraft = nbDraft;
	}

	public Boolean getActiveVersion() {
		return activeVersion;
	}

	public void setActiveVersion(Boolean activeVersion) {
		this.activeVersion = activeVersion;
	}
}
