package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {

    List<SignBook> findBySubject(String subject);

    @Query("select distinct sb from SignBook sb " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and size(sb.signRequests) > 0 " +
            "and sb.createBy.email like :creatorFilter " +
            "and (sb.status = :statusFilter or :statusFilter is null) " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findSignBooksAllPaged(String statusFilter, String workflowFilter, String docTitleFilter, String creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers v " +
            "join sb.signRequests sr " +
            "join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and ((key(rhs).user.eppn = :userEppn) or (:userEppn in r.user.eppn and lws.id = sb.liveWorkflow.id) or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted' " +
            "and sb.createBy.eppn like :creatorFilter " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findByRecipientAndCreateByEppn(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb.createBy from SignBook sb " +
            "left join sb.viewers v " +
            "join sb.signRequests sr " +
            "join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and ((key(rhs).user.eppn = :userEppn) or (:userEppn in r.user.eppn and lws.id = sb.liveWorkflow.id) or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'" +
            "and sb.createBy.eppn like :creatorFilter")
    List<User> findUserByRecipientAndCreateByEppn(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter);

    @Query("select distinct sb.createBy from SignBook sb " +
            "left join sb.viewers v " +
            "join sb.signRequests sr " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'" +
            "and sb.createBy.eppn like :creatorFilter")
    List<User> findSignBookAllUserByRecipientAndCreateByEppn( String workflowFilter, String docTitleFilter, String creatorFilter);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers v " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and :recipientUserEppn in (u.eppn) " +
            "and ((key(rhs).user.eppn = :userEppn) or (:userEppn in r.user.eppn and lws.id = sb.liveWorkflow.id) or sb.createBy.eppn = :userEppn or v.eppn = :userEppn) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted' " +
            "and sb.createBy.eppn like :creatorFilter " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findByRecipientAndCreateByEppn(String recipientUserEppn, String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r " +
            "where (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and sb.status = 'pending' and size(sb.signRequests) > 0 and r.user.eppn = :userEppn " +
            "and sb.createBy.eppn like :creatorFilter " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findToSign(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Page<SignBook> findEmpty(String userEppn, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Long countEmpty(String userEppn);

    @Query("select distinct sb from SignBook sb " +
            "join sb.signRequests sr " +
            "join sr.recipientHasSigned rhs " +
            "where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType")
    Page<SignBook> findByRecipientAndActionType(String recipientUserEppn, ActionType actionType, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "join sb.signRequests sr " +
            "join sr.recipientHasSigned rhs " +
            "where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType " +
            "and (sb.workflowName like :workflowFilter) " +
            "and (sb.subject like :docTitleFilter) " +
            "and sb.status <> 'deleted' " +
            "and sb.createBy.eppn like :creatorFilter")
    Page<SignBook> findByRecipientAndActionTypeNotDeleted(String recipientUserEppn, ActionType actionType, String workflowFilter, String docTitleFilter, String creatorFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.hidedBy hb where hb.eppn = :hidedByEppn")
    Page<SignBook> findByHidedByEppn(String hidedByEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "where sb.createBy.eppn = :userEppn " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status = :status")
    Page<SignBook> findByCreateByEppnAndStatusAndSignRequestsNotNull(String userEppn, SignRequestStatus status, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    Page<SignBook> findByStatus(SignRequestStatus signRequestStatus, Pageable pageable);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow = :workflow order by sb.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(sb) from SignBook sb where sb.liveWorkflow.workflow = :workflow")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select sb from SignBook sb join sb.viewers u where u.eppn = :userEppn")
    Page<SignBook> findByViewersContaining(String userEppn, Pageable pageable);

    @Query("select distinct sb.workflowName from SignBook sb join sb.signRequests sr left join sr.recipientHasSigned rhs where (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    List<String> findWorkflowNames(String userEppn);

    @Query("select distinct workflowName from SignBook")
    List<String> findWorkflowNames();

    @Query("select distinct sb.subject from SignBook sb join sb.signRequests sr left join sr.recipientHasSigned rhs where (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    List<String> findSubjects(String userEppn);

    @Query("select distinct subject from SignBook")
    List<String> findSubjects();

    @Query("select distinct sb.title from SignBook sb join sb.liveWorkflow lw left join lw.workflow w join sb.signRequests sr left join sr.recipientHasSigned rhs " +
            "where (lw.title is null or lw.title = '') and sb.title is not null and sb.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
    Collection<String> findSignBookTitles(String userEppn);

    Page<SignBook> findAll(Pageable pageable);

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

