package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.chart.WorkflowStatusChartDto;
import org.esupportail.esupsignature.dto.export.WorkflowDatasCsvDto;
import org.esupportail.esupsignature.dto.json.WorkflowDto;
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
    @Query("select w from Workflow w where w.id = :idLong or w.token = :idStr")
    Workflow findByIdOrToken(Long idLong, String idStr);
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

    @Query("select w from Workflow w where w.id = :idLong or w.token = :idStr")
    WorkflowDto getByIdJson(Long idLong, String idStr);

    @Query(value = """
        select distinct
            sb.id as signBookId,
            array_agg(distinct sbsr.id) as workflowDatasSignRequestIds,
            array_agg(distinct sbsr.title) as workflowDatasSignRequestTitles,
            sb.status as signBookStatus,
            cb.eppn as signBookCreateBy,
            sb.create_date as signBookCreateDate,
            sb.update_date as completedDate,
            sb.update_by as completedBy,
            lw.current_step_id as currentStepId,
            ws.description as currentStepDescription,
            array_agg((select email from user_account as u where u.id in (select user_id from recipient as r where r.id in (rhs.recipient_has_signed_key)))) as workflowDatasStepsRecipientsEmails,
            array_agg((select action_type from action as a where a.id in (rhs.recipient_has_signed_id))) as workflowDatasStepsActionsTypes,
            array_agg((select date from action as a where a.id in (rhs.recipient_has_signed_id))) as workflowDatasStepsActionsDates
        from sign_book sb
             join sign_request sbsr on sbsr.parent_sign_book_id = sb.id
             join public.sign_request_recipient_has_signed rhs on rhs.sign_request_id = sbsr.id
             join user_account cb on cb.id = sb.create_by_id
             join live_workflow lw on lw.id = sb.live_workflow_id
             join live_workflow_step lws on lws.id = lw.current_step_id
             join workflow_step ws on ws.id = lws.workflow_step_id
             join workflow w on w.id = lw.workflow_id
        where w.id = :id and sb.deleted is not true
        group by sb.id, sb.status, cb.eppn, sb.create_date, sb.update_date, sb.update_by, lw.current_step_id, ws.description
    """, nativeQuery = true)
    List<WorkflowDatasCsvDto> findWorkflowDatas(Long id);

    @Query("""
            select sb.status as status, count(sb.status) as count from SignBook sb
            where sb.liveWorkflow.workflow.id = :id and sb.deleted is not true group by sb.status
            """)
    List<WorkflowStatusChartDto> findWorkflowStatusCount(Long id);

}
