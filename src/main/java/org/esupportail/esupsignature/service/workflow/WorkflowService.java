package org.esupportail.esupsignature.service.workflow;

import org.esupportail.esupsignature.entity.Workflow;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowService {

	@Resource
	private List<Workflow> workflows;
	
	public List<String> getWorkflowNames() {
		List<String> woStrings = new ArrayList<String>();
		
		for(Workflow workflow : workflows ) {
			woStrings.add(workflow.getName());
		}
		return woStrings;
	}
	
	public List<Workflow> getWorkflows() {
		return workflows;
	}

	public Workflow getWorkflowByName(String name) {
		for(Workflow workflow : workflows ) {
			if(workflow.getName().equals(name)) {
				return workflow;
			}
		}
		return null;
	}
	
	public Workflow getWorkflowByClassName(String className) {
		for(Workflow workflow : workflows ) {
			if(workflow.getClass().getSimpleName().equals(className)) {
				return workflow;
			}
		}
		return null;
	}
	
}
