package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;

	private String name;

    private String title;

    private String description;

    private Integer counter;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @ManyToOne
    private User createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();

    private String managerRole;

    @ElementCollection(targetClass =  ShareType.class)
    private List<ShareType> authorizedShareTypes = new ArrayList<>();

    private Boolean publicUsage = false;

    private Boolean scanPdfMetadatas = false;

    private Boolean sendAlertToAllRecipients = false;

    private String documentsSourceUri;
    
    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    private Set<String> managers = new HashSet<>();

    @OrderColumn
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE, CascadeType.DETACH})
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Target> targets = new ArrayList<>();

    @ManyToMany
    private List<User> viewers = new ArrayList<>();

    private Boolean fromCode;

    private Boolean visibility = false;

    private String namingTemplate;

    private String targetNamingTemplate;

    private Boolean ownerSystem = false;

    private Boolean sealAtEnd = false;

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

    public Integer getCounter() {
        return counter;
    }

    public void setCounter(Integer counter) {
        this.counter = counter;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public User getCreateBy() {
        return createBy;
    }

    public void setCreateBy(User createBy) {
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

    public Boolean getPublicUsage() {
        return publicUsage;
    }

    public void setPublicUsage(Boolean publicUsage) {
        this.publicUsage = publicUsage;
    }

    public String getDocumentsSourceUri() {
        return documentsSourceUri;
    }

    public String getProtectedDocumentsSourceUri() {
        if(documentsSourceUri != null) {
            Pattern p = Pattern.compile("[^@]*:\\/\\/[^:]*:([^@]*)@.*?$");
            Matcher m = p.matcher(documentsSourceUri);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, m.group(0).replaceFirst(Pattern.quote(m.group(1)), "********"));
            }
            m.appendTail(sb);
            return sb.toString();
        }
        return "";
    }

    public void setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public Set<String> getManagers() {
        return managers;
    }

    public void setManagers(Set<String> managers) {
        this.managers = managers;
    }

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getManagerRole() {
        return managerRole;
    }

    public void setManagerRole(String managerRole) {
        this.managerRole = managerRole;
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

    public void setScanPdfMetadatas(Boolean scanPdfMetadatas) {
        this.scanPdfMetadatas = scanPdfMetadatas;
    }

    public Boolean getSendAlertToAllRecipients() {
        return sendAlertToAllRecipients;
    }

    public void setSendAlertToAllRecipients(Boolean sendAlertToAllRecipients) {
        this.sendAlertToAllRecipients = sendAlertToAllRecipients;
    }

    public Boolean getFromCode() {
        return fromCode;
    }

    public void setFromCode(Boolean fromCode) {
        this.fromCode = fromCode;
    }

    public Boolean getVisibility() {
        if (this.visibility == null) {
            return false;
        }
        return visibility;
    }

    public void setVisibility(Boolean hidden) {
        this.visibility = hidden;
    }

    public String getNamingTemplate() {
        return namingTemplate;
    }

    public void setNamingTemplate(String namingTemplate) {
        this.namingTemplate = namingTemplate;
    }

    public String getTargetNamingTemplate() {
        return targetNamingTemplate;
    }

    public void setTargetNamingTemplate(String targetNamingTemplate) {
        this.targetNamingTemplate = targetNamingTemplate;
    }

    public Boolean getOwnerSystem() {
        return ownerSystem;
    }

    public void setOwnerSystem(Boolean ownerSystem) {
        this.ownerSystem = ownerSystem;
    }

    public List<User> getViewers() {
        return viewers;
    }

    public void setViewers(List<User> viewers) {
        this.viewers = viewers;
    }

    public Boolean getSealAtEnd() {
        return sealAtEnd;
    }

    public void setSealAtEnd(Boolean sealAtEnd) {
        this.sealAtEnd = sealAtEnd;
    }
}
