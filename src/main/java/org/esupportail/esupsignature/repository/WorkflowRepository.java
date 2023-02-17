package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.web.ws.json.JsonDtoWorkflow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WorkflowRepository extends CrudRepository<Workflow, Long> {
    List<Workflow> findAll();
    @Query("select w from Workflow w")
    List<JsonDtoWorkflow> findAllJson();
    Workflow findByName(String name);
    List<Workflow> findByFromCodeIsTrue();
    List<Workflow> findByCreateByEppn(String userEppn);
    List<Workflow> findDistinctByAuthorizedShareTypesIsNotNull();
    List<Workflow> findByManagerRole(String role);
    @Query(value = "select count(*) from workflow as w where w.name = :name", nativeQuery = true)
    Long countByName(String name);
    Long countByNameAndCreateByEppn(String name, String userEppn);
    @Query("select distinct w from Workflow w where w.publicUsage = true or :role member of w.roles order by w.name")
    List<Workflow> findAuthorizedForms(String role);
    @Query("select distinct w from Workflow w where w.id not in (select distinct f.workflow from Form f) and w.createBy.eppn = 'system'")
    List<Workflow> findNotInForm();
    List<Workflow> findWorkflowByManagersIn(List<String> emails);
    List<Workflow> findByViewersEppn(String userEppn);
    @Query("select w from Workflow w where w.id = :id")
    JsonDtoWorkflow getByIdJson(Long id);
}
