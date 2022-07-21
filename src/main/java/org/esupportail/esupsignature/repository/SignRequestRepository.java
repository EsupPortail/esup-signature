package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long> {

    @Query("select count(distinct  s) from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn")
    Long countByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);

    List<SignRequest> findByIdIn(List<Long> ids);

    SignRequest findByToken(String token);

    @Query("select distinct s from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn")
    List<SignRequest> findByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);

    List<SignRequest> findByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

    Long countByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);

    Page<SignRequest> findById(Long id, Pageable pageable);

    Page<SignRequest> findAll(Pageable pageable);

    @Query("select s from SignRequest s join s.recipientHasSigned rhs where s.id = :id and key(rhs).user.eppn = :recipientUserEppn")
    List<SignRequest> findByIdAndRecipient(Long id, String recipientUserEppn);

    SignRequest findSignRequestByCommentsContains(Comment comment);

    @Query(value = "select * from sign_request where create_by_id = :userId and DATE_PART('day', now() - create_date) > :nbBeforeWarning and status = 'pending' and warning_readed = false", nativeQuery = true)
    List<SignRequest> findByCreateByEppnAndOlderPending(Long userId, Integer nbBeforeWarning);

    @Query(value = "select * from sign_request where DATE_PART('day', now() - create_date) > :nbBeforeDelete and status = 'pending' and warning_readed = true", nativeQuery = true)
    List<SignRequest> findByOlderPendingAndWarningReaded(Integer nbBeforeDelete);

    SignRequest findByLastOtp(String urlId);
}

