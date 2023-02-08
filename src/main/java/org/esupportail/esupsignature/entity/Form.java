package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.ShareType;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "form")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Form {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	private String title;

	private String name;

	@Size(max = 500)
	private String description;

	@Column(columnDefinition = "TEXT")
	private String message;

	private Integer version;

	@Deprecated
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	private Set<String> managers = new HashSet<>();

	private String managerRole;

	@OneToOne
	private Workflow workflow;
	
	private String preFillType;

	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	private Set<String> roles = new HashSet<>();

	@ElementCollection(targetClass =  ShareType.class, fetch = FetchType.EAGER)
	private Set<ShareType> authorizedShareTypes = new HashSet<>();

	private Boolean publicUsage = false;

	private Boolean pdfDisplay = true;

	private Boolean activeVersion = false;

	private Boolean deleted = false;

	@Transient
	private String messageToDisplay;

    @JsonIgnore
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Document document = new Document();

	@ManyToMany(cascade = CascadeType.REMOVE)
	@OrderColumn
	private List<Field> fields = new ArrayList<>();

	@OneToMany(cascade = CascadeType.DETACH)
	@OrderColumn
	private List<SignRequestParams> signRequestParams = new ArrayList<>();

	@Column(columnDefinition = "TEXT")
	private String action;

	@Transient
	private Integer totalPageCount = 1;

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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Deprecated
	public Set<String> getManagers() {
		return managers;
	}

	@Deprecated
	public void setManagers(Set<String> managers) {
		this.managers = managers;
	}

	public String getManagerRole() {
		return managerRole;
	}

	public void setManagerRole(String managerRole) {
		this.managerRole = managerRole;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public void setWorkflow(Workflow workflow) {
		this.workflow = workflow;
	}

	public String getPreFillType() {
		return preFillType;
	}

	public void setPreFillType(String preFillType) {
		this.preFillType = preFillType;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public Set<ShareType> getAuthorizedShareTypes() {
		return authorizedShareTypes;
	}

	public void setAuthorizedShareTypes(Set<ShareType> authorizedShareTypes) {
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

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
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

	public List<SignRequestParams> getSignRequestParams() {
		return signRequestParams;
	}

	public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
		this.signRequestParams = signRequestParams;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMessageToDisplay() {
		return messageToDisplay;
	}

	public void setMessageToDisplay(String messageToDisplay) {
		this.messageToDisplay = messageToDisplay;
	}

	public Integer getTotalPageCount() {
		return totalPageCount;
	}

	public void setTotalPageCount(Integer totalPageCount) {
		this.totalPageCount = totalPageCount;
	}
}
