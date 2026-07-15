package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.projection.jpa.AttachmentProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.DocumentProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignRequestLightProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignRequestProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignRequestTabProjectionDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.WsAccessToken;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long> {

    List<SignRequest> findByIdIn(List<Long> ids);

    @Query("""
            select distinct s from SignRequest s
            left join fetch s.createBy
            left join fetch s.parentSignBook sb
            left join fetch sb.liveWorkflow lw
            left join fetch lw.currentStep cs
            left join fetch cs.workflowStep
            left join fetch lw.workflow
            where s.id = :id
            """)
    Optional<SignRequest> findByIdWithShowContext(@Param("id") Long id);

    Optional<SignRequest> findByToken(String token);

    @Query("""
            select d.id as id,
                   d.fileName as fileName,
                   d.size as size,
                   d.contentType as contentType,
                   d.pdfaCheck as pdfaCheck
            from SignRequest s
            join s.originalDocuments d
            where s.id = :id
            order by index(d)
            """)
    List<DocumentProjectionDto> findOriginalDocumentProjectionsById(@Param("id") Long id);

    @Query("""
            select d.id as id,
                   d.fileName as fileName,
                   d.size as size,
                   d.contentType as contentType,
                   d.pdfaCheck as pdfaCheck
            from SignRequest s
            join s.signedDocuments d
            where s.id = :id
            order by index(d)
            """)
    List<DocumentProjectionDto> findSignedDocumentProjectionsById(@Param("id") Long id);

    @Query("""
            select d.id as id,
                   d.fileName as fileName,
                   d.size as size,
                   d.contentType as contentType,
                   d.pdfaCheck as pdfaCheck
            from SignRequest s
            join s.documentsHistory d
            where s.id = :id
            """)
    Optional<DocumentProjectionDto> findDocumentsHistoryProjectionById(@Param("id") Long id);

    @Query("""
            select d.id as id,
                   d.fileName as fileName,
                   d.size as size,
                   d.contentType as contentType,
                   d.pdfaCheck as pdfaCheck,
                   cb.eppn as createByEppn,
                   cb.firstname as createByFirstname,
                   cb.name as createByName
            from SignRequest s
            join s.attachments d
            left join d.createBy cb
            where s.id = :id
            order by index(d)
            """)
    List<AttachmentProjectionDto> findAttachmentProjectionsById(@Param("id") Long id);

    @Query(value = """
            select sr.id as id,
                   sr.title as title,
                   sr.status as status,
                   sr.deleted as deleted,
                   exists (
                       select 1
                       from sign_request_viewed_by srvb
                       join user_account viewed_user on viewed_user.id = srvb.viewed_by_id
                       where srvb.sign_request_id = sr.id
                         and viewed_user.eppn = :userEppn
                   ) as viewedByCurrentUser
            from sign_book_sign_requests sbs
            join sign_request sr on sr.id = sbs.sign_requests_id
            where sbs.sign_book_id = :signBookId
            order by sbs.sign_requests_order
            """, nativeQuery = true)
    List<SignRequestTabProjectionDto> findTabProjectionsBySignBookId(@Param("signBookId") Long signBookId, @Param("userEppn") String userEppn);

    @Query(value = """
            select count(*)
            from sign_book_sign_requests sbs
            join sign_request sr on sr.id = sbs.sign_requests_id
            where sbs.sign_book_id = :signBookId
              and sr.status = 'pending'
              and coalesce(sr.deleted, false) = false
            """, nativeQuery = true)
    int countPendingBySignBookId(@Param("signBookId") Long signBookId);

    @Query(value = """
            select sr.id
            from sign_book_sign_requests sbs
            join sign_request sr on sr.id = sbs.sign_requests_id
            where sbs.sign_book_id = :signBookId
              and sr.id <> :signRequestId
              and sr.status = 'pending'
              and coalesce(sr.deleted, false) = false
            order by sr.create_date desc nulls last, sr.id desc
            limit 1
            """, nativeQuery = true)
    Long findNextPendingIdBySignBookId(@Param("signBookId") Long signBookId, @Param("signRequestId") Long signRequestId);

    @Query(value = """
            select sr.id
            from sign_book_sign_requests sbs
            join sign_request sr on sr.id = sbs.sign_requests_id
            where sbs.sign_book_id = :signBookId
              and sr.status = 'pending'
              and coalesce(sr.deleted, false) = false
            order by sr.create_date desc nulls last, sr.id desc
            limit 1
            """, nativeQuery = true)
    Long findFirstPendingIdBySignBookId(@Param("signBookId") Long signBookId);

    @Query(value = """
            select sbs.sign_requests_order
            from sign_book_sign_requests sbs
            where sbs.sign_book_id = :signBookId
              and sbs.sign_requests_id = :signRequestId
            limit 1
            """, nativeQuery = true)
    Integer findOrderBySignBookIdAndSignRequestId(@Param("signBookId") Long signBookId, @Param("signRequestId") Long signRequestId);

    @Query("""
            select s.id as id,
                   parent.id as clonedFromId,
                   s.status as status,
                   s.deleted as deleted,
                   s.token as token,
                   cb.id as createById,
                   cb.eppn as createByEppn,
                   cb.firstname as createByFirstname,
                   cb.name as createByName
            from SignRequest s
            join s.clonedFrom parent
            left join s.createBy cb
            where parent.id = :signRequestId
            order by s.createDate desc, s.id desc
            """)
    List<SignRequestLightProjectionDto> findCloneLightProjectionsBySignRequestId(@Param("signRequestId") Long signRequestId);

    List<SignRequest> findByClonedFromId(Long clonedFromId);

    @Query("""
            select s.id as id, s.title as title, s.status as status, s.createDate as createDate, s.createBy.eppn as createByEppn, sb.endDate as endDate 
            from SignRequest s 
            join SignBook as sb on sb.id = s.parentSignBook.id
            join sb.liveWorkflow as lw
            join lw.workflow as w
            join w.wsAccessTokens as t
            where :token in (t) 
            """)
    List<SignRequestProjectionDto> findAllByToken(WsAccessToken token);

    @Query("select s.id as id, s.title as title, s.status as status, s.createDate as createDate, s.createBy.eppn as createByEppn, sb.endDate as endDate from SignRequest s join SignBook as sb on sb.id = s.parentSignBook.id")
    List<SignRequestProjectionDto> findAllForWs();

    List<SignRequest> findByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

    @Query("""
            select count(distinct s.parentSignBook) from SignRequest s where s.status = :status and (s.parentSignBook.deleted is null or s.parentSignBook.deleted != true) and s.parentSignBook.createBy.eppn = :createByEppn
            """)
    Long countByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

    @Query("""
        select r.user.id, sr.id
        from SignRequest sr
        join sr.parentSignBook sb
        join sb.liveWorkflow lw
        join lw.currentStep cs
        join cs.recipients r
        where sr.createBy.eppn = :eppn
          and sr.status = :status
    """)
    List<Object[]> findRecipientUserIdsBySignRequest(
            @Param("eppn") String eppn,
            @Param("status") SignRequestStatus status);

    Page<SignRequest> findById(Long id, Pageable pageable);

    Page<SignRequest> findAll(Pageable pageable);

    @Query("select s from SignRequest s join s.recipientHasSigned rhs where s.id = :id and key(rhs).user.eppn = :recipientUserEppn")
    List<SignRequest> findByIdAndRecipient(Long id, String recipientUserEppn);

    SignRequest findSignRequestByCommentsContains(Comment comment);

    @Query(value = "select * from sign_request where create_by_id = :userId and DATE_PART('day', now() - create_date) > :nbBeforeWarning and status = 'pending' and warning_readed = false", nativeQuery = true)
    List<SignRequest> findByCreateByEppnAndOlderPending(Long userId, Integer nbBeforeWarning);

    List<SignRequest> findByCreateByEppn(String userEpppn);

    @Query(value = "select * from sign_request where DATE_PART('day', now() - create_date) > :nbBeforeDelete and status = 'pending' and warning_readed = true", nativeQuery = true)
    List<SignRequest> findByOlderPendingAndWarningReaded(Integer nbBeforeDelete);

    @Query(value = "select s from SignRequest s join SignBook as sb on sb.id = s.parentSignBook.id where sb.endDate is null and s.cleanDocumentsHistoryDate is null and s.status != 'pending' and s.status != 'draft' and s.status != 'uploading' and s.status != 'refused'")
    List<SignRequest> findSignRequestsByCleanDocumentsHistoryDateIsNull();
}

