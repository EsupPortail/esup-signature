package org.esupportail.esupsignature.dto.page.user.signrequest;

import eu.europa.esig.dss.validation.reports.Reports;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;
import java.util.Set;

public class ShowSignRequestContextDto {

    private String userEppn;
    private String authUserEppn;
    private boolean otpView;
    private Long workflowId;
    private SignType currentStepSignType;
    private ShowSignRequestDto showSignRequest;
    private SignUiFrontDto signUiFront;
    private SignRequest signRequest;
    private SignBook signBook;
    private LiveWorkflow liveWorkflow;
    private LiveWorkflowStep currentStep;
    private Workflow workflow;
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
    private Boolean stepRepeatable;
    private int nbSignRequestInSignBookParent;
    private boolean lastStep;
    private List<Document> toSignDocuments;
    private Document toSignDocument;
    private boolean pdf;
    private Reports reports;
    private List<String> signatureIds;
    private boolean signatureIssue;
    private List<Field> fields;
    private List<SignRequestParams> currentSignRequestParamses;
    private List<Comment> comments;
    private List<SignRequestParams> spots;
    private List<String> signImages;
    private String signImagesWarningMessage;
    private User frontUser;
    private User frontAuthUser;
    private String action;
    private Set<String> supervisors;
    private boolean notSigned;

    public String getUserEppn() { return userEppn; }
    public void setUserEppn(String userEppn) { this.userEppn = userEppn; }
    public String getAuthUserEppn() { return authUserEppn; }
    public void setAuthUserEppn(String authUserEppn) { this.authUserEppn = authUserEppn; }
    public boolean isOtpView() { return otpView; }
    public void setOtpView(boolean otpView) { this.otpView = otpView; }
    public Long getWorkflowId() { return workflowId; }
    public void setWorkflowId(Long workflowId) { this.workflowId = workflowId; }
    public SignType getCurrentStepSignType() { return currentStepSignType; }
    public void setCurrentStepSignType(SignType currentStepSignType) { this.currentStepSignType = currentStepSignType; }
    public ShowSignRequestDto getShowSignRequest() { return showSignRequest; }
    public void setShowSignRequest(ShowSignRequestDto showSignRequest) { this.showSignRequest = showSignRequest; }
    public SignUiFrontDto getSignUiFront() { return signUiFront; }
    public void setSignUiFront(SignUiFrontDto signUiFront) { this.signUiFront = signUiFront; }
    public SignRequest getSignRequest() { return signRequest; }
    public void setSignRequest(SignRequest signRequest) { this.signRequest = signRequest; }
    public SignBook getSignBook() { return signBook; }
    public void setSignBook(SignBook signBook) { this.signBook = signBook; }
    public LiveWorkflow getLiveWorkflow() { return liveWorkflow; }
    public void setLiveWorkflow(LiveWorkflow liveWorkflow) { this.liveWorkflow = liveWorkflow; }
    public LiveWorkflowStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(LiveWorkflowStep currentStep) { this.currentStep = currentStep; }
    public Workflow getWorkflow() { return workflow; }
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }
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
    public void setCurrentStepSingleSignWithAnnotation(boolean currentStepSingleSignWithAnnotation) { this.currentStepSingleSignWithAnnotation = currentStepSingleSignWithAnnotation; }
    public SignLevel getCurrentStepMinSignLevel() { return currentStepMinSignLevel; }
    public void setCurrentStepMinSignLevel(SignLevel currentStepMinSignLevel) { this.currentStepMinSignLevel = currentStepMinSignLevel; }
    public SignLevel getCurrentStepMaxSignLevel() { return currentStepMaxSignLevel; }
    public void setCurrentStepMaxSignLevel(SignLevel currentStepMaxSignLevel) { this.currentStepMaxSignLevel = currentStepMaxSignLevel; }
    public Boolean getStepRepeatable() { return stepRepeatable; }
    public void setStepRepeatable(Boolean stepRepeatable) { this.stepRepeatable = stepRepeatable; }
    public int getNbSignRequestInSignBookParent() { return nbSignRequestInSignBookParent; }
    public void setNbSignRequestInSignBookParent(int nbSignRequestInSignBookParent) { this.nbSignRequestInSignBookParent = nbSignRequestInSignBookParent; }
    public boolean isLastStep() { return lastStep; }
    public void setLastStep(boolean lastStep) { this.lastStep = lastStep; }
    public List<Document> getToSignDocuments() { return toSignDocuments; }
    public void setToSignDocuments(List<Document> toSignDocuments) { this.toSignDocuments = toSignDocuments; }
    public Document getToSignDocument() { return toSignDocument; }
    public void setToSignDocument(Document toSignDocument) { this.toSignDocument = toSignDocument; }
    public boolean isPdf() { return pdf; }
    public void setPdf(boolean pdf) { this.pdf = pdf; }
    public Reports getReports() { return reports; }
    public void setReports(Reports reports) { this.reports = reports; }
    public List<String> getSignatureIds() { return signatureIds; }
    public void setSignatureIds(List<String> signatureIds) { this.signatureIds = signatureIds; }
    public boolean isSignatureIssue() { return signatureIssue; }
    public void setSignatureIssue(boolean signatureIssue) { this.signatureIssue = signatureIssue; }
    public List<Field> getFields() { return fields; }
    public void setFields(List<Field> fields) { this.fields = fields; }
    public List<SignRequestParams> getCurrentSignRequestParamses() { return currentSignRequestParamses; }
    public void setCurrentSignRequestParamses(List<SignRequestParams> currentSignRequestParamses) { this.currentSignRequestParamses = currentSignRequestParamses; }
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public List<SignRequestParams> getSpots() { return spots; }
    public void setSpots(List<SignRequestParams> spots) { this.spots = spots; }
    public List<String> getSignImages() { return signImages; }
    public void setSignImages(List<String> signImages) { this.signImages = signImages; }
    public String getSignImagesWarningMessage() { return signImagesWarningMessage; }
    public void setSignImagesWarningMessage(String signImagesWarningMessage) { this.signImagesWarningMessage = signImagesWarningMessage; }
    public User getFrontUser() { return frontUser; }
    public void setFrontUser(User frontUser) { this.frontUser = frontUser; }
    public User getFrontAuthUser() { return frontAuthUser; }
    public void setFrontAuthUser(User frontAuthUser) { this.frontAuthUser = frontAuthUser; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Set<String> getSupervisors() { return supervisors; }
    public void setSupervisors(Set<String> supervisors) { this.supervisors = supervisors; }
    public boolean isNotSigned() { return notSigned; }
    public void setNotSigned(boolean notSigned) { this.notSigned = notSigned; }

}

