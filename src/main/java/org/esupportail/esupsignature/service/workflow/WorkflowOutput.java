package org.esupportail.esupsignature.service.workflow;

import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import java.util.ArrayList;
import java.util.List;

public class WorkflowOutput {

	@ElementCollection(fetch = FetchType.EAGER)
	private List<String> targetEmails = new ArrayList<String>();

	private Boolean changeable = false;

	public List<String> getTargetEmails() {
		return targetEmails;
	}

	public void setTargetEmails(List<String> targetEmails) {
		this.targetEmails = targetEmails;
	}

	public Boolean getChangeable() {
		return changeable;
	}

	public void setChangeable(Boolean changeable) {
		this.changeable = changeable;
	}
}
