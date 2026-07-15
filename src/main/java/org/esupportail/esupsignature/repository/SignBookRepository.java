package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.projection.jpa.HomePostitItemProjection;
import org.esupportail.esupsignature.dto.projection.jpa.HomeSignRequestItemProjection;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepRecipientProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowTargetProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookListMetadataProjection;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookViewerProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.UserProjectionDto;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sb from SignBook sb where sb.id = :id")
    Optional<SignBook> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select distinct sb from SignBook sb
            left join fetch sb.liveWorkflow lw
            left join fetch lw.liveWorkflowSteps lws
            left join fetch lws.recipients r
            left join fetch r.user ru
            left join fetch lws.workflowStep ws
            where sb.id = :id
            """)
    Optional<SignBook> findByIdWithWizardContext(@Param("id") Long id);

    @Query("""
            select v.id as id,
                   v.firstname as firstname,
                   v.name as name,
                   v.email as email
            from SignBook sb
            join sb.viewers v
            where sb.id = :id
            order by lower(v.name), lower(v.firstname), v.id
            """)
    List<SignBookViewerProjectionDto> findViewerProjectionsById(@Param("id") Long id);

    @Query("""
            select t.targetUri as targetUri,
                   t.targetOk as targetOk
            from SignBook sb
            join sb.liveWorkflow lw
            join lw.targets t
            where sb.id = :id
            order by t.id
            """)
    List<LiveWorkflowTargetProjectionDto> findTargetProjectionsById(@Param("id") Long id);

    @Query("""
            select lws.id as id,
                   coalesce(lws.description, ws.description) as description,
                   coalesce(ws.changeable, false) as changeable,
                   lws.signType as signType,
                   lws.minSignLevel as minSignLevel,
                   coalesce(lws.autoSign, false) as autoSign,
                   coalesce(lws.allSignToComplete, false) as allSignToComplete,
                   coalesce(lws.repeatable, false) as repeatable,
                   coalesce(lws.sealVisa, false) as sealVisa
            from SignBook sb
            join sb.liveWorkflow lw
            join lw.liveWorkflowSteps lws
            left join lws.workflowStep ws
            where sb.id = :id
            order by index(lws)
            """)
    List<LiveWorkflowStepProjectionDto> findStepProjectionsById(@Param("id") Long id);

    @Query("""
            select lws.id as stepId,
                   r.id as recipientId,
                   r.signed as signed,
                   u.id as userId,
                   u.firstname as userFirstname,
                   u.name as userName,
                   u.email as userEmail,
                   u.phone as userPhone,
                   u.userType as userUserType
            from SignBook sb
            join sb.liveWorkflow lw
            join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where sb.id = :id
            order by index(lws), r.id
            """)
    List<LiveWorkflowStepRecipientProjectionDto> findStepRecipientProjectionsById(@Param("id") Long id);

    @Query(value = """
            select sbs.sign_book_id as signBookId,
                   (array_agg(sbs.sign_requests_id order by sbs.sign_requests_order))[1] as primarySignRequestId,
                   count(*) as signRequestCount
            from sign_book_sign_requests sbs
            where sbs.sign_book_id in (:ids)
            group by sbs.sign_book_id
            """, nativeQuery = true)
    List<SignBookListMetadataProjection> findListMetadataBySignBookIds(@Param("ids") Collection<Long> ids);

    @Query(value = """
            select sbs.sign_book_id as signBookId,
                   sr.id as signRequestId,
                   sr.title as title,
                   sr.status as status,
                   exists (
                       select 1
                       from sign_request_viewed_by srvb
                       join user_account viewed_user on viewed_user.id = srvb.viewed_by_id
                       where srvb.sign_request_id = sr.id
                         and viewed_user.eppn = :userEppn
                   ) as viewedByCurrentUser,
                   exists (
                       select 1
                       from sign_request_attachments sra
                       where sra.sign_request_id = sr.id
                   ) as hasAttachments,
                   exists (
                       select 1
                       from sign_request_recipient_has_signed rhs
                       join recipient recipient_key on recipient_key.id = rhs.recipient_has_signed_key
                       join user_account recipient_user on recipient_user.id = recipient_key.user_id
                       join action recipient_action on recipient_action.id = rhs.recipient_has_signed_id
                       where rhs.sign_request_id = sr.id
                         and recipient_user.eppn = :userEppn
                         and recipient_action.action_type = 'none'
                   ) as signableByCurrentUser,
                   (
                       select document.file_name
                       from sign_request_original_documents srod
                       join document document on document.id = srod.original_documents_id
                       where srod.sign_request_id = sr.id
                       order by srod.original_documents_order
                       limit 1
                   ) as firstOriginalFileName
            from sign_book_sign_requests sbs
            join sign_request sr on sr.id = sbs.sign_requests_id
            where sbs.sign_book_id in (:ids)
            order by sbs.sign_book_id, sbs.sign_requests_order
            """, nativeQuery = true)
    List<HomeSignRequestItemProjection> findHomeSignRequestItemsBySignBookIds(@Param("ids") Collection<Long> ids, @Param("userEppn") String userEppn);

    @Query(value = """
            select sbs.sign_book_id as signBookId,
                   author.firstname as authorFirstname,
                   author.name as authorName,
                   comment.text as text
            from sign_book_sign_requests sbs
            join sign_request_comments src on src.sign_request_id = sbs.sign_requests_id
            join comment comment on comment.id = src.comments_id
            left join user_account author on author.id = comment.create_by_id
            where sbs.sign_book_id in (:ids)
              and comment.is_postit = true
            order by sbs.sign_book_id, sbs.sign_requests_order, src.comments_order
            """, nativeQuery = true)
    List<HomePostitItemProjection> findHomePostitItemsBySignBookIds(@Param("ids") Collection<Long> ids);

    List<SignBook> findBySubject(String subject);

    @Query("""
            select distinct sb from SignBook sb
            left join sb.team team
            where team = :user
            """)
    List<SignBook> findByTeamContaining(User user);

    @Query("""
            select sb from SignBook sb
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
            and size(sb.signRequests) > 0
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (:statusFilter is null or :statusFilter = 'deleted' or sb.status = :statusFilter)
            and (:statusFilter is null or (sb.deleted is null and :statusFilter != 'deleted') or sb.deleted = :deleted or (:deleted is true and sb.status = 'deleted'))
            and (sb.createDate between :startDateFilter and :endDateFilter)
            """)
    Page<SignBook> findSignBooksAllPaged(SignRequestStatus statusFilter, Boolean deleted, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select sb from SignBook sb
            where sb.status <> 'deleted'
              and (sb.deleted is null or sb.deleted != true)
              and (sb.createDate between :startDateFilter and :endDateFilter)
              and :user member of sb.team
              and :user not member of sb.hidedBy
              and (:workflowFilter is null or sb.workflowName = :workflowFilter)
              and (:creatorFilter is null or sb.createBy = :creatorFilter)
              and (sb.createBy = :user or sb.status <> 'draft')
              and (:docTitleFilter is null
                   or lower(sb.subject) like :docTitleFilter escape '\\')
              and exists (
                  select 1 from SignRequest sr
                  where sr.parentSignBook = sb
              )
              and (
                  sb.status != 'pending'
                  or sb.createBy = :user
                  or exists (
                      select 1 from SignRequest sr
                      join sr.recipientHasSigned rhs
                      where sr.parentSignBook = sb
                        and key(rhs).user = :user
                  )
              )
        """)
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
           select sb from SignBook sb
           where sb.status <> 'deleted'
             and (sb.deleted is null or sb.deleted != true)
             and (sb.createDate between :startDateFilter and :endDateFilter)
             and :user member of sb.team
             and :user not member of sb.hidedBy
             and (:workflowFilter is null or sb.workflowName = :workflowFilter)
             and (:creatorFilter is null or sb.createBy = :creatorFilter)
             and (sb.createBy = :user or sb.status <> 'draft')
             and (:docTitleFilter is null\s
                  or lower(sb.subject) like :docTitleFilter escape '\\')
             and exists (
                 select 1 from SignRequest sr
                 where sr.parentSignBook = sb
             )
             and (
                 sb.status != 'pending'
                 or sb.createBy = :user
                 or exists (
                     select 1 from SignRequest sr
                     join sr.recipientHasSigned rhs
                     where sr.parentSignBook = sb
                       and key(rhs).user = :user
                 )
                 or exists (
                     select 1 from LiveWorkflowStep lws
                     join lws.recipients r
                     where lws = sb.liveWorkflow.currentStep
                       and r.user = :user
                 )
             )
             and (
                 :recipientUser is null
                 or exists (
                     select 1 from SignRequest sr
                     join sr.recipientHasSigned rhs
                     where sr.parentSignBook = sb
                       and key(rhs).user = :recipientUser
                 )
                 or exists (
                     select 1 from LiveWorkflowStep lws
                     join lws.recipients r
                     where lws = sb.liveWorkflow.currentStep
                       and r.user = :recipientUser
                 )
             )
           """)
    Page<SignBook> findByRecipientAndCreateByEppnIndexed(User recipientUser, User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
           select sb from SignBook sb
           where :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and (:workflowId is null or sb.liveWorkflow.workflow.id = :workflowId)
            and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (:statusFilter is null or :statusFilter = 'deleted' or sb.status = :statusFilter)
            and (:statusFilter is null or (sb.deleted is null and :statusFilter != 'deleted') or sb.deleted = :deleted or (:deleted = true and sb.status = 'deleted'))
            and (sb.createDate between :startDateFilter and :endDateFilter)
            and (
                :recipientUser is null
                or exists (
                    select 1 from SignRequest sr
                    join sr.recipientHasSigned rhs
                    where sr.parentSignBook = sb
                      and (key(rhs).user = :recipientUser)
                )
                or exists (
                    select 1 from LiveWorkflowStep lws
                    join lws.recipients r
                    where lws member of sb.liveWorkflow.liveWorkflowSteps
                    and r.user = :recipientUser
                )
            )
        """)
    Page<SignBook> findByWorkflowId(User recipientUser, SignRequestStatus statusFilter, Boolean deleted, Long workflowId, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable, User user);

    @Query("""
           select sb from SignBook sb
           where :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and (:workflowId is null or sb.liveWorkflow.workflow.id = :workflowId)
            and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            and (:statusFilter is null or :statusFilter = 'deleted' or sb.status = :statusFilter)
            and (:statusFilter is null or (sb.deleted is null and :statusFilter != 'deleted') or sb.deleted = :deleted or (:deleted = true and sb.status = 'deleted'))
            and (sb.createDate between :startDateFilter and :endDateFilter)
            and (
                :recipientUser is null
                or exists (
                    select 1 from SignRequest sr
                    join sr.recipientHasSigned rhs
                    where sr.parentSignBook = sb
                      and (key(rhs).user = :recipientUser)
                )
                or exists (
                    select 1 from LiveWorkflowStep lws
                    join lws.recipients r
                    where lws member of sb.liveWorkflow.liveWorkflowSteps
                    and r.user = :recipientUser
                )
            )
            order by
                case when :endDateSortAsc = true then coalesce(
                    sb.endDate,
                    (select max(signedDocument.createDate) from SignRequest signedRequest join signedRequest.signedDocuments signedDocument where signedRequest.parentSignBook = sb),
                    (select max(originalDocument.createDate) from SignRequest originalRequest join originalRequest.originalDocuments originalDocument where originalRequest.parentSignBook = sb),
                    sb.createDate
                ) end asc,
                case when :endDateSortAsc = false then coalesce(
                    sb.endDate,
                    (select max(signedDocument.createDate) from SignRequest signedRequest join signedRequest.signedDocuments signedDocument where signedRequest.parentSignBook = sb),
                    (select max(originalDocument.createDate) from SignRequest originalRequest join originalRequest.originalDocuments originalDocument where originalRequest.parentSignBook = sb),
                    sb.createDate
                ) end desc,
                sb.id desc
        """)
    Page<SignBook> findByWorkflowIdOrderByEffectiveEndDate(User recipientUser, SignRequestStatus statusFilter, Boolean deleted, Long workflowId, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable, User user, Boolean endDateSortAsc);

    @Query("""
            select sb from SignBook sb
            where (:workflowId is null or sb.liveWorkflow.workflow.id = :workflowId)
              and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
              and (:creatorFilter is null or sb.createBy = :creatorFilter)
              and (:statusFilter is null or :statusFilter = 'deleted' or sb.status = :statusFilter)
              and (:statusFilter is null or (sb.deleted is null and :statusFilter != 'deleted') or sb.deleted = :deleted or (:deleted = true and sb.status = 'deleted'))
              and size(sb.signRequests) > 0
              and :user member of sb.hidedBy
              and (sb.createDate between :startDateFilter and :endDateFilter)
              and (:recipientUser is null or exists (
                    select 1 from SignRequest sr
                    join sr.recipientHasSigned rhs
                    where sr.parentSignBook = sb
                      and key(rhs).user = :recipientUser
                  ) or exists (
                    select 1 from LiveWorkflowStep lws
                    join lws.recipients r
                    where lws in elements(sb.liveWorkflow.liveWorkflowSteps)
                      and r.user = :recipientUser
                  ))
            """)
    Page<SignBook> findByWorkflowIdHided(User recipientUser, SignRequestStatus statusFilter, Boolean deleted, Long workflowId, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable, User user);

    @Query("""
            select sb from SignBook sb
            where (:workflowId is null or sb.liveWorkflow.workflow.id = :workflowId)
              and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
              and (:creatorFilter is null or sb.createBy = :creatorFilter)
              and (:statusFilter is null or :statusFilter = 'deleted' or sb.status = :statusFilter)
              and (:statusFilter is null or (sb.deleted is null and :statusFilter != 'deleted') or sb.deleted = :deleted or (:deleted = true and sb.status = 'deleted'))
              and size(sb.signRequests) > 0
              and :user member of sb.hidedBy
              and (sb.createDate between :startDateFilter and :endDateFilter)
              and (:recipientUser is null or exists (
                    select 1 from SignRequest sr
                    join sr.recipientHasSigned rhs
                    where sr.parentSignBook = sb
                      and key(rhs).user = :recipientUser
                  ) or exists (
                    select 1 from LiveWorkflowStep lws
                    join lws.recipients r
                    where lws in elements(sb.liveWorkflow.liveWorkflowSteps)
                      and r.user = :recipientUser
                  ))
              order by
                  case when :endDateSortAsc = true then coalesce(
                      sb.endDate,
                      (select max(signedDocument.createDate) from SignRequest signedRequest join signedRequest.signedDocuments signedDocument where signedRequest.parentSignBook = sb),
                      (select max(originalDocument.createDate) from SignRequest originalRequest join originalRequest.originalDocuments originalDocument where originalRequest.parentSignBook = sb),
                      sb.createDate
                  ) end asc,
                  case when :endDateSortAsc = false then coalesce(
                      sb.endDate,
                      (select max(signedDocument.createDate) from SignRequest signedRequest join signedRequest.signedDocuments signedDocument where signedRequest.parentSignBook = sb),
                      (select max(originalDocument.createDate) from SignRequest originalRequest join originalRequest.originalDocuments originalDocument where originalRequest.parentSignBook = sb),
                      sb.createDate
                  ) end desc,
                  sb.id desc
            """)
    Page<SignBook> findByWorkflowIdHidedOrderByEffectiveEndDate(User recipientUser, SignRequestStatus statusFilter, Boolean deleted, Long workflowId, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable, User user, Boolean endDateSortAsc);

    @Query("""
            select distinct sb.subject from SignBook sb
            where (:workflowId is null or sb.liveWorkflow.workflow.id = :workflowId)
            and lower(sb.subject) like :searchString escape '\\'
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<String> findByWorkflowNameSubjects(Long workflowId, String searchString);

    @Query("""
        select sb from SignBook sb
        where sb.status = 'pending'
          and size(sb.signRequests) > 0
          and (sb.deleted is null or sb.deleted != true)
          and :user not member of sb.hidedBy
          and (:workflowFilter is null or sb.workflowName = :workflowFilter)
          and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
          and (:creatorFilter is null or sb.createBy = :creatorFilter)
          and (sb.createDate between :startDateFilter and :endDateFilter)
          and exists (
              select 1 from SignRequest sr
              join sr.recipientHasSigned rhs
              where sr.parentSignBook = sb
                and key(rhs).user = :user
                and rhs.actionType = 'none'
          )
          and exists (
              select 1 from LiveWorkflowStep lws
              join lws.recipients r
              where lws = sb.liveWorkflow.currentStep
                and r.user = :user
          )
        """)
    Page<SignBook> findToSign(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    @Query("""
            select count(s) from SignBook s where s.status = :status and (s.deleted is null or s.deleted != true) and :user not member of s.hidedBy and s.createBy = :user
            """)
    Long countByCreateByEppnAndStatus(User user, SignRequestStatus status);

    @Query("""
        select count(sb) from SignBook sb
        where sb.status = 'pending'
          and size(sb.signRequests) > 0
          and (sb.deleted is null or sb.deleted != true)
          and :user not member of sb.hidedBy
          and exists (
              select 1 from SignRequest sr
              join sr.recipientHasSigned rhs
              where sr.parentSignBook = sb
                and key(rhs).user = :user
                and rhs.actionType = 'none'
          )
          and exists (
              select 1 from LiveWorkflowStep lws
              join lws.recipients r
              where lws = sb.liveWorkflow.currentStep
                and r.user = :user
          )
        """)
    Long countToSign(User user);

    @Query(value = """
        with active_shares as (
            select distinct us.id,
                   us.all_sign_requests,
                   us.workflow_id,
                   us.form_id,
                   us.begin_date,
                   us.end_date
            from user_share us
            join user_account share_user on share_user.id = us.user_id
            join user_share_to_users ustu on ustu.user_share_id = us.id
            join user_account share_to_user on share_to_user.id = ustu.to_users_id
            join user_share_share_types usst on usst.user_share_id = us.id
            where share_user.eppn = :userEppn
              and share_to_user.eppn = :authUserEppn
              and usst.share_types in (0, 1, 2)
              and (us.begin_date is null or current_timestamp > us.begin_date)
              and (us.end_date is null or current_timestamp < us.end_date)
        )
        select count(distinct sb.id)
        from active_shares active_share
        join sign_book sb on (active_share.begin_date is null or sb.create_date > active_share.begin_date)
                         and (active_share.end_date is null or sb.create_date < active_share.end_date)
        join live_workflow lw on lw.id = sb.live_workflow_id
        join user_account target_user on target_user.eppn = :userEppn
        left join data sb_data on sb_data.sign_book_id = sb.id
        where sb.status = 'pending'
          and (sb.deleted is null or sb.deleted <> true)
          and (
              active_share.all_sign_requests = true
              or active_share.workflow_id = lw.workflow_id
              or active_share.form_id = sb_data.form_id
          )
          and not exists (
              select 1
              from sign_book_hided_by sbhb
              where sbhb.sign_book_id = sb.id
                and sbhb.hided_by_id = target_user.id
          )
          and exists (
              select 1
              from sign_request sr
              join sign_request_recipient_has_signed rhs on rhs.sign_request_id = sr.id
              join action recipient_action on recipient_action.id = rhs.recipient_has_signed_id
              join recipient recipient_key on recipient_key.id = rhs.recipient_has_signed_key
              where sr.parent_sign_book_id = sb.id
                and recipient_key.user_id = target_user.id
                and recipient_action.action_type = 'none'
          )
          and exists (
              select 1
              from live_workflow_step_recipients lwsr
              join recipient step_recipient on step_recipient.id = lwsr.recipients_id
              where lwsr.live_workflow_step_id = lw.current_step_id
                and step_recipient.user_id = target_user.id
          )
        """, nativeQuery = true)
    Long countToSignShared(@Param("userEppn") String userEppn, @Param("authUserEppn") String authUserEppn);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Page<SignBook> findEmpty(User user, Pageable pageable);

    @Query("select count(distinct sb) from SignBook sb join sb.liveWorkflow.currentStep.recipients r where size(sb.signRequests) = 0 and (r.user = :user or sb.createBy = :user)")
    Long countEmpty(User user);

    @Query("""
            select sb from SignBook sb
            where :user not member of sb.hidedBy
              and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
              and (:workflowFilter is null or sb.workflowName = :workflowFilter)
              and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
              and (:creatorFilter is null or sb.createBy = :creatorFilter)
              and exists (
                  select 1 from SignRequest sr
                  join sr.recipientHasSigned rhs
                  where sr.parentSignBook = sb
                    and key(rhs).user = :user
                    and rhs.actionType = 'signed'
              )
              and not exists (
                  select 1 from SignRequest sr
                  join sr.recipientHasSigned rhs
                  where sr.parentSignBook = sb
                    and key(rhs).user = :user
                    and rhs.actionType = 'refused'
              )
            """)
    Page<SignBook> findSignedByRecipientNotDeleted(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Pageable pageable);

    @Query("""
            select sb from SignBook sb
            where :user not member of sb.hidedBy
              and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
              and (:workflowFilter is null or sb.workflowName = :workflowFilter)
              and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
              and (:creatorFilter is null or sb.createBy = :creatorFilter)
              and exists (
                  select 1 from SignRequest sr
                  join sr.recipientHasSigned rhs
                  where sr.parentSignBook = sb
                    and key(rhs).user = :user
                    and rhs.actionType = 'refused'
              )
            """)
    Page<SignBook> findRefusedByRecipientNotDeleted(User user, String workflowFilter, String docTitleFilter, User creatorFilter, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.hidedBy hb where hb = :hidedBy")
    Page<SignBook> findByHidedById(User hidedBy, Pageable pageable);

    @Query("""
            select sb from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status = :status
            and (sb.deleted is null or sb.deleted != true)
            """)
    Page<SignBook> findByCreateByIdAndStatusAndSignRequestsNotNull(User user, SignRequestStatus status, Pageable pageable);

    @Query("""
            select sb from SignBook sb
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
            select sb from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and (sb.deleted = true or sb.status = 'deleted')
            """)
    Page<SignBook> findByCreateByIdDeleted(User user, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    List<SignBook> findByArchiveStatus(ArchiveStatus archiveStatus);

    @Query("SELECT s.id FROM SignBook s WHERE (s.status = 'completed' or s.status = 'refused' or s.status = 'exported') and (s.archiveStatus = 'none' or s.archiveStatus is null)")
    List<Long> findIdToArchive();

    List<SignBook> findByDeletedIsTrue();

    List<SignBook> findByStatusAndLiveWorkflowTargetsNotEmpty(SignRequestStatus signRequestStatus);

    Page<SignBook> findByStatus(SignRequestStatus signRequestStatus, Pageable pageable);

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow.id = :workflowId and sb.status != 'uploading'")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select sb from SignBook sb where (sb.liveWorkflow.workflow.id = :workflowIdLong or sb.liveWorkflow.workflow.token = :workflowIdStr) and sb.status != 'uploading'")
    List<SignBook> findByWorkflowIdOrToken(@Param("workflowIdLong") Long workflowIdLong, @Param("workflowIdStr") String workflowIdStr);

    @Query("select sb from SignBook sb where size(sb.signRequests) = 0 and sb.status != 'uploading'")
    List<SignBook> findEmpties();

    @Query("select sb from SignBook sb where sb.liveWorkflow.workflow = :workflow and (sb.hidedBy) is empty order by sb.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(sb) from SignBook sb where sb.liveWorkflow.workflow = :workflow and (sb.hidedBy) is empty")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select sb from SignBook sb join sb.viewers u where u = :user")
    Page<SignBook> findByViewersContaining(User user, Pageable pageable);

    @Query("select count(sb) from SignBook sb join sb.viewers u where u.eppn = :userEppn and sb.status = 'pending'")
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

    @Query("select distinct sb.workflowName from SignBook sb where sb.hidedBy is empty and lower(sb.workflowName) like :workflowName escape '\\'")
    List<String> findAllWorkflowNamesByName(String workflowName);

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
            select distinct sb.subject from SignBook sb
            left join sb.team team
            where (team = :user) and lower(sb.subject) like :searchString escape '\\'
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<String> findSubjects(User user, String searchString);

    @Query("""
            select distinct sb.createBy.name as name, sb.createBy.firstname as firstname, sb.createBy.eppn as eppn, sb.createBy.email as email from SignBook sb
            left join sb.team team
            where (:workflowFilter is null or sb.workflowName = :workflowFilter)
            and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
            and (team = :user)
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            and (:creatorFilter is null or sb.createBy = :creatorFilter)
            """)
    List<UserProjectionDto> findUserByRecipientAndCreateBy(User user, String workflowFilter, String docTitleFilter, User creatorFilter);

    Page<SignBook> findAll(Pageable pageable);

    @Query("""
            select distinct u.name as name, u.firstname as firstname, u.eppn as eppn, u.email as email from SignBook sb
            left join sb.team team
            left join sb.liveWorkflow lw
            left join lw.liveWorkflowSteps lws
            left join lws.recipients r
            left join r.user u
            where (team = :user) and u.email is not null
            and :user not member of sb.hidedBy
            and sb.status <> 'deleted' and (sb.deleted is null or sb.deleted != true)
            """)
    List<UserProjectionDto> findRecipientNames(User user);

    @Query("""
        select sb from SignBook sb
        where exists (
            select 1 from SignRequest sr
            where sr.parentSignBook = sb
              and sr.id in (
                  select l.signRequestId from Log l
                  where l.eppn = :eppn and l.eppn != l.eppnFor
              )
        )
        and (:workflowFilter is null or sb.workflowName = :workflowFilter)
        and (:docTitleFilter is null or lower(sb.subject) like :docTitleFilter escape '\\')
        and (:recipientUser is null or exists (
              select 1 from SignRequest sr2
              join sr2.recipientHasSigned rhs2
              where sr2.parentSignBook = sb
                and key(rhs2).user = :recipientUser
        ) or exists (
              select 1 from LiveWorkflowStep lws
              join lws.recipients r
              where lws in elements(sb.liveWorkflow.liveWorkflowSteps)
                and r.user = :recipientUser
        ))
        and (sb.createDate between :startDateFilter and :endDateFilter)
        and (:creatorFilter is null or sb.createBy = :creatorFilter)
        """)
    Page<SignBook> findOnShareByEppn(String eppn, User recipientUser, String workflowFilter, String docTitleFilter, User creatorFilter, Date startDateFilter, Date endDateFilter, Pageable pageable);

    List<SignBook> findByCreateByEppn(String userEppn);

    @Query("""
            select count(sb) from SignBook sb
            where sb.createBy = :user
            and :user not member of sb.hidedBy
            and size(sb.signRequests) > 0
            and (sb.deleted = true or sb.status = 'deleted')
            """)
    Long countByCreateByEppnAndDeleted(User user);
}

