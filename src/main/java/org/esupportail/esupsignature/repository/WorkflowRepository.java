package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Workflow;
import org.hibernate.jdbc.Work;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowRepository extends CrudRepository<Workflow, Long> {
    List<Workflow> findAll();
    Workflow findByName(String name);
    List<Workflow> findByFromCodeIsTrue();
    List<Workflow> findByCreateByEppn(String userEppn);
    @Query("select w from Workflow w join w.managers m where m = :email")
    List<Workflow> findByManagersContains(@Param("email") String email);
    List<Workflow> findDistinctByAuthorizedShareTypesIsNotNull();
    List<Workflow> findByRolesIn(List<String> roles);
    Long countByName(String name);
    Long countByNameAndCreateByEppn(String name, String userEppn);
    @Query("select distinct w from Workflow w where w.publicUsage = true or (w.visibility = true and :role member of w.roles) order by w.name")
    List<Workflow> findAuthorizedForms(String role);
}
