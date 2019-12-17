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
	private String token;
	
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

    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;

    @ManyToOne
    private SignBook parentSignBook;

    @OneToMany
    @OrderColumn
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    private Integer currentWorkflowStepNumber = 1;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

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

	public String getToken() {
        return this.token;
    }

	public void setToken(String token) {
        this.token = token;
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

    public SignBook getParentSignBook() {
        return parentSignBook;
    }

    public void setParentSignBook(SignBook parentSignBook) {
        this.parentSignBook = parentSignBook;
    }
}
