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

import java.util.Date;
import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {

    List<SignBook> findBySubject(String subject);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.team team
            where team = :user
            """)
    List<SignBook> findByTeamContaining(User user);

    @Query("""
            select distinct sb from SignBook sb
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject like concat('%', :docTitleFilter, '%'))
            and size(sb.signRequests) > 0
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (:statusFilter is null or sb.status = :statusFilter)
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findSignBooksAllPaged(SignRequestStatus statusFilter, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb left join sb.team team
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            where :user in (team)
            and (sb.status != 'pending' or key(rhs).user = :user or sb.createBy = :user)
            and (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject = :docTitleFilter)
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (sb.createBy = :user or sb.status <> 'draft')
            and size(sb.signRequests) > 0
            and :user not member of sb.hidedBy
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.team team
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject = :docTitleFilter)
            and (:recipientUser is null or key(rhs).user = :recipientUser or :recipientUser in (u))
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (:statusFilter is null or sb.status = :statusFilter)
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and size(sb.signRequests) > 0
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findByWorkflowName(User recipientUser, SignRequestStatus statusFilter, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select distinct sb.subject from SignBook sb
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<String> findByWorkflowNameSubjects(String workflowFilter);

    @Query("""
            select distinct sb.createBy from SignBook sb
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            """)
    List<User> findByWorkflowNameCreators(String workflowFilter);

    @Query("""
            select distinct u from SignBook sb
            left join sb.team team
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where (:workflowFilter is null or sb.workflowName = :workflowFilter) and u is not null
            """)
    List<User> findByWorkflowNameRecipientsUsers(String workflowFilter);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.team team
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where :user in (team)
            and (sb.status != 'pending' or key(rhs).user = :user or :user in (u) or sb.createBy = :user)
            and (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject = :docTitleFilter)
            and (:recipientUser is null or key(rhs).user = :recipientUser or :recipientUser in (u))
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (sb.createBy = :user or sb.status <> 'draft')
            and size(sb.signRequests) > 0
            and :user not member of sb.hidedBy
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User recipientUser, User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.liveWorkflow.currentStep.recipients r
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            where sb.status = 'pending' and size(sb.signRequests) > 0 and r.user = :user
            and rhs.actionType = 'none' and key(rhs).user = :user
            and (sb.deleted is null or sb.deleted != true)
            and :user not member of sb.hidedBy
            and (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject = :docTitleFilter)
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findToSign(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select distinct count(sb) from SignBook sb
            left join sb.liveWorkflow.currentStep.recipients r
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            where sb.status = 'pending' and size(sb.signRequests) > 0 and r.user = :user
            and rhs.actionType = 'none' and key(rhs).user = :user
            and (sb.deleted is null or sb.deleted != true)
            and :user not member of sb.hidedBy
            """)
    Long countToSign(User user);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Page<SignBook> findEmpty(User user, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Long countEmpty(User user);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            where key(rhs).user = :user and rhs.actionType = :actionType
            and (sb.workflowName = :workflowFilter or :workflowFilter is null)
            and (sb.subject = :docTitleFilter or :docTitleFilter is null)
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and :user not member of sb.hidedBy
            and (sb.createBy = :creatorFilter or :creatorFilter is null)
            """)
    Page<SignBook> findByRecipientAndActionTypeNotDeleted(User user, ActionType actionType, String workflowFilter, String docTitleFilter, User creatorFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.hidedBy hb where hb = :hidedBy")
    Page<SignBook> findByHidedById(User hidedBy, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status = :status
            and (sb.deleted is null or sb.deleted != true)
            """)
    Page<SignBook> findByCreateByIdAndStatusAndSignRequestsNotNull(User user, SignRequestStatus status, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and (sb.status = 'completed'
            or sb.status = 'archived'
            or sb.status = 'cleaned')
            and (sb.deleted is null or sb.deleted != true)
            """)
    Page<SignBook> findCompleted(User user, Pageable pageable);

    @Query("""
            select distinct sb from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.deleted = true
            """)
    Page<SignBook> findByCreateByIdDeleted(User user, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    List<SignBook> findByDeletedIsTrue();

    List<SignBook> findByStatusAndLiveWorkflowTargetsNotEmpty(SignRequestStatus signRequestStatus);

    Page<SignBook> findByStatus(SignRequestStatus signRequestStatus, Pageable pageable);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow.id = :workflowId and sb.status != 'uploading'")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select sb from SignBook sb where size(sb.signRequests) = 0 and sb.status != 'uploading'")
    List<SignBook> findEmpties();

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow = :workflow and (sb.hidedBy) is empty order by sb.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(sb) from SignBook sb where sb.liveWorkflow.workflow = :workflow and (sb.hidedBy) is empty")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select sb from SignBook sb join sb.viewers u where u = :user")
    Page<SignBook> findByViewersContaining(User user, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.viewers u where u.eppn = :userEppn and sb.status = 'pending'")
    Long countByViewersContaining(String userEppn);

    @Query("""
            select distinct sb.workflowName from SignBook sb
            left join sb.team team
            where (team = :user)
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<String> findAllWorkflowNames(User user);

    @Query("select distinct sb.workflowName from SignBook sb where sb.hidedBy is empty and sb.workflowName != ''")
    List<String> findAllWorkflowNames();

    @Query("""
            select distinct sb.subject from SignBook sb
            left join sb.team team
            where (team = :user)
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<String> findSubjects(User user);

    @Query("""
            select distinct sb.createBy from SignBook sb
            left join sb.team team
            where (sb.workflowName = :workflowFilter or :workflowFilter is null)
            and (sb.subject = :docTitleFilter or :docTitleFilter is null)
            and (team = :user)
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and (sb.createBy = :creatorFilter or :creatorFilter is null)
            """)
    List<User> findUserByRecipientAndCreateBy(User user, String workflowFilter, String docTitleFilter, User creatorFilter);

    Page<SignBook> findAll(Pageable pageable);

    @Query("""
            select distinct u from SignBook sb
            left join sb.team team
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where (team = :user)
            and :user not member of sb.hidedBy
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<User> findRecipientNames(User user);

    @Query("""
            select distinct sb from SignBook as sb
            left join sb.signRequests sr
            left join sr.recipientHasSigned rhs
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where sr.id in (select l.signRequestId from Log as l where l.eppn = :eppn and l.eppn != l.eppnFor)
            and (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or sb.subject = :docTitleFilter)
            and (:recipientUser is null or key(rhs).user = :recipientUser or :recipientUser in (u))
            and (sb.createDate between :startDateFilter and :endDateFilter)
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            """)
    Page<SignBook> findOnShareByEppn(String eppn, User recipientUser, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    List<SignBook> findByCreateByEppn(String userEppn);
}

