package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @Column(unique=true)
    private String token;

	private String name;

    private String description;

    @Column(columnDefinition = "TEXT")
    private String message;

    private Boolean disableEmailAlerts = false;

    private String mailFrom;

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

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    private Set<String> dashboardRoles = new HashSet<>();

    @ElementCollection(targetClass =  ShareType.class, fetch = FetchType.EAGER)
    private Set<ShareType> authorizedShareTypes = new HashSet<>();

    private Boolean publicUsage = false;

    private Boolean scanPdfMetadatas = false;

    private Boolean sendAlertToAllRecipients = false;

    private String documentsSourceUri;

    private Boolean forbidDownloadsBeforeEnd = false;

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    private Set<String> managers = new HashSet<>();

    @ManyToMany(mappedBy = "workflows")
    private Set<WsAccessToken> wsAccessTokens = new HashSet<>();

    @OrderColumn
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE, CascadeType.DETACH})
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Target> targets = new ArrayList<>();

    @ManyToMany
    private List<User> viewers = new ArrayList<>();

    private Boolean fromCode;

    private String namingTemplate;

    private String targetNamingTemplate;

    private Boolean ownerSystem = false;

    private Boolean disableDeleteByCreator = false;

    private Boolean sealAtEnd = false;

    private String signRequestParamsDetectionPattern;

    @Transient
    private String messageToDisplay;

    private Boolean externalCanEdit = false;

    private Boolean authorizeClone = false;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date startArchiveDate;

    private String archiveTarget;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getDisableEmailAlerts() {
        return disableEmailAlerts;
    }

    public void setDisableEmailAlerts(Boolean disableEmailAlerts) {
        this.disableEmailAlerts = disableEmailAlerts;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
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

    public List<Target> getTargetsOrdered() {
        return targets.stream().sorted(Comparator.comparing(Target::getId)).toList();
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

    public Set<String> getDashboardRoles() {
        if(dashboardRoles == null) {
            dashboardRoles = new HashSet<>();
        }
        return dashboardRoles;
    }

    public void setDashboardRoles(Set<String> dashboardRoles) {
        this.dashboardRoles = dashboardRoles;
    }

    public Set<ShareType> getAuthorizedShareTypes() {
        return authorizedShareTypes;
    }

    public void setAuthorizedShareTypes(Set<ShareType> authorizedShareTypes) {
        this.authorizedShareTypes = authorizedShareTypes;
    }

    public Boolean getScanPdfMetadatas() {
        return scanPdfMetadatas;
    }

    public void setScanPdfMetadatas(Boolean scanPdfMetadatas) {
        this.scanPdfMetadatas = scanPdfMetadatas;
    }

    public Boolean getSendAlertToAllRecipients() {
        return Objects.requireNonNullElse(sendAlertToAllRecipients, false);
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

    public Boolean getDisableDeleteByCreator() {
        return disableDeleteByCreator;
    }

    public void setDisableDeleteByCreator(Boolean disableDeleteByCreator) {
        this.disableDeleteByCreator = disableDeleteByCreator;
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

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public void setMessageToDisplay(String messageToDisplay) {
        this.messageToDisplay = messageToDisplay;
    }

    public Boolean getForbidDownloadsBeforeEnd() {
        return forbidDownloadsBeforeEnd;
    }

    public void setForbidDownloadsBeforeEnd(Boolean forbidDownloadsBeforeEnd) {
        this.forbidDownloadsBeforeEnd = forbidDownloadsBeforeEnd;
    }

    public String getSignRequestParamsDetectionPattern() {
        return signRequestParamsDetectionPattern;
    }

    public void setSignRequestParamsDetectionPattern(String signRequestParamsDetectionPattern) {
        this.signRequestParamsDetectionPattern = signRequestParamsDetectionPattern;
    }

    public Boolean getExternalCanEdit() {
        if(externalCanEdit == null) {
            return false;
        }
        return externalCanEdit;
    }

    public void setExternalCanEdit(Boolean extrenalCanEdit) {
        this.externalCanEdit = extrenalCanEdit;
    }

    public Boolean getAuthorizeClone() {
        if(authorizeClone == null) {
            return false;
        }
        return authorizeClone;
    }

    public void setAuthorizeClone(Boolean authorizeClone) {
        this.authorizeClone = authorizeClone;
    }

    public Date getStartArchiveDate() {
        return startArchiveDate;
    }

    public void setStartArchiveDate(Date startArchiveDate) {
        this.startArchiveDate = startArchiveDate;
    }

    public String getArchiveTarget() {
        return archiveTarget;
    }

    public void setArchiveTarget(String archiveTarget) {
        this.archiveTarget = archiveTarget;
    }
}
