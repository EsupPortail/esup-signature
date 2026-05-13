package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.Date;
import java.util.List;

public class AdminWorkflowUpdateViewDto {

    private String workflowRole;
    private WorkflowDto workflow;
    private Long nbWorkflowSignRequests;
    private List<String> roles;
    private List<TagDto> allTags;
    private List<Long> selectedTagIds;

    public String getWorkflowRole() {
        return workflowRole;
    }

    public void setWorkflowRole(String workflowRole) {
        this.workflowRole = workflowRole;
    }

    public WorkflowDto getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowDto workflow) {
        this.workflow = workflow;
    }

    public Long getNbWorkflowSignRequests() {
        return nbWorkflowSignRequests;
    }

    public void setNbWorkflowSignRequests(Long nbWorkflowSignRequests) {
        this.nbWorkflowSignRequests = nbWorkflowSignRequests;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<TagDto> getAllTags() {
        return allTags;
    }

    public void setAllTags(List<TagDto> allTags) {
        this.allTags = allTags;
    }

    public List<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public void setSelectedTagIds(List<Long> selectedTagIds) {
        this.selectedTagIds = selectedTagIds;
    }

    public static class WorkflowDto {
        private Long id;
        private String description;
        private String token;
        private String mailFrom;
        private String namingTemplate;
        private Boolean isFeatured;
        private Boolean publicUsage;
        private List<String> roles;
        private List<String> managers;
        private List<String> dashboardRoles;
        private List<ViewerDto> viewers;
        private List<ExternalAuth> externalAuths;
        private List<ShareType> authorizedShareTypes;
        private Boolean sealAtEnd;
        private Boolean sendAlertToAllRecipients;
        private Boolean ownerSystem;
        private Boolean disableDeleteByCreator;
        private Boolean disableUpdateByCreator;
        private Boolean disableEmailAlerts;
        private Boolean forbidDownloadsBeforeEnd;
        private Boolean authorizeClone;
        private Boolean externalCanReaderAnnotations;
        private Boolean externalCanEdit;
        private Boolean externalCanReaderAttachments;
        private Boolean externalCanEditAttachments;
        private Boolean disableSidebarForExternal;
        private String signRequestParamsDetectionPattern;
        private Boolean scanPdfMetadatas;
        private String documentsSourceUri;
        private Boolean unzip;
        private String targetNamingTemplate;
        private Date startArchiveDate;
        private String archiveTarget;
        private List<TagDto> tags;
        private List<TargetDto> targetsOrdered;
        private List<WorkflowStepDto> workflowSteps;
        private Boolean fromCode;
        private String message;

        public WorkflowDto() {
        }

        public WorkflowDto(Long id,
                           String description,
                           String token,
                           String mailFrom,
                           String namingTemplate,
                           Boolean isFeatured,
                           Boolean publicUsage,
                           List<String> roles,
                           List<String> managers,
                           List<String> dashboardRoles,
                           List<ViewerDto> viewers,
                           List<ExternalAuth> externalAuths,
                           List<ShareType> authorizedShareTypes,
                           Boolean sealAtEnd,
                           Boolean sendAlertToAllRecipients,
                           Boolean ownerSystem,
                           Boolean disableDeleteByCreator,
                           Boolean disableUpdateByCreator,
                           Boolean disableEmailAlerts,
                           Boolean forbidDownloadsBeforeEnd,
                           Boolean authorizeClone,
                           Boolean externalCanReaderAnnotations,
                           Boolean externalCanEdit,
                           Boolean externalCanReaderAttachments,
                           Boolean externalCanEditAttachments,
                           Boolean disableSidebarForExternal,
                           String signRequestParamsDetectionPattern,
                           Boolean scanPdfMetadatas,
                           String documentsSourceUri,
                           Boolean unzip,
                           String targetNamingTemplate,
                           Date startArchiveDate,
                           String archiveTarget,
                           List<TagDto> tags,
                           List<TargetDto> targetsOrdered,
                           List<WorkflowStepDto> workflowSteps,
                           Boolean fromCode,
                           String message) {
            this.id = id;
            this.description = description;
            this.token = token;
            this.mailFrom = mailFrom;
            this.namingTemplate = namingTemplate;
            this.isFeatured = isFeatured;
            this.publicUsage = publicUsage;
            this.roles = roles;
            this.managers = managers;
            this.dashboardRoles = dashboardRoles;
            this.viewers = viewers;
            this.externalAuths = externalAuths;
            this.authorizedShareTypes = authorizedShareTypes;
            this.sealAtEnd = sealAtEnd;
            this.sendAlertToAllRecipients = sendAlertToAllRecipients;
            this.ownerSystem = ownerSystem;
            this.disableDeleteByCreator = disableDeleteByCreator;
            this.disableUpdateByCreator = disableUpdateByCreator;
            this.disableEmailAlerts = disableEmailAlerts;
            this.forbidDownloadsBeforeEnd = forbidDownloadsBeforeEnd;
            this.authorizeClone = authorizeClone;
            this.externalCanReaderAnnotations = externalCanReaderAnnotations;
            this.externalCanEdit = externalCanEdit;
            this.externalCanReaderAttachments = externalCanReaderAttachments;
            this.externalCanEditAttachments = externalCanEditAttachments;
            this.disableSidebarForExternal = disableSidebarForExternal;
            this.signRequestParamsDetectionPattern = signRequestParamsDetectionPattern;
            this.scanPdfMetadatas = scanPdfMetadatas;
            this.documentsSourceUri = documentsSourceUri;
            this.unzip = unzip;
            this.targetNamingTemplate = targetNamingTemplate;
            this.startArchiveDate = startArchiveDate;
            this.archiveTarget = archiveTarget;
            this.tags = tags;
            this.targetsOrdered = targetsOrdered;
            this.workflowSteps = workflowSteps;
            this.fromCode = fromCode;
            this.message = message;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getMailFrom() { return mailFrom; }
        public void setMailFrom(String mailFrom) { this.mailFrom = mailFrom; }
        public String getNamingTemplate() { return namingTemplate; }
        public void setNamingTemplate(String namingTemplate) { this.namingTemplate = namingTemplate; }
        public Boolean getIsFeatured() { return isFeatured; }
        public void setIsFeatured(Boolean featured) { isFeatured = featured; }
        public Boolean getPublicUsage() { return publicUsage; }
        public void setPublicUsage(Boolean publicUsage) { this.publicUsage = publicUsage; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getManagers() { return managers; }
        public void setManagers(List<String> managers) { this.managers = managers; }
        public List<String> getDashboardRoles() { return dashboardRoles; }
        public void setDashboardRoles(List<String> dashboardRoles) { this.dashboardRoles = dashboardRoles; }
        public List<ViewerDto> getViewers() { return viewers; }
        public void setViewers(List<ViewerDto> viewers) { this.viewers = viewers; }
        public List<ExternalAuth> getExternalAuths() { return externalAuths; }
        public void setExternalAuths(List<ExternalAuth> externalAuths) { this.externalAuths = externalAuths; }
        public List<ShareType> getAuthorizedShareTypes() { return authorizedShareTypes; }
        public void setAuthorizedShareTypes(List<ShareType> authorizedShareTypes) { this.authorizedShareTypes = authorizedShareTypes; }
        public Boolean getSealAtEnd() { return sealAtEnd; }
        public void setSealAtEnd(Boolean sealAtEnd) { this.sealAtEnd = sealAtEnd; }
        public Boolean getSendAlertToAllRecipients() { return sendAlertToAllRecipients; }
        public void setSendAlertToAllRecipients(Boolean sendAlertToAllRecipients) { this.sendAlertToAllRecipients = sendAlertToAllRecipients; }
        public Boolean getOwnerSystem() { return ownerSystem; }
        public void setOwnerSystem(Boolean ownerSystem) { this.ownerSystem = ownerSystem; }
        public Boolean getDisableDeleteByCreator() { return disableDeleteByCreator; }
        public void setDisableDeleteByCreator(Boolean disableDeleteByCreator) { this.disableDeleteByCreator = disableDeleteByCreator; }
        public Boolean getDisableUpdateByCreator() { return disableUpdateByCreator; }
        public void setDisableUpdateByCreator(Boolean disableUpdateByCreator) { this.disableUpdateByCreator = disableUpdateByCreator; }
        public Boolean getDisableEmailAlerts() { return disableEmailAlerts; }
        public void setDisableEmailAlerts(Boolean disableEmailAlerts) { this.disableEmailAlerts = disableEmailAlerts; }
        public Boolean getForbidDownloadsBeforeEnd() { return forbidDownloadsBeforeEnd; }
        public void setForbidDownloadsBeforeEnd(Boolean forbidDownloadsBeforeEnd) { this.forbidDownloadsBeforeEnd = forbidDownloadsBeforeEnd; }
        public Boolean getAuthorizeClone() { return authorizeClone; }
        public void setAuthorizeClone(Boolean authorizeClone) { this.authorizeClone = authorizeClone; }
        public Boolean getExternalCanReaderAnnotations() { return externalCanReaderAnnotations; }
        public void setExternalCanReaderAnnotations(Boolean externalCanReaderAnnotations) { this.externalCanReaderAnnotations = externalCanReaderAnnotations; }
        public Boolean getExternalCanEdit() { return externalCanEdit; }
        public void setExternalCanEdit(Boolean externalCanEdit) { this.externalCanEdit = externalCanEdit; }
        public Boolean getExternalCanReaderAttachments() { return externalCanReaderAttachments; }
        public void setExternalCanReaderAttachments(Boolean externalCanReaderAttachments) { this.externalCanReaderAttachments = externalCanReaderAttachments; }
        public Boolean getExternalCanEditAttachments() { return externalCanEditAttachments; }
        public void setExternalCanEditAttachments(Boolean externalCanEditAttachments) { this.externalCanEditAttachments = externalCanEditAttachments; }
        public Boolean getDisableSidebarForExternal() { return disableSidebarForExternal; }
        public void setDisableSidebarForExternal(Boolean disableSidebarForExternal) { this.disableSidebarForExternal = disableSidebarForExternal; }
        public String getSignRequestParamsDetectionPattern() { return signRequestParamsDetectionPattern; }
        public void setSignRequestParamsDetectionPattern(String signRequestParamsDetectionPattern) { this.signRequestParamsDetectionPattern = signRequestParamsDetectionPattern; }
        public Boolean getScanPdfMetadatas() { return scanPdfMetadatas; }
        public void setScanPdfMetadatas(Boolean scanPdfMetadatas) { this.scanPdfMetadatas = scanPdfMetadatas; }
        public String getDocumentsSourceUri() { return documentsSourceUri; }
        public void setDocumentsSourceUri(String documentsSourceUri) { this.documentsSourceUri = documentsSourceUri; }
        public Boolean getUnzip() { return unzip; }
        public void setUnzip(Boolean unzip) { this.unzip = unzip; }
        public String getTargetNamingTemplate() { return targetNamingTemplate; }
        public void setTargetNamingTemplate(String targetNamingTemplate) { this.targetNamingTemplate = targetNamingTemplate; }
        public Date getStartArchiveDate() { return startArchiveDate; }
        public void setStartArchiveDate(Date startArchiveDate) { this.startArchiveDate = startArchiveDate; }
        public String getArchiveTarget() { return archiveTarget; }
        public void setArchiveTarget(String archiveTarget) { this.archiveTarget = archiveTarget; }
        public List<TagDto> getTags() { return tags; }
        public void setTags(List<TagDto> tags) { this.tags = tags; }
        public List<TargetDto> getTargetsOrdered() { return targetsOrdered; }
        public void setTargetsOrdered(List<TargetDto> targetsOrdered) { this.targetsOrdered = targetsOrdered; }
        public List<WorkflowStepDto> getWorkflowSteps() { return workflowSteps; }
        public void setWorkflowSteps(List<WorkflowStepDto> workflowSteps) { this.workflowSteps = workflowSteps; }
        public Boolean getFromCode() { return fromCode; }
        public void setFromCode(Boolean fromCode) { this.fromCode = fromCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class TargetDto {
        private Long id;
        private String protectedTargetUri;
        private Boolean sendDocument;
        private Boolean sendReport;
        private Boolean sendAttachment;
        private Boolean sendZip;

        public TargetDto() {
        }

        public TargetDto(Long id,
                         String protectedTargetUri,
                         Boolean sendDocument,
                         Boolean sendReport,
                         Boolean sendAttachment,
                         Boolean sendZip) {
            this.id = id;
            this.protectedTargetUri = protectedTargetUri;
            this.sendDocument = sendDocument;
            this.sendReport = sendReport;
            this.sendAttachment = sendAttachment;
            this.sendZip = sendZip;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProtectedTargetUri() { return protectedTargetUri; }
        public void setProtectedTargetUri(String protectedTargetUri) { this.protectedTargetUri = protectedTargetUri; }
        public Boolean getSendDocument() { return sendDocument; }
        public void setSendDocument(Boolean sendDocument) { this.sendDocument = sendDocument; }
        public Boolean getSendReport() { return sendReport; }
        public void setSendReport(Boolean sendReport) { this.sendReport = sendReport; }
        public Boolean getSendAttachment() { return sendAttachment; }
        public void setSendAttachment(Boolean sendAttachment) { this.sendAttachment = sendAttachment; }
        public Boolean getSendZip() { return sendZip; }
        public void setSendZip(Boolean sendZip) { this.sendZip = sendZip; }
    }

    public static class WorkflowStepDto {
        private Long id;
        private String description;
        private Boolean autoSign;
        private SignType signType;
        private SignLevel minSignLevel;
        private SignLevel maxSignLevel;
        private Boolean sealVisa;
        private Integer maxRecipients;
        private Boolean changeable;
        private Boolean repeatable;
        private Boolean multiSign;
        private Boolean singleSignWithAnnotation;
        private Boolean allSignToComplete;
        private Boolean attachmentAlert;
        private Boolean attachmentRequire;
        private List<UserDto> users;
        private CertificatDto certificat;

        public WorkflowStepDto() {
        }

        public WorkflowStepDto(Long id,
                               String description,
                               Boolean autoSign,
                               SignType signType,
                               SignLevel minSignLevel,
                               SignLevel maxSignLevel,
                               Boolean sealVisa,
                               Integer maxRecipients,
                               Boolean changeable,
                               Boolean repeatable,
                               Boolean multiSign,
                               Boolean singleSignWithAnnotation,
                               Boolean allSignToComplete,
                               Boolean attachmentAlert,
                               Boolean attachmentRequire,
                               List<UserDto> users,
                               CertificatDto certificat) {
            this.id = id;
            this.description = description;
            this.autoSign = autoSign;
            this.signType = signType;
            this.minSignLevel = minSignLevel;
            this.maxSignLevel = maxSignLevel;
            this.sealVisa = sealVisa;
            this.maxRecipients = maxRecipients;
            this.changeable = changeable;
            this.repeatable = repeatable;
            this.multiSign = multiSign;
            this.singleSignWithAnnotation = singleSignWithAnnotation;
            this.allSignToComplete = allSignToComplete;
            this.attachmentAlert = attachmentAlert;
            this.attachmentRequire = attachmentRequire;
            this.users = users;
            this.certificat = certificat;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getAutoSign() { return autoSign; }
        public void setAutoSign(Boolean autoSign) { this.autoSign = autoSign; }
        public SignType getSignType() { return signType; }
        public void setSignType(SignType signType) { this.signType = signType; }
        public SignLevel getMinSignLevel() { return minSignLevel; }
        public void setMinSignLevel(SignLevel minSignLevel) { this.minSignLevel = minSignLevel; }
        public SignLevel getMaxSignLevel() { return maxSignLevel; }
        public void setMaxSignLevel(SignLevel maxSignLevel) { this.maxSignLevel = maxSignLevel; }
        public Boolean getSealVisa() { return sealVisa; }
        public void setSealVisa(Boolean sealVisa) { this.sealVisa = sealVisa; }
        public Integer getMaxRecipients() { return maxRecipients; }
        public void setMaxRecipients(Integer maxRecipients) { this.maxRecipients = maxRecipients; }
        public Boolean getChangeable() { return changeable; }
        public void setChangeable(Boolean changeable) { this.changeable = changeable; }
        public Boolean getRepeatable() { return repeatable; }
        public void setRepeatable(Boolean repeatable) { this.repeatable = repeatable; }
        public Boolean getMultiSign() { return multiSign; }
        public void setMultiSign(Boolean multiSign) { this.multiSign = multiSign; }
        public Boolean getSingleSignWithAnnotation() { return singleSignWithAnnotation; }
        public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) { this.singleSignWithAnnotation = singleSignWithAnnotation; }
        public Boolean getAllSignToComplete() { return allSignToComplete; }
        public void setAllSignToComplete(Boolean allSignToComplete) { this.allSignToComplete = allSignToComplete; }
        public Boolean getAttachmentAlert() { return attachmentAlert; }
        public void setAttachmentAlert(Boolean attachmentAlert) { this.attachmentAlert = attachmentAlert; }
        public Boolean getAttachmentRequire() { return attachmentRequire; }
        public void setAttachmentRequire(Boolean attachmentRequire) { this.attachmentRequire = attachmentRequire; }
        public List<UserDto> getUsers() { return users; }
        public void setUsers(List<UserDto> users) { this.users = users; }
        public CertificatDto getCertificat() { return certificat; }
        public void setCertificat(CertificatDto certificat) { this.certificat = certificat; }
    }

    public static class CertificatDto {
        private Long id;

        public CertificatDto() {
        }

        public CertificatDto(Long id) {
            this.id = id;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }

    public static class ViewerDto {
        private String email;
        private String firstname;
        private String name;

        public ViewerDto() {
        }

        public ViewerDto(String email, String firstname, String name) {
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class TagDto {
        private Long id;
        private String name;
        private String color;

        public TagDto() {
        }

        public TagDto(Long id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class UserDto {
        private String eppn;
        private String email;
        private String firstname;
        private String name;

        public UserDto() {
        }

        public UserDto(String eppn, String email, String firstname, String name) {
            this.eppn = eppn;
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
