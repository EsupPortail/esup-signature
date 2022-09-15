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

    @Query("select distinct sb from SignBook sb " +
            "left join sb.team team " +
            "where team = :user ")
    List<SignBook> findByTeamContaining(User user);

    @Query("select distinct sb from SignBook sb " +
            "where (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
            "and size(sb.signRequests) > 0 " +
            "and (sb.createBy = :creatorFilter or :creatorFilter is null)" +
            "and (sb.status = :statusFilter or :statusFilter is null) " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findSignBooksAllPaged(String statusFilter, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

//    @Query("select distinct sb from SignBook sb " +
//            "left join sb.viewers viewer " +
//            "left join sb.signRequests sr " +
//            "left join sr.recipientHasSigned rhs " +
//            "left join sb.liveWorkflow lw " +
//            "left join lw.liveWorkflowSteps lws " +
//            "left join lws.recipients r " +
//            "where (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
//            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
//            "and ((key(rhs).user = :user) or (:user in r.user and lws.id = sb.liveWorkflow.id) or sb.createBy = :user or viewer = :user) " +
//            "and sb.hidedBy is empty " +
//            "and size(sb.signRequests) > 0 " +
//            "and sb.status <> 'deleted' " +
//            "and (sb.createBy = :creatorFilter or :creatorFilter is null)" +
//            "and (sb.createDate between :startDateFilter and :endDateFilter)")
//    Page<SignBook> findByRecipientAndCreateByEppnOld(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.team team " +
            "where team = :user " +
            "and (:workflowFilter is null or sb.workflowName = :workflowFilter) " +
            "and (:docTitleFilter is null or sb.subject = :docTitleFilter) " +
            "and (:creatorFilter is null or sb.createBy = :creatorFilter)" +
            "and size(sb.signRequests) > 0 " +
            "and sb.hidedBy is empty " +
            "and sb.status <> 'deleted' " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.team team " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where :user in team " +
            "and (:workflowFilter is null or sb.workflowName = :workflowFilter) " +
            "and (:docTitleFilter is null or sb.subject = :docTitleFilter) " +
            "and (:recipientUser is null or key(rhs).user = :recipientUser or :recipientUser in (u)) " +
            "and (:creatorFilter is null or sb.createBy = :creatorFilter) " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted' " +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User recipientUser, User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.viewers viewer " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "where ((key(rhs).user = :user) or (:user in r.user and lw.id = sb.liveWorkflow.id and lws.id in (select id from LiveWorkflowStep where id = lw.id)) or sb.createBy = :user or viewer = :user)")
    List<SignBook> findByUserForUpdate(User user);

    @Query("select distinct sb.createBy from SignBook sb " +
            "left join sb.team team " +
            "where (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
            "and (team = :user) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'" +
            "and (sb.createBy = :creatorFilter or :creatorFilter is null)")
    List<User> findUserByRecipientAndCreateBy(User user, String workflowFilter, String docTitleFilter, User creatorFilter);

    @Query("select distinct sb.createBy from SignBook sb " +
            "where (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status <> 'deleted'" +
            "and (sb.createBy = :creatorFilter or :creatorFilter is null)")
    List<User> findSignBookAllUserByRecipientAndCreateBy(String workflowFilter, String docTitleFilter, User creatorFilter);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.liveWorkflow.currentStep.recipients r " +
            "where (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
            "and sb.status = 'pending' and size(sb.signRequests) > 0 and r.user = :user " +
            "and (sb.createBy = :creatorFilter or :creatorFilter is null)" +
            "and (sb.createDate between :startDateFilter and :endDateFilter)")
    Page<SignBook> findToSign(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Page<SignBook> findEmpty(User user, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Long countEmpty(User user);

//    @Query("select distinct sb from SignBook sb " +
//            "join sb.signRequests sr " +
//            "join sr.recipientHasSigned rhs " +
//            "where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType")
//    Page<SignBook> findByRecipientAndActionType(String recipientUserEppn, ActionType actionType, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "where key(rhs).user = :recipientUser and rhs.actionType = :actionType " +
            "and (sb.workflowName = :workflowFilter or :workflowFilter is null) " +
            "and (sb.subject = :docTitleFilter or :docTitleFilter is null) " +
            "and sb.status <> 'deleted' " +
            "and (sb.createBy = :creatorFilter or :creatorFilter is null)")
    Page<SignBook> findByRecipientAndActionTypeNotDeleted(User recipientUser, ActionType actionType, String workflowFilter, String docTitleFilter, User creatorFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.hidedBy hb where hb = :hidedBy")
    Page<SignBook> findByHidedById(User hidedBy, Pageable pageable);

    @Query("select distinct sb from SignBook sb " +
            "where sb.createBy = :user " +
            "and sb.hidedBy is empty " +
            "and size(sb.signRequests) > 0 " +
            "and sb.status = :status")
    Page<SignBook> findByCreateByIdAndStatusAndSignRequestsNotNull(User user, SignRequestStatus status, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    Page<SignBook> findByStatus(SignRequestStatus signRequestStatus, Pageable pageable);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow = :workflow order by sb.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(sb) from SignBook sb where sb.liveWorkflow.workflow = :workflow")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select sb from SignBook sb join sb.viewers u where u = :user")
    Page<SignBook> findByViewersContaining(User user, Pageable pageable);

    @Query("select distinct sb.workflowName from SignBook sb " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "where (key(rhs).user = :user or sb.createBy = :user) " +
            "and sb.hidedBy is empty")
    List<String> findWorkflowNames(User user);

    @Query("select distinct workflowName from SignBook")
    List<String> findWorkflowNames();

    @Query("select distinct sb.subject from SignBook sb " +
            "left join sb.signRequests sr " +
            "left join sr.recipientHasSigned rhs " +
            "where (key(rhs).user = :user or sb.createBy = :user) " +
            "and sb.hidedBy is empty")
    List<String> findSubjects(User user);

//    @Query("select distinct subject from SignBook")
//    List<String> findSubjects();
//
//    @Query("select distinct sb.title from SignBook sb join sb.liveWorkflow lw left join lw.workflow w join sb.signRequests sr left join sr.recipientHasSigned rhs " +
//            "where (lw.title is null or lw.title = '') and sb.title is not null and sb.title <> '' and (key(rhs).user.eppn = :userEppn or sb.createBy.eppn = :userEppn)")
//    Collection<String> findSignBookTitles(String userEppn);

    Page<SignBook> findAll(Pageable pageable);

    @Query("select distinct u from SignBook sb " +
            "left join sb.team team " +
            "left join sb.liveWorkflow lw " +
            "left join lw.liveWorkflowSteps lws " +
            "left join lws.recipients r " +
            "left join r.user u " +
            "where (team = :user) " +
            "and sb.hidedBy is empty " +
            "and sb.status <> 'deleted'")
    List<User> findRecipientNames(User user);

    List<SignBook> findByCreateByEppn(String userEppn);

}

