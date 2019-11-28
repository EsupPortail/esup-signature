package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.*;

@Entity
public class SignRequest {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	@Column(unique=true)
	private String name;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @JsonIgnore
    @Transient
    transient String comment;

    @JsonIgnore
    @Transient
    transient User creator;

    private String exportedDocumentURI;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Document> originalDocuments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Document> signedDocuments = new ArrayList<>();
    
    private boolean overloadSignBookParams = false;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.REMOVE})
    private List<SignRequestParams> signRequestParamsList = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @OneToMany
    @OrderColumn
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    private Integer currentWorkflowStepNumber = 1;


    @Enumerated(EnumType.STRING)
    private SignBook.DocumentIOType targetType;

    private String documentsTargetUri;

    public enum SignRequestStatus {
		draft, pending, canceled, checked, signed, refused, deleted, exported, completed;
	}

	public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public int countSignOk() {
    	int nbSign = 0;
		for(WorkflowStep workflowStep : workflowSteps) {
			if(workflowStep.isCompleted()) {
				nbSign++;
			}
		}
		nbSign += signedDocuments.size();
		return nbSign;
    }

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

	public String getName() {
        return this.name;
    }

	public void setName(String name) {
        this.name = name;
    }

	public String getTitle() {
        return this.title;
    }

	public void setTitle(String title) {
        this.title = title;
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

	public String getComment() {
        return this.comment;
    }

	public void setComment(String comment) {
        this.comment = comment;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public SignRequestStatus getStatus() {
        return status;
    }

    public List<Document> getOriginalDocuments() {
        return this.originalDocuments;
    }

	public void setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }

	public List<Document> getSignedDocuments() {
        return this.signedDocuments;
    }

	public void setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }

	public boolean isOverloadSignBookParams() {
        return this.overloadSignBookParams;
    }

	public void setOverloadSignBookParams(boolean overloadSignBookParams) {
        this.overloadSignBookParams = overloadSignBookParams;
    }

	public List<SignRequestParams> getSignRequestParamsList() {
        return this.signRequestParamsList;
    }

	public void setSignRequestParamsList(List<SignRequestParams> signRequestParams) {
        this.signRequestParamsList = signRequestParams;
    }

    public Integer getCurrentWorkflowStepNumber() {
        return this.currentWorkflowStepNumber;
    }

    public void setCurrentWorkflowStepNumber(Integer signBooksWorkflowStep) {
        this.currentWorkflowStepNumber = signBooksWorkflowStep;
    }

	public String getExportedDocumentURI() {
		return exportedDocumentURI;
	}

	public void setExportedDocumentURI(String exportedDocumentURI) {
		this.exportedDocumentURI = exportedDocumentURI;
	}

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public WorkflowStep getCurrentWorkflowStep() {
        if(workflowSteps.size() >= currentWorkflowStepNumber) {
            return workflowSteps.get(currentWorkflowStepNumber - 1);
        } else {
            return new WorkflowStep();
        }
    }

    public SignBook.DocumentIOType getTargetType() {
        return targetType;
    }

    public void setTargetType(SignBook.DocumentIOType targetType) {
        this.targetType = targetType;
    }

    public String getDocumentsTargetUri() {
        return documentsTargetUri;
    }

    public void setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }
}
