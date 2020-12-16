package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.custom.SignRequestRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long>, SignRequestRepositoryCustom {
    Long countById(Long id);
    List<SignRequest> findByIdIn(List<Long> ids);
    List<SignRequest> findByToken(String token);
    Long countByToken(String token);
    @Query("select s from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where r.user.id = :recipientUserId and r.signed is false")
    List<SignRequest> findByRecipientUserToSign(@Param("recipientUserId") Long recipientUserId);
    @Query("select s from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where r.user.id = :recipientUserId")
    List<SignRequest> findByRecipientUserId(@Param("recipientUserId") Long recipientUserId);
    List<SignRequest> findByCreateById(Long createById);
    List<SignRequest> findByCreateByIdAndStatus(Long createById, SignRequestStatus status);
    Page<SignRequest> findById(Long id, Pageable pageable);
    Page<SignRequest> findAll(Pageable pageable);

}
