package org.esupportail.esupsignature.dto.page.user.signrequest;

import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contexte de construction de la vue "show sign request".
 * Ne contient aucune entité JPA — uniquement des scalaires et des DTO pré-calculés
 * dans la méthode transactionnelle {@code UiFetchSignRequestService.buildShowSignRequestContext}.
 */
public class ShowSignRequestContextDto {

    // ---- Session / identité ----
    private String userEppn;
    private String authUserEppn;
    private boolean otpView;

    // ---- Pour le contrôleur ----
    private String signImagesWarningMessage;
    private Long workflowId;
    private SignType currentStepSignType;

    // ---- DTO finaux pré-construits ----
    private ShowSignRequestDto showSignRequest;
    private SignUiFrontDto signUiFront;

    // ---- Scalaires issus de SignRequest ----
    private Long signRequestId;
    private SignRequestStatus signRequestStatus;
    private boolean signRequestDeleted;
    private String signRequestToken;
    private Long dataId;
    private Long formId;
    private boolean hasDocumentsHistory;
    private String exportedDocumentURI;
    private String pdfaCheck;

    // ---- Scalaires issus de SignBook ----
    private Long signBookId;
    private SignRequestStatus signBookStatus;

    // ---- Light DTOs mappés dans la transaction ----
    private org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto signBookLight;
    private ShowSignRequestDto.SignRequestLightDto signRequestLight;
    private ShowSignRequestDto.WorkflowMetaDto workflowMeta;
    private boolean workflowAvailable;

    // ---- État courant du workflow ----
    private boolean signable;
    private boolean editable;
    private boolean manager;
    private boolean attachmentAlert;
    private boolean attachmentRequire;
    private SignType currentSignType;
    private Integer currentStepNumber;
    private Long currentStepId;
    private boolean currentStepMultiSign;
    private boolean currentStepSingleSignWithAnnotation;
    private SignLevel currentStepMinSignLevel;
    private SignLevel currentStepMaxSignLevel;
    private SignType currentStepRepeatableSignType;
    private Boolean stepRepeatable;
    private int nbSignRequestInSignBookParent;
    private int nbPendingSignRequestInSignBookParent;
    private boolean lastStep;

    // ---- Collections DTO pré-mappées ----
    private List<ShowSignRequestDto.StepDto> steps;
    private List<ShowSignRequestDto.TargetDto> targets;
    private List<ShowSignRequestDto.SignRequestLightDto> clonedSignRequests;
    private List<ShowSignRequestDto.AttachmentDto> attachments;
    private List<ShowSignRequestDto.DocumentDto> originalDocuments;
    private List<ShowSignRequestDto.DocumentDto> signedDocuments;
    private String lastSignedDocumentContentType;
    private List<ShowSignRequestDto.SignRequestTabDto> signRequestTabs;
    private Map<Long, ShowSignRequestDto.RecipientActionDto> recipientActions;

    // ---- Documents à signer (scalaires) ----
    private String toSignDocumentContentType;
    private boolean pdf;

    // ---- Validation DSS (non-JPA) ----
    private Reports reports;
    private List<String> signatureIds;
    private boolean signatureIssue;

    // ---- Données métier chargées par services ----
    /** {@code Field} entities chargées explicitement via service, pas via lazy du graphe principal. */
    private List<FieldFrontDto> fieldFrontDtos;
    /** {@code SignRequestParams} chargés explicitement via service. */
    private List<SignRequestParams> currentSignRequestParamses;
    /** {@code Comment} chargés explicitement via service (commentaires du flux de signature). */
    private List<Comment> comments;
    /** {@code SignRequestParams} de type spot. */
    private List<SignRequestParams> spots;
    private List<String> signImages;
    private String action;
    private Set<String> supervisors;
    private boolean notSigned;

    // ---- Valeurs pré-calculées dans la transaction ----
    private boolean viewedByCurrentUser;
    private boolean displayNotif;
    private boolean tempUsers;
    private boolean currentUserAsSigned;
    private boolean viewRight;
    private boolean auditTrailChecked;
    private boolean hasNextSignBook;
    private Long nextSignRequestId;

    // ---- Audit trail (non-JPA direct) ----
    private AuditTrail auditTrail;
    private String auditTrailSize;

    // ---- Signature avec ----
    private List<SignWith> signWiths;
    private boolean sealCertOK;
    private List<SealCertificatProperties> sealCertificatPropertieses;

    // ---- Externes ----
    private List<RecipientWsDto> externalsRecipients;
    private List<Log> logs;
    private List<Comment> postits;

    // ---- Info utilisateur front (pré-calculé) ----
    private Integer signImageNumber;
    private boolean restore;
    private String phone;
    private Boolean returnToHomeAfterSign;
    private String userName;
    private String authUserName;

