package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WorkflowStepRepository extends CrudRepository<WorkflowStep, Long>  {
    @Query("select w from WorkflowStep w where :recipientId = key(w.recipients)")
    List<WorkflowStep> findByRecipients(Long recipientId);
}
