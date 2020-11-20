package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Workflow;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LiveWorkflowRepository extends CrudRepository<LiveWorkflow, Long> {
    public List<LiveWorkflow> findByWorkflow(Workflow workflow);
}
