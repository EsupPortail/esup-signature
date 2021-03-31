package org.esupportail.esupsignature.web.ws.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties
public class JsonWorkflowStep {

	private Long workflowId;

	private Integer stepNumber;

	private String description;

	private List<String> recipientsEmails = new ArrayList<>();

	private List<String> names = new ArrayList<>();

	private List<String> firstnames = new ArrayList<>();

	private List<String> phones = new ArrayList<>();

	private Boolean changeable = false;

	private Integer signLevel = 0;

	private String signType = "visa";

	private Boolean allSignToComplete = false;

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

	public List<String> getRecipientsEmails() {
		return recipientsEmails;
	}

	public void setRecipientsEmails(List<String> recipientsEmails) {
		this.recipientsEmails = recipientsEmails;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}

	public List<String> getFirstnames() {
		return firstnames;
	}

	public void setFirstnames(List<String> firstnames) {
		this.firstnames = firstnames;
	}

	public List<String> getPhones() {
		return phones;
	}

	public void setPhones(List<String> phones) {
		this.phones = phones;
	}

	public Boolean getChangeable() {
		return changeable;
	}

	public void setChangeable(Boolean changeable) {
		this.changeable = changeable;
	}

	public String getSignType() {
		return signType;
	}

	public void setSignType(String signType) {
		this.signType = signType;
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
