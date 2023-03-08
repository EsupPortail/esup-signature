package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LiveWorkflowStepRepository extends CrudRepository<LiveWorkflowStep, Long> {
    List<LiveWorkflowStep> findByWorkflowStep(WorkflowStep workflowStep);
    Long countBySignRequestParamsContains(SignRequestParams signRequestParams);
}
