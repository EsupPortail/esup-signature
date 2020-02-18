package org.esupportail.esupsignature.web.controller.ws.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties
public class JsonWorkflowStep {

	private Integer stepNumber;

	private String description;

	private List<String> recipientEmails = new ArrayList<>();

	private Boolean changeable = false;

	private Integer signLevel = 0;

	private Boolean allSignToComplete = false;

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

	public List<String> getRecipientEmails() {
		return recipientEmails;
	}

	public void setRecipientEmails(List<String> recipientEmails) {
		this.recipientEmails = recipientEmails;
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

	public Boolean getAllSignToComplete() {
		return allSignToComplete;
	}

	public void setAllSignToComplete(Boolean allSignToComplete) {
		this.allSignToComplete = allSignToComplete;
	}

}
