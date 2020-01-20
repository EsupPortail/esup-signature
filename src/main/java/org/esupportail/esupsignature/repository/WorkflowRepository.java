package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Workflow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowRepository extends CrudRepository<Workflow, Long> {
    List<Workflow> findByName(String name);
    List<Workflow> findByCreateBy(String eppn);
    @Query("select w from Workflow w join w.managers m where m = :email")
    List<Workflow> findByManagersContains(@Param("email") String email);
    Long countByName(String name);
    List<Workflow> findByExternal(Boolean external);
}
