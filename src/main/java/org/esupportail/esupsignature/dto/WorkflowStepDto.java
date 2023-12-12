package org.esupportail.esupsignature.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties
public class WorkflowStepDto {

	private String title;

	private Long workflowId;

	private Integer stepNumber;

	private String description;

	private List<String> recipientsCCEmails = new ArrayList<>();

	private List<RecipientWsDto> recipients = new ArrayList<>();

	private Boolean changeable = false;

	private Integer signLevel = 0;

	private SignType signType = SignType.visa;

	private Boolean repeatable = false;

	private SignType repeatableSignType = SignType.visa;

	private Boolean allSignToComplete = false;

	private Boolean userSignFirst = false;

	private Boolean multiSign = true;

	private Boolean autoSign = false;

	private String comment;

	private Boolean attachmentRequire = false;

	private Integer maxRecipients = 99;

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

	public Boolean getChangeable() {
		return changeable;
	}

	public void setChangeable(Boolean changeable) {
		this.changeable = changeable;
	}

	public Integer getSignLevel() {
		return signLevel;
	}

	public void setSignLevel(Integer signLevel) {
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

	public Boolean getAutoSign() {
		return autoSign;
	}

	public void setAutoSign(Boolean autoSign) {
		this.autoSign = autoSign;
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

	public Integer getMaxRecipients() {
		return maxRecipients;
	}

	public void setMaxRecipients(Integer maxRecipients) {
		this.maxRecipients = maxRecipients;
	}
}
