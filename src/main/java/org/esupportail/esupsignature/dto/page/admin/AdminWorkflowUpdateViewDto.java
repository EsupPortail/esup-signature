package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.ExternalAuth;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.Date;
import java.util.List;

public class AdminWorkflowUpdateViewDto {

    private final String workflowRole;
    private final WorkflowDto workflow;
    private final Long nbWorkflowSignRequests;
    private final List<String> roles;
    private final List<TagDto> allTags;
    private final List<Long> selectedTagIds;

    public AdminWorkflowUpdateViewDto(String workflowRole,
                                      WorkflowDto workflow,
                                      Long nbWorkflowSignRequests,
                                      List<String> roles,
                                      List<TagDto> allTags,
                                      List<Long> selectedTagIds) {
        this.workflowRole = workflowRole;
        this.workflow = workflow;
        this.nbWorkflowSignRequests = nbWorkflowSignRequests;
        this.roles = roles;
        this.allTags = allTags;
        this.selectedTagIds = selectedTagIds;
    }

    public String getWorkflowRole() {
        return workflowRole;
    }

    public WorkflowDto getWorkflow() {
        return workflow;
    }

    public Long getNbWorkflowSignRequests() {
        return nbWorkflowSignRequests;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<TagDto> getAllTags() {
        return allTags;
    }

    public List<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public static class WorkflowDto {
        private final Long id;
        private final String description;
        private final String token;
        private final String mailFrom;
        private final String namingTemplate;
        private final Boolean isFeatured;
        private final Boolean publicUsage;
        private final List<String> roles;
        private final List<String> managers;
        private final List<String> dashboardRoles;
        private final List<ViewerDto> viewers;
        private final List<ExternalAuth> externalAuths;
        private final List<ShareType> authorizedShareTypes;
        private final Boolean sealAtEnd;
        private final Boolean sendAlertToAllRecipients;
        private final Boolean ownerSystem;
        private final Boolean disableDeleteByCreator;
        private final Boolean disableEmailAlerts;
        private final Boolean forbidDownloadsBeforeEnd;
        private final Boolean authorizeClone;
        private final Boolean externalCanReaderAnnotations;
        private final Boolean externalCanEdit;
        private final Boolean externalCanReaderAttachments;
        private final Boolean externalCanEditAttachments;
        private final Boolean disableSidebarForExternal;
        private final String signRequestParamsDetectionPattern;
        private final Boolean scanPdfMetadatas;
        private final String documentsSourceUri;
        private final String targetNamingTemplate;
        private final Date startArchiveDate;
        private final String archiveTarget;
        private final List<TagDto> tags;
        private final List<TargetDto> targetsOrdered;
        private final List<WorkflowStepDto> workflowSteps;
        private final Boolean fromCode;
        private final String message;

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
            this.targetNamingTemplate = targetNamingTemplate;
            this.startArchiveDate = startArchiveDate;
            this.archiveTarget = archiveTarget;
            this.tags = tags;
            this.targetsOrdered = targetsOrdered;
            this.workflowSteps = workflowSteps;
            this.fromCode = fromCode;
            this.message = message;
        }

        public Long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getToken() {
            return token;
        }

        public String getMailFrom() {
            return mailFrom;
        }

        public String getNamingTemplate() {
            return namingTemplate;
        }

        public Boolean getIsFeatured() {
            return isFeatured;
        }

        public Boolean getPublicUsage() {
            return publicUsage;
        }

        public List<String> getRoles() {
            return roles;
        }

        public List<String> getManagers() {
            return managers;
        }

        public List<String> getDashboardRoles() {
            return dashboardRoles;
        }

        public List<ViewerDto> getViewers() {
            return viewers;
        }

        public List<ExternalAuth> getExternalAuths() {
            return externalAuths;
        }

        public List<ShareType> getAuthorizedShareTypes() {
            return authorizedShareTypes;
        }

        public Boolean getSealAtEnd() {
            return sealAtEnd;
        }

        public Boolean getSendAlertToAllRecipients() {
            return sendAlertToAllRecipients;
        }

        public Boolean getOwnerSystem() {
            return ownerSystem;
        }

        public Boolean getDisableDeleteByCreator() {
            return disableDeleteByCreator;
        }

        public Boolean getDisableEmailAlerts() {
            return disableEmailAlerts;
        }

        public Boolean getForbidDownloadsBeforeEnd() {
            return forbidDownloadsBeforeEnd;
        }

        public Boolean getAuthorizeClone() {
            return authorizeClone;
        }

        public Boolean getExternalCanReaderAnnotations() {
            return externalCanReaderAnnotations;
        }

        public Boolean getExternalCanEdit() {
            return externalCanEdit;
        }

        public Boolean getExternalCanReaderAttachments() {
            return externalCanReaderAttachments;
        }

        public Boolean getExternalCanEditAttachments() {
            return externalCanEditAttachments;
        }

        public Boolean getDisableSidebarForExternal() {
            return disableSidebarForExternal;
        }

        public String getSignRequestParamsDetectionPattern() {
            return signRequestParamsDetectionPattern;
        }

        public Boolean getScanPdfMetadatas() {
            return scanPdfMetadatas;
        }

        public String getDocumentsSourceUri() {
            return documentsSourceUri;
        }

        public String getTargetNamingTemplate() {
            return targetNamingTemplate;
        }

        public Date getStartArchiveDate() {
            return startArchiveDate;
        }

        public String getArchiveTarget() {
            return archiveTarget;
        }

        public List<TagDto> getTags() {
            return tags;
        }

        public List<TargetDto> getTargetsOrdered() {
            return targetsOrdered;
        }

        public List<WorkflowStepDto> getWorkflowSteps() {
            return workflowSteps;
        }

        public Boolean getFromCode() {
            return fromCode;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class TargetDto {
        private final Long id;
        private final String protectedTargetUri;
        private final Boolean sendDocument;
        private final Boolean sendReport;
        private final Boolean sendAttachment;
        private final Boolean sendZip;

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

        public Long getId() {
            return id;
        }

        public String getProtectedTargetUri() {
            return protectedTargetUri;
        }

        public Boolean getSendDocument() {
            return sendDocument;
        }

        public Boolean getSendReport() {
            return sendReport;
        }

        public Boolean getSendAttachment() {
            return sendAttachment;
        }

        public Boolean getSendZip() {
            return sendZip;
        }
    }

    public static class WorkflowStepDto {
        private final Long id;
        private final String description;
        private final Boolean autoSign;
        private final SignType signType;
        private final SignLevel minSignLevel;
        private final SignLevel maxSignLevel;
        private final Boolean sealVisa;
        private final Integer maxRecipients;
        private final Boolean changeable;
        private final Boolean repeatable;
        private final Boolean multiSign;
        private final Boolean singleSignWithAnnotation;
        private final Boolean allSignToComplete;
        private final Boolean attachmentAlert;
        private final Boolean attachmentRequire;
        private final List<UserDto> users;
        private final CertificatDto certificat;

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

        public Long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public Boolean getAutoSign() {
            return autoSign;
        }

        public SignType getSignType() {
            return signType;
        }

        public SignLevel getMinSignLevel() {
            return minSignLevel;
        }

        public SignLevel getMaxSignLevel() {
            return maxSignLevel;
        }

        public Boolean getSealVisa() {
            return sealVisa;
        }

        public Integer getMaxRecipients() {
            return maxRecipients;
        }

        public Boolean getChangeable() {
            return changeable;
        }

        public Boolean getRepeatable() {
            return repeatable;
        }

        public Boolean getMultiSign() {
            return multiSign;
        }

        public Boolean getSingleSignWithAnnotation() {
            return singleSignWithAnnotation;
        }

        public Boolean getAllSignToComplete() {
            return allSignToComplete;
        }

        public Boolean getAttachmentAlert() {
            return attachmentAlert;
        }

        public Boolean getAttachmentRequire() {
            return attachmentRequire;
        }

        public List<UserDto> getUsers() {
            return users;
        }

        public CertificatDto getCertificat() {
            return certificat;
        }
    }

    public static class CertificatDto {
        private final Long id;

        public CertificatDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }

    public static class ViewerDto {
        private final String email;
        private final String firstname;
        private final String name;

        public ViewerDto(String email, String firstname, String name) {
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getName() {
            return name;
        }
    }

    public static class TagDto {
        private final Long id;
        private final String name;
        private final String color;

        public TagDto(Long id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getColor() {
            return color;
        }
    }

    public static class UserDto {
        private final String eppn;
        private final String email;
        private final String firstname;
        private final String name;

        public UserDto(String eppn, String email, String firstname, String name) {
            this.eppn = eppn;
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEppn() {
            return eppn;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getName() {
            return name;
        }
    }
}



