package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.service.interfaces.workflow.ClassWorkflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "form")
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Form {

	@Id
    @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "hibernate_sequence"
	)
	@SequenceGenerator(
		name = "hibernate_sequence",
		allocationSize = 1
	)
    private Long id;

	private String title;

	private String name;

	@Size(max = 500)
	private String description;

	@Column(columnDefinition = "TEXT")
	private String message;

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

	private Boolean hideButton = false;

	private Boolean pdfDisplay = true;

	private Boolean activeVersion = false;

	private Boolean deleted = false;

	@Transient
	private String messageToDisplay;

    @Transient
    private ClassWorkflow modelClassWorkflow;

    @JsonIgnore
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Document document = new Document();

	@ManyToMany
	@OrderColumn
	private List<Field> fields = new ArrayList<>();

	@Column(columnDefinition = "TEXT")
	private String action;

	private Boolean isFeatured = false;

	@Transient
	private Integer totalPageCount = 1;

    @Transient
    public List<Tag> tags = new ArrayList<>();

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

	public Boolean getHideButton() {
		if(hideButton == null) return false;
		return hideButton;
	}

	public void setHideButton(Boolean hideButton) {
		this.hideButton = hideButton;
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

    public List<Tag> getTags() {
        if(this.workflow != null) {
            return workflow.getTags();
        }
        return new ArrayList<>();
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

	public Boolean getIsFeatured() {
		if(isFeatured == null) return false;
		return isFeatured;
	}

	public void setIsFeatured(Boolean featured) {
		isFeatured = featured;
	}

	public void setModelClassWorkflow(ClassWorkflow modelClassWorkflow) {
        this.modelClassWorkflow = modelClassWorkflow;
    }

    public List<SignRequestParams> getSignRequestParams() {
        return this.workflow
                .getWorkflowSteps()
                .stream()
                .flatMap(ws -> ws.getSignRequestParams().stream())
                .toList();
    }
}
