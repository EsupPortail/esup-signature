package org.esupportail.esupsignature.dto.page.user.signrequest;

import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShowSignRequestDto {

    private SignBookLightDto signBookLight;
    private SignRequestLightDto signRequestLight;
    private SignRequestFullDto signRequestFull;
    private WorkflowMetaDto workflow;
    private String urlProfil;
    private Boolean displayNotif;
    private Boolean tempUsers;
    private Boolean lastStep;
    private Boolean currentUserAsSigned;
    private List<String> signatureIds;
    private Boolean signatureIssue;
    private Set<String> supervisors;
    private String toSignDocumentContentType;
    private List<Comment> postits;
    private List<AttachmentDto> attachments;
    private List<DocumentDto> originalDocuments;
    private List<DocumentDto> signedDocuments;
    private String exportedDocumentURI;
    private String lastSignedDocumentContentType;
    private Boolean hasNextSignBook;
    private Long nextSignRequestId;
    private List<SignWith> signWiths;
    private AuditTrail auditTrail;
    private String size;
    private Boolean sealCertOK;
    private List<SealCertificatProperties> sealCertificatPropertieses;
    private List<StepDto> steps;
    private List<TargetDto> targets;
    private List<SignRequestLightDto> clonedSignRequests;
    private Map<Long, RecipientActionDto> recipientActions;
    private List<SignRequestTabDto> signRequestTabs;
    private Integer liveWorkflowStepCount;
    private Boolean viewedByCurrentUser;
    private Boolean viewRight;
    private List<Log> logs;
    private String pdfaCheck;
    private Boolean auditTrailChecked;
    private List<RecipientWsDto> externalsRecipients;

    public SignBookLightDto getSignBookLight() { return signBookLight; }
    public void setSignBookLight(SignBookLightDto signBookLight) { this.signBookLight = signBookLight; }
    public SignRequestLightDto getSignRequestLight() { return signRequestLight; }
    public void setSignRequestLight(SignRequestLightDto signRequestLight) { this.signRequestLight = signRequestLight; }
    public SignRequestFullDto getSignRequestFull() { return signRequestFull; }
    public void setSignRequestFull(SignRequestFullDto signRequestFull) { this.signRequestFull = signRequestFull; }
    public WorkflowMetaDto getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowMetaDto workflow) { this.workflow = workflow; }
    public String getUrlProfil() { return urlProfil; }
    public void setUrlProfil(String urlProfil) { this.urlProfil = urlProfil; }
    public Boolean getDisplayNotif() { return displayNotif; }
    public void setDisplayNotif(Boolean displayNotif) { this.displayNotif = displayNotif; }
    public Boolean getTempUsers() { return tempUsers; }
    public void setTempUsers(Boolean tempUsers) { this.tempUsers = tempUsers; }
    public Boolean getLastStep() { return lastStep; }
    public void setLastStep(Boolean lastStep) { this.lastStep = lastStep; }
    public Boolean getCurrentUserAsSigned() { return currentUserAsSigned; }
    public void setCurrentUserAsSigned(Boolean currentUserAsSigned) { this.currentUserAsSigned = currentUserAsSigned; }
    public List<String> getSignatureIds() { return signatureIds; }
    public void setSignatureIds(List<String> signatureIds) { this.signatureIds = signatureIds; }
    public Boolean getSignatureIssue() { return signatureIssue; }
    public void setSignatureIssue(Boolean signatureIssue) { this.signatureIssue = signatureIssue; }
    public Set<String> getSupervisors() { return supervisors; }
    public void setSupervisors(Set<String> supervisors) { this.supervisors = supervisors; }
    public String getToSignDocumentContentType() { return toSignDocumentContentType; }
    public void setToSignDocumentContentType(String toSignDocumentContentType) { this.toSignDocumentContentType = toSignDocumentContentType; }
    public List<Comment> getPostits() { return postits; }
    public void setPostits(List<Comment> postits) { this.postits = postits; }
    public List<AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentDto> attachments) { this.attachments = attachments; }
    public List<DocumentDto> getOriginalDocuments() { return originalDocuments; }
    public void setOriginalDocuments(List<DocumentDto> originalDocuments) { this.originalDocuments = originalDocuments; }
    public List<DocumentDto> getSignedDocuments() { return signedDocuments; }
    public void setSignedDocuments(List<DocumentDto> signedDocuments) { this.signedDocuments = signedDocuments; }
    public String getExportedDocumentURI() { return exportedDocumentURI; }
    public void setExportedDocumentURI(String exportedDocumentURI) { this.exportedDocumentURI = exportedDocumentURI; }
    public String getLastSignedDocumentContentType() { return lastSignedDocumentContentType; }
    public void setLastSignedDocumentContentType(String lastSignedDocumentContentType) { this.lastSignedDocumentContentType = lastSignedDocumentContentType; }
    public Boolean getHasNextSignBook() { return hasNextSignBook; }
    public void setHasNextSignBook(Boolean hasNextSignBook) { this.hasNextSignBook = hasNextSignBook; }
    public Long getNextSignRequestId() { return nextSignRequestId; }
    public void setNextSignRequestId(Long nextSignRequestId) { this.nextSignRequestId = nextSignRequestId; }
    public List<SignWith> getSignWiths() { return signWiths; }
    public void setSignWiths(List<SignWith> signWiths) { this.signWiths = signWiths; }
    public AuditTrail getAuditTrail() { return auditTrail; }
    public void setAuditTrail(AuditTrail auditTrail) { this.auditTrail = auditTrail; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Boolean getSealCertOK() { return sealCertOK; }
    public void setSealCertOK(Boolean sealCertOK) { this.sealCertOK = sealCertOK; }
    public List<SealCertificatProperties> getSealCertificatPropertieses() { return sealCertificatPropertieses; }
    public void setSealCertificatPropertieses(List<SealCertificatProperties> sealCertificatPropertieses) { this.sealCertificatPropertieses = sealCertificatPropertieses; }
    public List<StepDto> getSteps() { return steps; }
    public void setSteps(List<StepDto> steps) { this.steps = steps; }
    public List<TargetDto> getTargets() { return targets; }
    public void setTargets(List<TargetDto> targets) { this.targets = targets; }
    public List<SignRequestLightDto> getClonedSignRequests() { return clonedSignRequests; }
    public void setClonedSignRequests(List<SignRequestLightDto> clonedSignRequests) { this.clonedSignRequests = clonedSignRequests; }
    public Map<Long, RecipientActionDto> getRecipientActions() { return recipientActions; }
    public void setRecipientActions(Map<Long, RecipientActionDto> recipientActions) { this.recipientActions = recipientActions; }
    public List<SignRequestTabDto> getSignRequestTabs() { return signRequestTabs; }
    public void setSignRequestTabs(List<SignRequestTabDto> signRequestTabs) { this.signRequestTabs = signRequestTabs; }
    public Integer getLiveWorkflowStepCount() { return liveWorkflowStepCount; }
    public void setLiveWorkflowStepCount(Integer liveWorkflowStepCount) { this.liveWorkflowStepCount = liveWorkflowStepCount; }
    public Boolean getViewedByCurrentUser() { return viewedByCurrentUser; }
    public void setViewedByCurrentUser(Boolean viewedByCurrentUser) { this.viewedByCurrentUser = viewedByCurrentUser; }
    public Boolean getViewRight() { return viewRight; }
    public void setViewRight(Boolean viewRight) { this.viewRight = viewRight; }
    public List<Log> getLogs() { return logs; }
    public void setLogs(List<Log> logs) { this.logs = logs; }
    public String getPdfaCheck() { return pdfaCheck; }
    public void setPdfaCheck(String pdfaCheck) { this.pdfaCheck = pdfaCheck; }
    public Boolean getAuditTrailChecked() { return auditTrailChecked; }
    public void setAuditTrailChecked(Boolean auditTrailChecked) { this.auditTrailChecked = auditTrailChecked; }
    public List<RecipientWsDto> getExternalsRecipients() { return externalsRecipients; }
    public void setExternalsRecipients(List<RecipientWsDto> externalsRecipients) { this.externalsRecipients = externalsRecipients; }

    public SignBookLightDto signBookLight() { return signBookLight; }
    public SignRequestLightDto signRequestLight() { return signRequestLight; }
    public SignRequestFullDto signRequestFull() { return signRequestFull; }
    public WorkflowMetaDto workflow() { return workflow; }
    public String urlProfil() { return urlProfil; }
    public Boolean displayNotif() { return displayNotif; }
    public Boolean tempUsers() { return tempUsers; }
    public Boolean lastStep() { return lastStep; }
    public Boolean currentUserAsSigned() { return currentUserAsSigned; }
    public List<String> signatureIds() { return signatureIds; }
    public Boolean signatureIssue() { return signatureIssue; }
    public Set<String> supervisors() { return supervisors; }
    public String toSignDocumentContentType() { return toSignDocumentContentType; }
    public List<Comment> postits() { return postits; }
    public List<AttachmentDto> attachments() { return attachments; }
    public List<DocumentDto> originalDocuments() { return originalDocuments; }
    public List<DocumentDto> signedDocuments() { return signedDocuments; }
    public String exportedDocumentURI() { return exportedDocumentURI; }
    public String lastSignedDocumentContentType() { return lastSignedDocumentContentType; }
    public Boolean hasNextSignBook() { return hasNextSignBook; }
    public Long nextSignRequestId() { return nextSignRequestId; }
    public List<SignWith> signWiths() { return signWiths; }
    public AuditTrail auditTrail() { return auditTrail; }
    public String size() { return size; }
    public Boolean sealCertOK() { return sealCertOK; }
    public List<SealCertificatProperties> sealCertificatPropertieses() { return sealCertificatPropertieses; }
    public List<StepDto> steps() { return steps; }
    public List<TargetDto> targets() { return targets; }
    public List<SignRequestLightDto> clonedSignRequests() { return clonedSignRequests; }
    public Map<Long, RecipientActionDto> recipientActions() { return recipientActions; }
    public List<SignRequestTabDto> signRequestTabs() { return signRequestTabs; }
    public Integer liveWorkflowStepCount() { return liveWorkflowStepCount; }
    public Boolean viewedByCurrentUser() { return viewedByCurrentUser; }
    public Boolean viewRight() { return viewRight; }
    public List<Log> logs() { return logs; }
    public String pdfaCheck() { return pdfaCheck; }
    public Boolean auditTrailChecked() { return auditTrailChecked; }
    public List<RecipientWsDto> externalsRecipients() { return externalsRecipients; }

    public static class SignBookViewerDto {
        private Long id;
        private String firstname;
        private String name;
        private String email;

        public SignBookViewerDto() {
        }

        public SignBookViewerDto(Long id, String firstname, String name, String email) {
            this.id = id;
            this.firstname = firstname;
            this.name = name;
            this.email = email;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class WorkflowMetaDto {
        private Boolean hasWorkflow;
        private Boolean externalCanReaderAnnotations;
        private Boolean disableSidebarForExternal;
        private Boolean externalCanReaderAttachments;
        private Boolean externalCanEdit;
        private Boolean externalCanEditAttachments;
        private Boolean authorizeClone;
        private Boolean forbidDownloadsBeforeEnd;
        private Boolean sendAlertToAllRecipients;
        private Integer workflowStepCount;
        private String mailFrom;

        public WorkflowMetaDto() {
        }

        public WorkflowMetaDto(Boolean hasWorkflow, Boolean externalCanReaderAnnotations,
                               Boolean disableSidebarForExternal, Boolean externalCanReaderAttachments,
                               Boolean externalCanEdit, Boolean externalCanEditAttachments, Boolean authorizeClone,
                               Boolean forbidDownloadsBeforeEnd, Boolean sendAlertToAllRecipients,
                               Integer workflowStepCount, String mailFrom) {
            this.hasWorkflow = hasWorkflow;
            this.externalCanReaderAnnotations = externalCanReaderAnnotations;
            this.disableSidebarForExternal = disableSidebarForExternal;
            this.externalCanReaderAttachments = externalCanReaderAttachments;
            this.externalCanEdit = externalCanEdit;
            this.externalCanEditAttachments = externalCanEditAttachments;
            this.authorizeClone = authorizeClone;
            this.forbidDownloadsBeforeEnd = forbidDownloadsBeforeEnd;
            this.sendAlertToAllRecipients = sendAlertToAllRecipients;
            this.workflowStepCount = workflowStepCount;
            this.mailFrom = mailFrom;
        }

        public Boolean getHasWorkflow() { return hasWorkflow; }
        public void setHasWorkflow(Boolean hasWorkflow) { this.hasWorkflow = hasWorkflow; }
        public Boolean getExternalCanReaderAnnotations() { return externalCanReaderAnnotations; }
        public void setExternalCanReaderAnnotations(Boolean externalCanReaderAnnotations) { this.externalCanReaderAnnotations = externalCanReaderAnnotations; }
        public Boolean getDisableSidebarForExternal() { return disableSidebarForExternal; }
        public void setDisableSidebarForExternal(Boolean disableSidebarForExternal) { this.disableSidebarForExternal = disableSidebarForExternal; }
        public Boolean getExternalCanReaderAttachments() { return externalCanReaderAttachments; }
        public void setExternalCanReaderAttachments(Boolean externalCanReaderAttachments) { this.externalCanReaderAttachments = externalCanReaderAttachments; }
        public Boolean getExternalCanEdit() { return externalCanEdit; }
        public void setExternalCanEdit(Boolean externalCanEdit) { this.externalCanEdit = externalCanEdit; }
        public Boolean getExternalCanEditAttachments() { return externalCanEditAttachments; }
        public void setExternalCanEditAttachments(Boolean externalCanEditAttachments) { this.externalCanEditAttachments = externalCanEditAttachments; }
        public Boolean getAuthorizeClone() { return authorizeClone; }
        public void setAuthorizeClone(Boolean authorizeClone) { this.authorizeClone = authorizeClone; }
        public Boolean getForbidDownloadsBeforeEnd() { return forbidDownloadsBeforeEnd; }
        public void setForbidDownloadsBeforeEnd(Boolean forbidDownloadsBeforeEnd) { this.forbidDownloadsBeforeEnd = forbidDownloadsBeforeEnd; }
        public Boolean getSendAlertToAllRecipients() { return sendAlertToAllRecipients; }
        public void setSendAlertToAllRecipients(Boolean sendAlertToAllRecipients) { this.sendAlertToAllRecipients = sendAlertToAllRecipients; }
        public Integer getWorkflowStepCount() { return workflowStepCount; }
        public void setWorkflowStepCount(Integer workflowStepCount) { this.workflowStepCount = workflowStepCount; }
        public String getMailFrom() { return mailFrom; }
        public void setMailFrom(String mailFrom) { this.mailFrom = mailFrom; }
    }

    public static class SignRequestLightDto {
        private Long id;
        private Long clonedFromId;
        private SignRequestStatus status;
        private Boolean deleted;
        private String token;
        private SignRequestUserDto createBy;
        private List<String> links;

        public SignRequestLightDto() {
        }

        public SignRequestLightDto(Long id, SignRequestStatus status, Boolean deleted, String token,
                                   SignRequestUserDto createBy, List<String> links) {
            this.id = id;
            this.status = status;
            this.deleted = deleted;
            this.token = token;
            this.createBy = createBy;
            this.links = links;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getClonedFromId() { return clonedFromId; }
        public void setClonedFromId(Long clonedFromId) { this.clonedFromId = clonedFromId; }
        public SignRequestStatus getStatus() { return status; }
        public void setStatus(SignRequestStatus status) { this.status = status; }
        public Boolean getDeleted() { return deleted; }
        public void setDeleted(Boolean deleted) { this.deleted = deleted; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public SignRequestUserDto getCreateBy() { return createBy; }
        public void setCreateBy(SignRequestUserDto createBy) { this.createBy = createBy; }
        public List<String> getLinks() { return links; }
        public void setLinks(List<String> links) { this.links = links; }
    }

    public static class SignRequestUserDto {
        private Long id;
        private String eppn;
        private String firstname;
        private String name;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class SignRequestTabDto {
        private Long id;
        private String title;
        private SignRequestStatus status;
        private boolean deleted;
        private boolean viewedByCurrentUser;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public SignRequestStatus getStatus() { return status; }
        public void setStatus(SignRequestStatus status) { this.status = status; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(Boolean deleted) { this.deleted = Boolean.TRUE.equals(deleted); }
        public boolean isViewedByCurrentUser() {
            return viewedByCurrentUser;
        }
        public void setViewedByCurrentUser(boolean viewedByCurrentUser) {
            this.viewedByCurrentUser = viewedByCurrentUser;
        }
    }

    public static class AttachmentDto {
        private Long id;
        private String fileName;
        private AttachmentUserDto createBy;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public AttachmentUserDto getCreateBy() { return createBy; }
        public void setCreateBy(AttachmentUserDto createBy) { this.createBy = createBy; }
    }

    public static class AttachmentUserDto {
        private String eppn;
        private String firstname;
        private String name;

        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class DocumentDto {
        private Long id;
        private String fileName;
        private Long size;
        private String contentType;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }

    public static class StepDto {
        private Long id;
        private String description;
        private Boolean changeable;
        private SignType signType;
        private SignLevel minSignLevel;
        private Boolean autoSign;
        private Boolean allSignToComplete;
        private Boolean repeatable;
        private Boolean sealVisa;
        private List<StepUserDto> users;
        private List<StepRecipientDto> recipients;

        public StepDto() {
        }

        public StepDto(Long id, String description, Boolean changeable, SignType signType, SignLevel minSignLevel,
                       Boolean autoSign, Boolean allSignToComplete, Boolean repeatable, Boolean sealVisa, List<StepUserDto> users,
                       List<StepRecipientDto> recipients) {
            this.id = id;
            this.description = description;
            this.changeable = changeable;
            this.signType = signType;
            this.minSignLevel = minSignLevel;
            this.autoSign = autoSign;
            this.allSignToComplete = allSignToComplete;
            this.repeatable = repeatable;
            this.sealVisa = sealVisa;
            this.users = users;
            this.recipients = recipients;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getChangeable() { return changeable; }
        public void setChangeable(Boolean changeable) { this.changeable = changeable; }
        public SignType getSignType() { return signType; }
        public void setSignType(SignType signType) { this.signType = signType; }
        public SignLevel getMinSignLevel() { return minSignLevel; }
        public void setMinSignLevel(SignLevel minSignLevel) { this.minSignLevel = minSignLevel; }
        public Boolean getAutoSign() { return autoSign; }
        public void setAutoSign(Boolean autoSign) { this.autoSign = autoSign; }
        public Boolean getAllSignToComplete() { return allSignToComplete; }
        public void setAllSignToComplete(Boolean allSignToComplete) { this.allSignToComplete = allSignToComplete; }
        public Boolean getRepeatable() { return repeatable; }
        public void setRepeatable(Boolean repeatable) { this.repeatable = repeatable; }
        public Boolean getSealVisa() { return sealVisa; }
        public void setSealVisa(Boolean sealVisa) { this.sealVisa = sealVisa; }
        public List<StepUserDto> getUsers() { return users; }
        public void setUsers(List<StepUserDto> users) { this.users = users; }
        public List<StepRecipientDto> getRecipients() { return recipients; }
        public void setRecipients(List<StepRecipientDto> recipients) { this.recipients = recipients; }
    }

    public static class StepRecipientDto {
        private Long id;
        private StepUserDto user;
        private Boolean signed;

        public StepRecipientDto() {
        }

        public StepRecipientDto(Long id, StepUserDto user, Boolean signed) {
            this.id = id;
            this.user = user;
            this.signed = signed;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public StepUserDto getUser() { return user; }
        public void setUser(StepUserDto user) { this.user = user; }
        public Boolean getSigned() { return signed; }
        public void setSigned(Boolean signed) { this.signed = signed; }
    }

    public static class StepUserDto {
        private Long id;
        private String firstname;
        private String name;
        private String email;
        private String phone;
        private String hidedPhone;
        private UserType userType;

        public StepUserDto() {
        }

        public StepUserDto(Long id, String firstname, String name, String email, String phone, String hidedPhone, UserType userType) {
            this.id = id;
            this.firstname = firstname;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.hidedPhone = hidedPhone;
            this.userType = userType;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getHidedPhone() { return hidedPhone; }
        public void setHidedPhone(String hidedPhone) { this.hidedPhone = hidedPhone; }
        public UserType getUserType() { return userType; }
        public void setUserType(UserType userType) { this.userType = userType; }
    }

    public static class TargetDto {
        private String targetUri;
        private String protectedTargetUri;
        private Boolean targetOk;

        public TargetDto() {
        }

        public TargetDto(String targetUri, String protectedTargetUri, Boolean targetOk) {
            this.targetUri = targetUri;
            this.protectedTargetUri = protectedTargetUri;
            this.targetOk = targetOk;
        }

        public String getTargetUri() { return targetUri; }
        public void setTargetUri(String targetUri) { this.targetUri = targetUri; }
        public String getProtectedTargetUri() { return protectedTargetUri; }
        public void setProtectedTargetUri(String protectedTargetUri) { this.protectedTargetUri = protectedTargetUri; }
        public Boolean getTargetOk() { return targetOk; }
        public void setTargetOk(Boolean targetOk) { this.targetOk = targetOk; }
    }

    public static class RecipientActionDto {
        private ActionType actionType;
        private Date date;

        public RecipientActionDto() {
        }

        public RecipientActionDto(ActionType actionType, Date date) {
            this.actionType = actionType;
            this.date = date;
        }

        public ActionType getActionType() { return actionType; }
        public void setActionType(ActionType actionType) { this.actionType = actionType; }
        public Date getDate() { return date; }
        public void setDate(Date date) { this.date = date; }
    }
}
