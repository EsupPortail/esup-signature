package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

@Component
public class NoWorkflow extends DefaultWorkflow {

	@Override
	public String getName() {
		return "NoWorkflowClassWorkflow";
	}

	@Override
	public String getDescription() {
		return "Pas de workflow";
	}

}
