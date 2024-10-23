package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.json.SignRequestWsDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long> {

    List<SignRequest> findByIdIn(List<Long> ids);

    Optional<SignRequest> findByToken(String token);

    @Query("select s.id as id, s.title as title, s.status as status, s.createDate as createDate, s.createBy.eppn as createByEppn, sb.endDate as endDate from SignRequest s join SignBook as sb on sb.id = s.parentSignBook.id")
    List<SignRequestWsDto> findAllByForWs();

    List<SignRequest> findByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

    @Query("""
            select count(distinct s.parentSignBook) from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = :status and (s.parentSignBook.deleted is null or s.parentSignBook.deleted != true) and s.parentSignBook.createBy.eppn = :createByEppn
            """)
    Long countByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

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

}

