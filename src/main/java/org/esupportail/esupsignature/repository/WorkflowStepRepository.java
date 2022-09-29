package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WorkflowStepRepository extends CrudRepository<WorkflowStep, Long>  {
    List<WorkflowStep> findByCertificatId(Long id);
}
