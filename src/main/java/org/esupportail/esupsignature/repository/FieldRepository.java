package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FieldRepository extends CrudRepository<Field, Long> {
    List<Field> findByWorkflowStepsContains(WorkflowStep workflowStep);
}
