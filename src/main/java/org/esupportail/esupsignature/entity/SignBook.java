package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignBook {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
    
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getVersion() {
        return this.version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
	
	@Column(unique=true)
	private String name;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;
    
    private Boolean external = false;
    
    @Size(max = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ElementCollection(targetClass=String.class)
    private List<String> recipientEmails = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    @OrderColumn
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    private Integer currentWorkflowStepNumber = 1;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

    private String documentsTargetUri;

    private String exportedDocumentURI;

	@Enumerated(EnumType.STRING)
	private SignBookType signBookType;
	
	public enum SignBookType {
		system, group, workflow;
	}

	@OneToMany
    @OrderColumn
    private List<SignRequest> signRequests = new ArrayList<>();

    @JsonIgnore
    @Transient
    transient String comment;

    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public void setSignBookType(SignBookType signBookType) {
        this.signBookType = signBookType;
    }
        
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Date getCreateDate() {
        return this.createDate;
    }
    
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public String getCreateBy() {
        return this.createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public Date getUpdateDate() {
        return this.updateDate;
    }
    
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    public String getUpdateBy() {
        return this.updateBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getRecipientEmails() {
        return this.recipientEmails;
    }

    public SignBookType getSignBookType() {
        return this.signBookType;
    }

    public String getExportedDocumentURI() {
        return exportedDocumentURI;
    }

    public void setExportedDocumentURI(String exportedDocumentURI) {
        this.exportedDocumentURI = exportedDocumentURI;
    }

    public Boolean isExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}

    public List<SignRequest> getSignRequests() {
        return signRequests;
    }

    public void setSignRequests(List<SignRequest> signRequests) {
        this.signRequests = signRequests;
    }

    public Boolean getExternal() {
        return external;
    }

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public Integer getCurrentWorkflowStepNumber() {
        return currentWorkflowStepNumber;
    }

    public void setCurrentWorkflowStepNumber(Integer currentWorkflowStepNumber) {
        this.currentWorkflowStepNumber = currentWorkflowStepNumber;
    }

    public DocumentIOType getTargetType() {
        return targetType;
    }

    public void setTargetType(DocumentIOType targetType) {
        this.targetType = targetType;
    }

    public String getDocumentsTargetUri() {
        return documentsTargetUri;
    }

    public void setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public SignRequestStatus getStatus() {
        SignRequestStatus signRequestStatus = SignRequestStatus.completed;
        if(currentWorkflowStepNumber <= workflowSteps.size()) {
            signRequestStatus = SignRequestStatus.pending;
        }
        return signRequestStatus;
    }

    public WorkflowStep getCurrentWorkflowStep() {
        if(workflowSteps.size() >= currentWorkflowStepNumber) {
            return workflowSteps.get(currentWorkflowStepNumber - 1);
        } else {
            return new WorkflowStep();
        }
    }
    
}
