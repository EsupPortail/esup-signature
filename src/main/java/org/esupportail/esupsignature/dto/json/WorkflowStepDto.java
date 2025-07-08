package org.esupportail.esupsignature.dto.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties
public class WorkflowStepDto {

	private String title;

	private Long workflowId;

	private Integer stepNumber = 1;

	private String description;

	private List<String> recipientsCCEmails = new ArrayList<>();

	private List<RecipientWsDto> recipients = new ArrayList<>();

	private List<SignRequestParamsWsDto> signRequestParams = new ArrayList<>();

	private Boolean changeable = null;

	private SignLevel signLevel = SignLevel.simple;

	private SignType signType;

	private Boolean repeatable = null;

	private SignType repeatableSignType = SignType.visa;

	private Boolean allSignToComplete;

	private Boolean userSignFirst = false;

	private Boolean multiSign;

	private Boolean singleSignWithAnnotation;

	private Boolean autoSign = false;

	private Boolean forceAllSign = false;

	private String comment;

	private Boolean attachmentRequire;

	private Boolean attachmentAlert;

	private Integer maxRecipients = 99;

	private List<String> targetEmails = new ArrayList<>();

	public WorkflowStepDto() {
	}

	public WorkflowStepDto(RecipientWsDto recipientWsDto) {
		this.recipients.add(recipientWsDto);
	}

	public WorkflowStepDto(SignType signType, String description, List<RecipientWsDto> recipients, Boolean changeable, Integer maxRecipients, Boolean allSignToComplete, Boolean attachmentRequire) {
		this.signType = signType;
		this.description = description;
		this.changeable = changeable;
		this.maxRecipients = maxRecipients;
		this.allSignToComplete = allSignToComplete;
		this.attachmentRequire = attachmentRequire;
		if(recipients != null) {
			this.recipients.addAll(recipients);
		}
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Long getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public Integer getStepNumber() {
		return stepNumber;
	}

	public void setStepNumber(Integer stepNumber) {
		this.stepNumber = stepNumber;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getRecipientsCCEmails() {
		return recipientsCCEmails;
	}

	public void setRecipientsCCEmails(List<String> recipientsEmails) {
		this.recipientsCCEmails = recipientsEmails;
	}

	public List<RecipientWsDto> getRecipients() {
		return recipients;
	}

	public void setRecipients(List<RecipientWsDto> recipients) {
		this.recipients = recipients;
	}

	public List<SignRequestParamsWsDto> getSignRequestParams() {
		return signRequestParams;
	}

	public void setSignRequestParams(List<SignRequestParamsWsDto> signRequestParams) {
		this.signRequestParams = signRequestParams;
	}

	public Boolean getChangeable() {
		return changeable;
	}

	public void setChangeable(Boolean changeable) {
		this.changeable = changeable;
	}

	public SignLevel getSignLevel() {
		return signLevel;
	}

	public void setSignLevel(SignLevel signLevel) {
		this.signLevel = signLevel;
	}

	public SignType getSignType() {
		return signType;
	}

	public void setSignType(SignType signType) {
		this.signType = signType;
	}

	public Boolean getRepeatable() {
		return repeatable;
	}

	public void setRepeatable(Boolean repeatable) {
		this.repeatable = repeatable;
	}

	public SignType getRepeatableSignType() {
		return repeatableSignType;
	}

	public void setRepeatableSignType(SignType repeatableSignType) {
		this.repeatableSignType = repeatableSignType;
	}

	public Boolean getAllSignToComplete() {
		return allSignToComplete;
	}

	public void setAllSignToComplete(Boolean allSignToComplete) {
		this.allSignToComplete = allSignToComplete;
	}

	public Boolean getUserSignFirst() {
		return userSignFirst;
	}

	public void setUserSignFirst(Boolean userSignFirst) {
		this.userSignFirst = userSignFirst;
	}

	public Boolean getMultiSign() {
		return multiSign;
	}

	public void setMultiSign(Boolean multiSign) {
		this.multiSign = multiSign;
	}

	public Boolean getSingleSignWithAnnotation() {
		return singleSignWithAnnotation;
	}

	public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) {
		this.singleSignWithAnnotation = singleSignWithAnnotation;
	}

	public Boolean getAutoSign() {
		return autoSign;
	}

	public void setAutoSign(Boolean autoSign) {
		this.autoSign = autoSign;
	}

	public Boolean getForceAllSign() {
		return forceAllSign;
	}

	public void setForceAllSign(Boolean forceAllSign) {
		this.forceAllSign = forceAllSign;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Boolean getAttachmentRequire() {
		return attachmentRequire;
	}

	public void setAttachmentRequire(Boolean attachmentRequire) {
		this.attachmentRequire = attachmentRequire;
	}

	public Boolean getAttachmentAlert() {
		return attachmentAlert;
	}

	public void setAttachmentAlert(Boolean attachmentAlert) {
		this.attachmentAlert = attachmentAlert;
	}

	public Integer getMaxRecipients() {
		return maxRecipients;
	}

	public void setMaxRecipients(Integer maxRecipients) {
		this.maxRecipients = maxRecipients;
	}

	public List<String> getTargetEmails() {
		return targetEmails;
	}

	public void setTargetEmails(List<String> targetEmails) {
		this.targetEmails = targetEmails;
	}
}
