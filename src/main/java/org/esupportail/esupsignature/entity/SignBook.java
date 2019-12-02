package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private DocumentIOType sourceType;
    
    private String documentsSourceUri;
    
    @ElementCollection(targetClass=String.class)
    private List<String> moderatorEmails = new ArrayList<>();

    @ElementCollection(targetClass=String.class)
    private List<String> recipientEmails = new ArrayList<>();

    @OneToMany
    @OrderColumn
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;
    
    private String documentsTargetUri;    

    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL }, orphanRemoval = true)
    private Document modelFile = new Document();

	@Enumerated(EnumType.STRING)
	private SignBookType signBookType;
	
	public enum SignBookType {
		system, user, group, workflow;
	}
	
    public enum DocumentIOType {
		none, smb, vfs, cmis, mail;
	}

    public void setSourceType(DocumentIOType sourceType) {
        this.sourceType = sourceType;
    }
    
    public void setTargetType(DocumentIOType targetType) {
        this.targetType = targetType;
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
    
    public DocumentIOType getSourceType() {
        return this.sourceType;
    }
    
    public String getDocumentsSourceUri() {
        return this.documentsSourceUri;
    }
    
    public void setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }
    
    public List<String> getModeratorEmails() {
        return this.moderatorEmails;
    }
    
    public void setModeratorEmails(List<String> moderatorEmails) {
        this.moderatorEmails = moderatorEmails;
    }
    
    public List<String> getRecipientEmails() {
        return this.recipientEmails;
    }
    
    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public DocumentIOType getTargetType() {
        return this.targetType;
    }
    
    public String getDocumentsTargetUri() {
        return this.documentsTargetUri;
    }
    
    public void setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }
    
    public Document getModelFile() {
        return this.modelFile;
    }
    
    public void setModelFile(Document modelFile) {
        this.modelFile = modelFile;
    }

    /*
    public SignRequestParams getSignRequestParams() {
        return this.signRequestParams;
    }
    
    public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
    */

    public SignBookType getSignBookType() {
        return this.signBookType;
    }

	public Boolean isExternal() {
		return external;
	}

	public void setExternal(Boolean external) {
		this.external = external;
	}
    
}
