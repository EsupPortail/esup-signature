package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workflow {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;

	@Column(unique=true)
	private String name;

    private String description;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;
    
    private Boolean external = false;

    private Boolean publicUsage = false;

    private Boolean archive = false;

    @Enumerated(EnumType.STRING)
    private DocumentIOType sourceType;
    
    private String documentsSourceUri;
    
    @ElementCollection(targetClass=String.class)
    private List<String> managers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;
    
    private String documentsTargetUri;

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

    public Boolean getArchive() {
        return archive;
    }

    public void setArchive(Boolean archive) {
        this.archive = archive;
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

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
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
}
