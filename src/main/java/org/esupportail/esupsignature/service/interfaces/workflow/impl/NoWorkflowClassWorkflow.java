package org.esupportail.esupsignature.service.interfaces.workflow.impl;

import org.esupportail.esupsignature.service.interfaces.workflow.ModelClassWorkflow;
import org.springframework.stereotype.Component;

@Component
public class NoWorkflowClassWorkflow extends ModelClassWorkflow {

	@Override
	public String getName() {
		return "NoWorkflowClassWorkflow";
	}

	@Override
	public String getDescription() {
		return "Pas de workflow";
	}

}
