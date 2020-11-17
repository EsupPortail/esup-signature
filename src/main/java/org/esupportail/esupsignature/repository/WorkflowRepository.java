package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.custom.WorkflowRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowRepository extends CrudRepository<Workflow, Long>, WorkflowRepositoryCustom {
    List<Workflow> findAll();
    Workflow findByName(String name);
    List<Workflow> findByFromCodeIsTrue();
    List<Workflow> findByCreateBy(User user);
    @Query("select w from Workflow w join w.managers m where m = :email")
    List<Workflow> findByManagersContains(@Param("email") String email);
    List<Workflow> findDistinctByAuthorizedShareTypesIsNotNull();
    Long countByName(String name);
}
