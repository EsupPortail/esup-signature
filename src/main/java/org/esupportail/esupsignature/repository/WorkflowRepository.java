package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.WorkflowDatasDto;
import org.esupportail.esupsignature.dto.WorkflowDto;
import org.esupportail.esupsignature.entity.Workflow;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface WorkflowRepository extends CrudRepository<Workflow, Long> {
    List<Workflow> findAll();
    @Query("select w from Workflow w")
    List<WorkflowDto> findAllJson();
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
    @Query("select distinct w from Workflow w where w.id not in (select distinct f.workflow.id from Form f where f.workflow is not null) and w.createBy.eppn = 'system'")
    List<Workflow> findNotInForm();
    @Query("select distinct w from Workflow w " +
            "where :email member of w.managers " +
            "or exists (select r from Workflow w2 join w2.dashboardRoles r where w = w2 and r in :roles)")
    List<Workflow> findWorkflowByManagersIn(String email, Set<String> roles);
    List<Workflow> findByViewersEppn(String userEppn);
    @Query("select w from Workflow w where w.id = :id")
    WorkflowDto getByIdJson(Long id);

    @Query("select distinct " +
            "sr.parentSignBook.id as signBookId, " +
            "sr.parentSignBook.signRequests as workflowDatasSignRequestDtos, " +
            "sr.status as signBookStatus, " +
            "sr.createBy.eppn as signBookCreateBy, " +
            "sr.createDate as signBookCreateDate, " +
            "sr.parentSignBook.updateDate as completedDate, " +
            "sr.parentSignBook.updateBy as completedBy, " +
            "sr.parentSignBook.liveWorkflow.currentStep.id as currentStepId, " +
            "sr.parentSignBook.liveWorkflow.currentStep.workflowStep.description as currentStepDescription, " +
            "key(rhs) as workflowDatasStepsRecipiensDtos, " +
            "value(rhs) as workflowDatasStepsActionsDtos " +
            "from SignRequest sr " +
            "join sr.parentSignBook.signRequests srs " +
            "join sr.recipientHasSigned rhs " +
            "where sr.parentSignBook.liveWorkflow.workflow.id = :id")
    List<WorkflowDatasDto> findWorkflowDatas(Long id);
}
