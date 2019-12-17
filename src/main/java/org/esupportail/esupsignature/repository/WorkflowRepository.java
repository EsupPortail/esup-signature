package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Workflow;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WorkflowRepository extends CrudRepository<Workflow, Long> {
    List<Workflow> findByName(String name);
    Long countByName(String name);
    List<Workflow> findByExternal(Boolean external);
}