    // ---- Accesseurs ----

    public String getUserEppn() { return userEppn; }
    public void setUserEppn(String userEppn) { this.userEppn = userEppn; }
    public String getAuthUserEppn() { return authUserEppn; }
    public void setAuthUserEppn(String authUserEppn) { this.authUserEppn = authUserEppn; }
    public boolean isOtpView() { return otpView; }
    public void setOtpView(boolean otpView) { this.otpView = otpView; }
    public String getSignImagesWarningMessage() { return signImagesWarningMessage; }
    public void setSignImagesWarningMessage(String signImagesWarningMessage) { this.signImagesWarningMessage = signImagesWarningMessage; }
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    public SignType getCurrentStepSignType() { return currentStepSignType; }
    public void setCurrentStepSignType(SignType currentStepSignType) { this.currentStepSignType = currentStepSignType; }
    public ShowSignRequestDto getShowSignRequest() { return showSignRequest; }
    public void setShowSignRequest(ShowSignRequestDto showSignRequest) { this.showSignRequest = showSignRequest; }
    public SignUiFrontDto getSignUiFront() { return signUiFront; }
    public void setSignUiFront(SignUiFrontDto signUiFront) { this.signUiFront = signUiFront; }
    public Long getSignRequestId() { return signRequestId; }
    public void setSignRequestId(Long signRequestId) { this.signRequestId = signRequestId; }
    public SignRequestStatus getSignRequestStatus() { return signRequestStatus; }
    public void setSignRequestStatus(SignRequestStatus signRequestStatus) { this.signRequestStatus = signRequestStatus; }
    public boolean isSignRequestDeleted() { return signRequestDeleted; }
    public void setSignRequestDeleted(boolean signRequestDeleted) { this.signRequestDeleted = signRequestDeleted; }
    public String getSignRequestToken() { return signRequestToken; }
    public void setSignRequestToken(String signRequestToken) { this.signRequestToken = signRequestToken; }
    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public boolean isHasDocumentsHistory() { return hasDocumentsHistory; }
    public void setHasDocumentsHistory(boolean hasDocumentsHistory) { this.hasDocumentsHistory = hasDocumentsHistory; }
    public String getExportedDocumentURI() { return exportedDocumentURI; }
    public void setExportedDocumentURI(String exportedDocumentURI) { this.exportedDocumentURI = exportedDocumentURI; }
    public String getPdfaCheck() { return pdfaCheck; }
    public void setPdfaCheck(String pdfaCheck) { this.pdfaCheck = pdfaCheck; }
    public Long getSignBookId() { return signBookId; }
    public void setSignBookId(Long signBookId) { this.signBookId = signBookId; }
    public SignRequestStatus getSignBookStatus() { return signBookStatus; }
    public void setSignBookStatus(SignRequestStatus signBookStatus) { this.signBookStatus = signBookStatus; }
    public org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto getSignBookLight() { return signBookLight; }
    public void setSignBookLight(org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto signBookLight) { this.signBookLight = signBookLight; }
    public ShowSignRequestDto.SignRequestLightDto getSignRequestLight() { return signRequestLight; }
    public void setSignRequestLight(ShowSignRequestDto.SignRequestLightDto signRequestLight) { this.signRequestLight = signRequestLight; }
    public ShowSignRequestDto.WorkflowMetaDto getWorkflowMeta() { return workflowMeta; }
    public void setWorkflowMeta(ShowSignRequestDto.WorkflowMetaDto workflowMeta) { this.workflowMeta = workflowMeta; }
    public boolean isWorkflowAvailable() { return workflowAvailable; }
    public void setWorkflowAvailable(boolean workflowAvailable) { this.workflowAvailable = workflowAvailable; }
    public boolean isSignable() { return signable; }
    public void setSignable(boolean signable) { this.signable = signable; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public boolean isManager() { return manager; }
    public void setManager(boolean manager) { this.manager = manager; }
    public boolean isAttachmentAlert() { return attachmentAlert; }
    public void setAttachmentAlert(boolean attachmentAlert) { this.attachmentAlert = attachmentAlert; }
    public boolean isAttachmentRequire() { return attachmentRequire; }
    public void setAttachmentRequire(boolean attachmentRequire) { this.attachmentRequire = attachmentRequire; }
    public SignType getCurrentSignType() { return currentSignType; }
    public void setCurrentSignType(SignType currentSignType) { this.currentSignType = currentSignType; }
    public Integer getCurrentStepNumber() { return currentStepNumber; }
    public void setCurrentStepNumber(Integer currentStepNumber) { this.currentStepNumber = currentStepNumber; }
    public Long getCurrentStepId() { return currentStepId; }
    public void setCurrentStepId(Long currentStepId) { this.currentStepId = currentStepId; }
    public boolean isCurrentStepMultiSign() { return currentStepMultiSign; }
    public void setCurrentStepMultiSign(boolean currentStepMultiSign) { this.currentStepMultiSign = currentStepMultiSign; }
    public boolean isCurrentStepSingleSignWithAnnotation() { return currentStepSingleSignWithAnnotation; }
    public void setCurrentStepSingleSignWithAnnotation(boolean v) { this.currentStepSingleSignWithAnnotation = v; }
    public SignLevel getCurrentStepMinSignLevel() { return currentStepMinSignLevel; }
    public void setCurrentStepMinSignLevel(SignLevel l) { this.currentStepMinSignLevel = l; }
    public SignLevel getCurrentStepMaxSignLevel() { return currentStepMaxSignLevel; }
    public void setCurrentStepMaxSignLevel(SignLevel l) { this.currentStepMaxSignLevel = l; }
    public SignType getCurrentStepRepeatableSignType() { return currentStepRepeatableSignType; }
    public void setCurrentStepRepeatableSignType(SignType t) { this.currentStepRepeatableSignType = t; }
    public Boolean getStepRepeatable() { return stepRepeatable; }
    public void setStepRepeatable(Boolean stepRepeatable) { this.stepRepeatable = stepRepeatable; }
    public int getNbSignRequestInSignBookParent() { return nbSignRequestInSignBookParent; }
    public void setNbSignRequestInSignBookParent(int n) { this.nbSignRequestInSignBookParent = n; }
    public int getNbPendingSignRequestInSignBookParent() { return nbPendingSignRequestInSignBookParent; }
    public void setNbPendingSignRequestInSignBookParent(int n) { this.nbPendingSignRequestInSignBookParent = n; }
    public boolean isLastStep() { return lastStep; }
    public void setLastStep(boolean lastStep) { this.lastStep = lastStep; }
    public List<ShowSignRequestDto.StepDto> getSteps() { return steps; }
    public void setSteps(List<ShowSignRequestDto.StepDto> steps) { this.steps = steps; }
    public List<ShowSignRequestDto.TargetDto> getTargets() { return targets; }
    public void setTargets(List<ShowSignRequestDto.TargetDto> targets) { this.targets = targets; }
    public List<ShowSignRequestDto.SignRequestLightDto> getClonedSignRequests() { return clonedSignRequests; }
    public void setClonedSignRequests(List<ShowSignRequestDto.SignRequestLightDto> clonedSignRequests) { this.clonedSignRequests = clonedSignRequests; }
    public List<ShowSignRequestDto.AttachmentDto> getAttachments() { return attachments; }
    public void setAttachments(List<ShowSignRequestDto.AttachmentDto> attachments) { this.attachments = attachments; }
    public List<ShowSignRequestDto.DocumentDto> getOriginalDocuments() { return originalDocuments; }
    public void setOriginalDocuments(List<ShowSignRequestDto.DocumentDto> originalDocuments) { this.originalDocuments = originalDocuments; }
    public List<ShowSignRequestDto.DocumentDto> getSignedDocuments() { return signedDocuments; }
    public void setSignedDocuments(List<ShowSignRequestDto.DocumentDto> signedDocuments) { this.signedDocuments = signedDocuments; }
    public String getLastSignedDocumentContentType() { return lastSignedDocumentContentType; }
    public void setLastSignedDocumentContentType(String lastSignedDocumentContentType) { this.lastSignedDocumentContentType = lastSignedDocumentContentType; }
    public List<ShowSignRequestDto.SignRequestTabDto> getSignRequestTabs() { return signRequestTabs; }
    public void setSignRequestTabs(List<ShowSignRequestDto.SignRequestTabDto> signRequestTabs) { this.signRequestTabs = signRequestTabs; }
    public Map<Long, ShowSignRequestDto.RecipientActionDto> getRecipientActions() { return recipientActions; }
    public void setRecipientActions(Map<Long, ShowSignRequestDto.RecipientActionDto> recipientActions) { this.recipientActions = recipientActions; }
    public String getToSignDocumentContentType() { return toSignDocumentContentType; }
    public void setToSignDocumentContentType(String toSignDocumentContentType) { this.toSignDocumentContentType = toSignDocumentContentType; }
    public boolean isPdf() { return pdf; }
    public void setPdf(boolean pdf) { this.pdf = pdf; }
    public Reports getReports() { return reports; }
    public void setReports(Reports reports) { this.reports = reports; }
    public List<String> getSignatureIds() { return signatureIds; }
    public void setSignatureIds(List<String> signatureIds) { this.signatureIds = signatureIds; }
    public boolean isSignatureIssue() { return signatureIssue; }
    public void setSignatureIssue(boolean signatureIssue) { this.signatureIssue = signatureIssue; }
    public List<FieldFrontDto> getFieldFrontDtos() { return fieldFrontDtos; }
    public void setFieldFrontDtos(List<FieldFrontDto> fieldFrontDtos) { this.fieldFrontDtos = fieldFrontDtos; }
    public List<SignRequestParams> getCurrentSignRequestParamses() { return currentSignRequestParamses; }
    public void setCurrentSignRequestParamses(List<SignRequestParams> currentSignRequestParamses) { this.currentSignRequestParamses = currentSignRequestParamses; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public List<SignRequestParams> getSpots() { return spots; }
    public void setSpots(List<SignRequestParams> spots) { this.spots = spots; }
    public List<String> getSignImages() { return signImages; }
    public void setSignImages(List<String> signImages) { this.signImages = signImages; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Set<String> getSupervisors() { return supervisors; }
    public void setSupervisors(Set<String> supervisors) { this.supervisors = supervisors; }
    public boolean isNotSigned() { return notSigned; }
    public void setNotSigned(boolean notSigned) { this.notSigned = notSigned; }
    public boolean isViewedByCurrentUser() { return viewedByCurrentUser; }
    public void setViewedByCurrentUser(boolean viewedByCurrentUser) { this.viewedByCurrentUser = viewedByCurrentUser; }
    public boolean isDisplayNotif() { return displayNotif; }
    public void setDisplayNotif(boolean displayNotif) { this.displayNotif = displayNotif; }
    public boolean isTempUsers() { return tempUsers; }
    public void setTempUsers(boolean tempUsers) { this.tempUsers = tempUsers; }
    public boolean isCurrentUserAsSigned() { return currentUserAsSigned; }
    public void setCurrentUserAsSigned(boolean currentUserAsSigned) { this.currentUserAsSigned = currentUserAsSigned; }
    public boolean isViewRight() { return viewRight; }
    public void setViewRight(boolean viewRight) { this.viewRight = viewRight; }
    public boolean isAuditTrailChecked() { return auditTrailChecked; }
    public void setAuditTrailChecked(boolean auditTrailChecked) { this.auditTrailChecked = auditTrailChecked; }
    public boolean isHasNextSignBook() { return hasNextSignBook; }
    public void setHasNextSignBook(boolean hasNextSignBook) { this.hasNextSignBook = hasNextSignBook; }
    public Long getNextSignRequestId() { return nextSignRequestId; }
    public void setNextSignRequestId(Long nextSignRequestId) { this.nextSignRequestId = nextSignRequestId; }
    public AuditTrail getAuditTrail() { return auditTrail; }
    public void setAuditTrail(AuditTrail auditTrail) { this.auditTrail = auditTrail; }
    public String getAuditTrailSize() { return auditTrailSize; }
    public void setAuditTrailSize(String auditTrailSize) { this.auditTrailSize = auditTrailSize; }
    public List<SignWith> getSignWiths() { return signWiths; }
    public void setSignWiths(List<SignWith> signWiths) { this.signWiths = signWiths; }
    public boolean isSealCertOK() { return sealCertOK; }
    public void setSealCertOK(boolean sealCertOK) { this.sealCertOK = sealCertOK; }
    public List<SealCertificatProperties> getSealCertificatPropertieses() { return sealCertificatPropertieses; }
    public void setSealCertificatPropertieses(List<SealCertificatProperties> sealCertificatPropertieses) { this.sealCertificatPropertieses = sealCertificatPropertieses; }
    public List<RecipientWsDto> getExternalsRecipients() { return externalsRecipients; }
    public void setExternalsRecipients(List<RecipientWsDto> externalsRecipients) { this.externalsRecipients = externalsRecipients; }
    public List<Log> getLogs() { return logs; }
    public void setLogs(List<Log> logs) { this.logs = logs; }
    public List<Comment> getPostits() { return postits; }
    public void setPostits(List<Comment> postits) { this.postits = postits; }
    public Integer getSignImageNumber() { return signImageNumber; }
    public void setSignImageNumber(Integer signImageNumber) { this.signImageNumber = signImageNumber; }
    public boolean isRestore() { return restore; }
    public void setRestore(boolean restore) { this.restore = restore; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getReturnToHomeAfterSign() { return returnToHomeAfterSign; }
    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) { this.returnToHomeAfterSign = returnToHomeAfterSign; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAuthUserName() { return authUserName; }
    public void setAuthUserName(String authUserName) { this.authUserName = authUserName; }
}
