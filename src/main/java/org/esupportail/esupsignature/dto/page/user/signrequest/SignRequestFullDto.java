package org.esupportail.esupsignature.dto.page.user.signrequest;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;

public class SignRequestFullDto {

    private Long signRequestId;
    private Long dataId;
    private Long formId;
    private List<SignRequestParams> signRequestParams;
    private SignType currentSignType;
    private Boolean signable;
    private Boolean editable;
    private List<Comment> comments;
    private List<SignRequestParams> spots;
    private Boolean pdf;
    private Integer currentStepNumber;
    private Boolean currentStepMultiSign;
    private Boolean currentStepSingleSignWithAnnotation;
    private SignLevel currentStepMinSignLevel;
    private List<String> signImages;
    private List<Field> fields;
    private Boolean stepRepeatable;
    private SignType currentStepRepeatableSignType;
    private SignRequestStatus status;
    private String action;
    private Integer nbSignRequests;
    private Boolean notSigned;
    private Boolean attachmentAlert;
    private Boolean attachmentRequire;
    private Boolean manager;
    private Boolean updateAllowed;
    private Boolean commentDeleteAllowed;
    private Boolean hasDocumentsHistory;

    public Long getSignRequestId() { return signRequestId; }
    public void setSignRequestId(Long signRequestId) { this.signRequestId = signRequestId; }
    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public List<SignRequestParams> getSignRequestParams() { return signRequestParams; }
    public void setSignRequestParams(List<SignRequestParams> signRequestParams) { this.signRequestParams = signRequestParams; }
    public SignType getCurrentSignType() { return currentSignType; }
    public void setCurrentSignType(SignType currentSignType) { this.currentSignType = currentSignType; }
    public Boolean getSignable() { return signable; }
    public void setSignable(Boolean signable) { this.signable = signable; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public List<SignRequestParams> getSpots() { return spots; }
    public void setSpots(List<SignRequestParams> spots) { this.spots = spots; }
    public Boolean getPdf() { return pdf; }
    public void setPdf(Boolean pdf) { this.pdf = pdf; }
    public Integer getCurrentStepNumber() { return currentStepNumber; }
    public void setCurrentStepNumber(Integer currentStepNumber) { this.currentStepNumber = currentStepNumber; }
    public Boolean getCurrentStepMultiSign() { return currentStepMultiSign; }
    public void setCurrentStepMultiSign(Boolean currentStepMultiSign) { this.currentStepMultiSign = currentStepMultiSign; }
    public Boolean getCurrentStepSingleSignWithAnnotation() { return currentStepSingleSignWithAnnotation; }
    public void setCurrentStepSingleSignWithAnnotation(Boolean currentStepSingleSignWithAnnotation) { this.currentStepSingleSignWithAnnotation = currentStepSingleSignWithAnnotation; }
    public SignLevel getCurrentStepMinSignLevel() { return currentStepMinSignLevel; }
    public void setCurrentStepMinSignLevel(SignLevel currentStepMinSignLevel) { this.currentStepMinSignLevel = currentStepMinSignLevel; }
    public List<String> getSignImages() { return signImages; }
    public void setSignImages(List<String> signImages) { this.signImages = signImages; }
    public List<Field> getFields() { return fields; }
    public void setFields(List<Field> fields) { this.fields = fields; }
    public Boolean getStepRepeatable() { return stepRepeatable; }
    public void setStepRepeatable(Boolean stepRepeatable) { this.stepRepeatable = stepRepeatable; }
    public SignType getCurrentStepRepeatableSignType() { return currentStepRepeatableSignType; }
    public void setCurrentStepRepeatableSignType(SignType currentStepRepeatableSignType) { this.currentStepRepeatableSignType = currentStepRepeatableSignType; }
    public SignRequestStatus getStatus() { return status; }
    public void setStatus(SignRequestStatus status) { this.status = status; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getNbSignRequests() { return nbSignRequests; }
    public void setNbSignRequests(Integer nbSignRequests) { this.nbSignRequests = nbSignRequests; }
    public Boolean getNotSigned() { return notSigned; }
    public void setNotSigned(Boolean notSigned) { this.notSigned = notSigned; }
    public Boolean getAttachmentAlert() { return attachmentAlert; }
    public void setAttachmentAlert(Boolean attachmentAlert) { this.attachmentAlert = attachmentAlert; }
    public Boolean getAttachmentRequire() { return attachmentRequire; }
    public void setAttachmentRequire(Boolean attachmentRequire) { this.attachmentRequire = attachmentRequire; }
    public Boolean getManager() { return manager; }
    public void setManager(Boolean manager) { this.manager = manager; }
    public Boolean getUpdateAllowed() { return updateAllowed; }
    public void setUpdateAllowed(Boolean updateAllowed) { this.updateAllowed = updateAllowed; }
    public Boolean getCommentDeleteAllowed() { return commentDeleteAllowed; }
    public void setCommentDeleteAllowed(Boolean commentDeleteAllowed) { this.commentDeleteAllowed = commentDeleteAllowed; }
    public Boolean getHasDocumentsHistory() { return hasDocumentsHistory; }
    public void setHasDocumentsHistory(Boolean hasDocumentsHistory) { this.hasDocumentsHistory = hasDocumentsHistory; }

    public Long signRequestId() { return signRequestId; }
    public Long dataId() { return dataId; }
    public Long formId() { return formId; }
    public List<SignRequestParams> signRequestParams() { return signRequestParams; }
    public SignType currentSignType() { return currentSignType; }
    public Boolean signable() { return signable; }
    public Boolean editable() { return editable; }
    public List<Comment> comments() { return comments; }
    public List<SignRequestParams> spots() { return spots; }
    public Boolean pdf() { return pdf; }
    public Integer currentStepNumber() { return currentStepNumber; }
    public Boolean currentStepMultiSign() { return currentStepMultiSign; }
    public Boolean currentStepSingleSignWithAnnotation() { return currentStepSingleSignWithAnnotation; }
    public SignLevel currentStepMinSignLevel() { return currentStepMinSignLevel; }
    public List<String> signImages() { return signImages; }
    public List<Field> fields() { return fields; }
    public Boolean stepRepeatable() { return stepRepeatable; }
    public SignType currentStepRepeatableSignType() { return currentStepRepeatableSignType; }
    public SignRequestStatus status() { return status; }
    public String action() { return action; }
    public Integer nbSignRequests() { return nbSignRequests; }
    public Boolean notSigned() { return notSigned; }
    public Boolean attachmentAlert() { return attachmentAlert; }
    public Boolean attachmentRequire() { return attachmentRequire; }
    public Boolean manager() { return manager; }
    public Boolean updateAllowed() { return updateAllowed; }
    public Boolean commentDeleteAllowed() { return commentDeleteAllowed; }
    public Boolean hasDocumentsHistory() { return hasDocumentsHistory; }
}
