package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @Column(unique=true)
    private String name;

    private String title;

    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;

    private String role;

    @ElementCollection(targetClass= ShareType.class)
    private List<ShareType> authorizedShareTypes = new ArrayList<>();

    private Boolean external = false;

    private Boolean publicUsage = false;

    private Boolean scanPdfMetadatas = false;

    @Enumerated(EnumType.STRING)
    private DocumentIOType sourceType;

    private String documentsSourceUri;

    @ElementCollection(targetClass=String.class)
    private List<String> managers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<LiveWorkflowStep> workflowSteps = new ArrayList<LiveWorkflowStep>();

    @OneToOne(cascade = CascadeType.REMOVE)
    private LiveWorkflowStep currentStep;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

    private String documentsTargetUri;

    @ManyToOne()
    private Workflow workflow;

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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Boolean getExternal() {
        return external;
    }

    public void setExternal(Boolean external) {
        this.external = external;
    }

    public Boolean getPublicUsage() {
        return publicUsage;
    }

    public void setPublicUsage(Boolean publicUsage) {
        this.publicUsage = publicUsage;
    }

    public DocumentIOType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DocumentIOType sourceType) {
        this.sourceType = sourceType;
    }

    public String getDocumentsSourceUri() {
        return documentsSourceUri;
    }

    public void setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }

    public List<String> getManagers() {
        return managers;
    }

    public void setManagers(List<String> managers) {
        this.managers = managers;
    }

    public List<LiveWorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<LiveWorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public LiveWorkflowStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(LiveWorkflowStep currentStep) {
        this.currentStep = currentStep;
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

    public Boolean getScanPdfMetadatas() {
        return scanPdfMetadatas;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public void setScanPdfMetadatas(Boolean scanPdfMetadatas) {
        this.scanPdfMetadatas = scanPdfMetadatas;
    }

    public Integer getCurrentStepNumber() {
        if (this.getWorkflowSteps().isEmpty()) {
            return -1;
        }
        if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getAllSignToComplete()) {
            if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getRecipients().stream().allMatch(Recipient::getSigned)) {
                return this.workflowSteps.indexOf(this.getCurrentStep()) + 2;
            }
        } else {
            if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getRecipients().stream().anyMatch(Recipient::getSigned)) {
                return this.workflowSteps.indexOf(this.getCurrentStep()) + 2;
            }
        }
        return this.workflowSteps.indexOf(this.getCurrentStep()) + 1;
    }
}
