package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {

    Page<SignBook> findByCreateByEppn(String userEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers v " +
            "join sb.signRequests sr " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "where (sb.title like :workflowFilter or sb.liveWorkflow.title like :workflowFilter)" +
            "and (sb.name like :docTitleFilter or sb.title like :docTitleFilter or sr.title like :docTitleFilter or sb.liveWorkflow.title like :docTitleFilter)" +
            "and (:userEppn in r.user.eppn  or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'")
    Page<SignBook> findByRecipientAndCreateByEppn(String userEppn, String workflowFilter, String docTitleFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers v " +
            "join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where lw.workflow is null " +
            "and (sb.title is null or sb.title = '') " +
            "and (sb.liveWorkflow.title is null or sb.liveWorkflow.title = '') " +
            "and :recipientUserEppn in (u.eppn) " +
            "and (:userEppn in (key(rhs).user.eppn) or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'")
    Page<SignBook> findByRecipientAndCreateByEppnAndTitleNull(String recipientUserEppn, String userEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers v " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where (sb.title like :workflowFilter or sb.liveWorkflow.title like :workflowFilter)" +
            "and (sb.name like :docTitleFilter or sb.title like :docTitleFilter or sr.title like :docTitleFilter or sb.liveWorkflow.title like :docTitleFilter)" +
            "and :recipientUserEppn in (u.eppn) " +
            "and (:userEppn in (key(rhs).user.eppn)  or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'")
    Page<SignBook> findByRecipientAndCreateByEppn(String recipientUserEppn, String userEppn, String workflowFilter, String docTitleFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where sb.status = 'pending' and size(sb.signRequests) > 0 and r.user.eppn = :userEppn")
    Page<SignBook> findToSign(String userEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Page<SignBook> findEmpty(String userEppn, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Long countEmpty(String userEppn);

    @Query("select distinct sb from SignBook sb " +
            "join sb.signRequests sr " +
            "join sr.recipientHasSigned rhs " +
            "where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType")
    Page<SignBook> findByRecipientAndActionType(String recipientUserEppn, ActionType actionType, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.signRequests sr join sr.recipientHasSigned rhs where key(rhs).user.eppn = :recipientUserEppn")
    Page<SignBook> findByRecipient(String recipientUserEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.hidedBy hb where hb.eppn = :hidedByEppn")
    Page<SignBook> findByHidedByEppn(String hidedByEppn, Pageable pageable);

    Page<SignBook> findByCreateByEppnAndStatusAndSignRequestsNotNull(String userEppn, SignRequestStatus status, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow = :workflow order by sb.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(sb) from SignBook sb where sb.liveWorkflow.workflow = :workflow")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    List<SignBook> findByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus);

    @Query("select sb from SignBook sb join sb.viewers u where u.eppn = :userEppn")
    Page<SignBook> findByViewersContaining(String userEppn, Pageable pageable);

    @Query("select distinct sb.title from SignBook sb join sb.signRequests sr left join sr.recipientHasSigned rhs where sb.title is not null and sb.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findDocTitles(String userEppn);

    @Query("select distinct sb.name from SignBook sb join sb.signRequests sr left join sr.recipientHasSigned rhs where sb.name is not null and sb.name <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findDocNames(String userEppn);

    @Query("select distinct sr.title from SignBook sb join sb.signRequests sr left join sr.recipientHasSigned rhs where sr.title is not null and sr.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findSignRequestTitles(String userEppn);

    @Query("select distinct lw.title from SignBook sb join sb.liveWorkflow lw join sb.signRequests sr left join sr.recipientHasSigned rhs where lw.title is not null and lw.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findLiveWorkflowTitles(String userEppn);

    @Query("select distinct w.description from SignBook sb join sb.liveWorkflow lw join lw.workflow w join sb.signRequests sr left join sr.recipientHasSigned rhs where w.description is not null and w.description <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findWorkflowTitles(String userEppn);

    @Query("select distinct sb.title from SignBook sb join sb.liveWorkflow lw left join lw.workflow w join sb.signRequests sr left join sr.recipientHasSigned rhs " +
            "where (lw.title is null or lw.title = '') and sb.title is not null and sb.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findSignBookTitles(String userEppn);

//    @Query("select distinct r from Recipient r " +
//            "left join LiveWorkflowStep lws on r member lws.recipients " +
//            "left join LiveWorkflow lw on lws member lw.liveWorkflowSteps " +
//            "left join SignBook sb on lw = sb.liveWorkflow " +
//            "left join sb.viewers v " +
//            "where (sb.createBy.eppn = :userEppn or :userEppn in v.eppn)")
//    List<Recipient> findRecipientNames(String userEppn);

    @Query("select distinct u from SignBook sb " +
            "left join sb.viewers v " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where (:userEppn in (key(rhs).user.eppn) or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.status <> 'deleted'")
    List<User> findRecipientNames(String userEppn);

}

