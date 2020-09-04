package org.esupportail.esupsignature.repository.custom;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;

import java.util.List;


public interface WorkflowRepositoryCustom {

    List<Workflow> findAutorizedWorkflowByUser(User user);


}
