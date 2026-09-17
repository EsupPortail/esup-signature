package org.esupportail.esupsignature.dto.page.user.signrequest;

import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;

public class SignUiFrontDto {

    private Long signRequestId;
    private Long dataId;
    private Long formId;
    private List<ShowSignRequestDto.StepDto> steps;
    private List<SignRequestParamsFrontDto> currentSignRequestParamses;
    private Integer signImageNumber;
    private SignType currentSignType;
    private Boolean signable;
    private Boolean editable;
    private List<CommentFrontDto> comments;
    private List<SignRequestParamsFrontDto> spots;
    private Boolean pdf;
    private Integer currentStepNumber;
    private Boolean currentStepMultiSign;
    private Boolean currentStepSingleSignWithAnnotation;
    private SignLevel currentStepMinSignLevel;
    private Boolean workflowAvailable;
    private List<String> signImages;
    private String userName;
    private String authUserName;
    private List<FieldFrontDto> fields;
    private Boolean stepRepeatable;
    private SignRequestStatus status;
    private String action;
    private Integer nbSignRequests;
    private Integer nbPendingSignRequests;
    private Boolean notSigned;
    private Boolean attachmentAlert;
    private Boolean attachmentRequire;
    private Boolean otp;
    private Boolean restore;
    private String phone;
    private Boolean returnToHomeAfterSign;
    private Boolean manager;

    public Long getSignRequestId() { return signRequestId; }
    public void setSignRequestId(Long signRequestId) { this.signRequestId = signRequestId; }
    public Long getDataId() { return dataId; }
    public void setDataId(Long dataId) { this.dataId = dataId; }
    public Long getFormId() { return formId; }
    public void setFormId(Long formId) { this.formId = formId; }
    public List<ShowSignRequestDto.StepDto> getSteps() { return steps; }
    public void setSteps(List<ShowSignRequestDto.StepDto> steps) { this.steps = steps; }
    public List<SignRequestParamsFrontDto> getCurrentSignRequestParamses() { return currentSignRequestParamses; }
    public void setCurrentSignRequestParamses(List<SignRequestParamsFrontDto> currentSignRequestParamses) { this.currentSignRequestParamses = currentSignRequestParamses; }
    public Integer getSignImageNumber() { return signImageNumber; }
    public void setSignImageNumber(Integer signImageNumber) { this.signImageNumber = signImageNumber; }
    public SignType getCurrentSignType() { return currentSignType; }
    public void setCurrentSignType(SignType currentSignType) { this.currentSignType = currentSignType; }
    public Boolean getSignable() { return signable; }
    public void setSignable(Boolean signable) { this.signable = signable; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public List<CommentFrontDto> getComments() { return comments; }
    public void setComments(List<CommentFrontDto> comments) { this.comments = comments; }
    public List<SignRequestParamsFrontDto> getSpots() { return spots; }
    public void setSpots(List<SignRequestParamsFrontDto> spots) { this.spots = spots; }
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
    public Boolean getWorkflowAvailable() { return workflowAvailable; }
    public void setWorkflowAvailable(Boolean workflowAvailable) { this.workflowAvailable = workflowAvailable; }
    public List<String> getSignImages() { return signImages; }
    public void setSignImages(List<String> signImages) { this.signImages = signImages; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAuthUserName() { return authUserName; }
    public void setAuthUserName(String authUserName) { this.authUserName = authUserName; }
    public List<FieldFrontDto> getFields() { return fields; }
    public void setFields(List<FieldFrontDto> fields) { this.fields = fields; }
    public Boolean getStepRepeatable() { return stepRepeatable; }
    public void setStepRepeatable(Boolean stepRepeatable) { this.stepRepeatable = stepRepeatable; }
    public SignRequestStatus getStatus() { return status; }
    public void setStatus(SignRequestStatus status) { this.status = status; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Integer getNbSignRequests() { return nbSignRequests; }
    public void setNbSignRequests(Integer nbSignRequests) { this.nbSignRequests = nbSignRequests; }
    public Integer getNbPendingSignRequests() { return nbPendingSignRequests; }
    public void setNbPendingSignRequests(Integer nbPendingSignRequests) { this.nbPendingSignRequests = nbPendingSignRequests; }
    public Boolean getNotSigned() { return notSigned; }
    public void setNotSigned(Boolean notSigned) { this.notSigned = notSigned; }
    public Boolean getAttachmentAlert() { return attachmentAlert; }
    public void setAttachmentAlert(Boolean attachmentAlert) { this.attachmentAlert = attachmentAlert; }
    public Boolean getAttachmentRequire() { return attachmentRequire; }
    public void setAttachmentRequire(Boolean attachmentRequire) { this.attachmentRequire = attachmentRequire; }
    public Boolean getOtp() { return otp; }
    public void setOtp(Boolean otp) { this.otp = otp; }
    public Boolean getRestore() { return restore; }
    public void setRestore(Boolean restore) { this.restore = restore; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Boolean getReturnToHomeAfterSign() { return returnToHomeAfterSign; }
    public void setReturnToHomeAfterSign(Boolean returnToHomeAfterSign) { this.returnToHomeAfterSign = returnToHomeAfterSign; }
    public Boolean getManager() { return manager; }
    public void setManager(Boolean manager) { this.manager = manager; }

    public Long signRequestId() { return signRequestId; }
    public Long dataId() { return dataId; }
    public Long formId() { return formId; }
    public List<ShowSignRequestDto.StepDto> steps() { return steps; }
    public List<SignRequestParamsFrontDto> currentSignRequestParamses() { return currentSignRequestParamses; }
    public Integer signImageNumber() { return signImageNumber; }
    public SignType currentSignType() { return currentSignType; }
    public Boolean signable() { return signable; }
    public Boolean editable() { return editable; }
    public List<CommentFrontDto> comments() { return comments; }
    public List<SignRequestParamsFrontDto> spots() { return spots; }
    public Boolean pdf() { return pdf; }
    public Integer currentStepNumber() { return currentStepNumber; }
    public Boolean currentStepMultiSign() { return currentStepMultiSign; }
    public Boolean currentStepSingleSignWithAnnotation() { return currentStepSingleSignWithAnnotation; }
    public SignLevel currentStepMinSignLevel() { return currentStepMinSignLevel; }
    public Boolean workflowAvailable() { return workflowAvailable; }
    public List<String> signImages() { return signImages; }
    public String userName() { return userName; }
    public String authUserName() { return authUserName; }
    public List<FieldFrontDto> fields() { return fields; }
    public Boolean stepRepeatable() { return stepRepeatable; }
    public SignRequestStatus status() { return status; }
    public String action() { return action; }
    public Integer nbSignRequests() { return nbSignRequests; }
    public Integer nbPendingSignRequests() { return nbPendingSignRequests; }
    public Boolean notSigned() { return notSigned; }
    public Boolean attachmentAlert() { return attachmentAlert; }
    public Boolean attachmentRequire() { return attachmentRequire; }
    public Boolean otp() { return otp; }
    public Boolean restore() { return restore; }
    public String phone() { return phone; }
    public Boolean returnToHomeAfterSign() { return returnToHomeAfterSign; }
    public Boolean manager() { return manager; }
}
