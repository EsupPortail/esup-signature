package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long>, SignRequestRepositoryCustom  {
	Long countById(Long id);
    List<SignRequest> findByToken(String token);
    Long countByToken(String token);
    @Query("select s from SignRequest s join s.recipients r where r.user = :recipientUser and r.signed is false")
    List<SignRequest> findByRecipientUser(@Param("recipientUser") User recipientUser);
    List<SignRequest> findByRecipientsContains(Recipient recipients);
    Page<SignRequest> findByCreateBy(String createBy, Pageable pageable);
    Page<SignRequest> findByCreateByAndStatus(String createBy, SignRequestStatus status, Pageable pageable);
    Page<SignRequest> findByCreateByAndStatusNot(String createBy, SignRequestStatus statusNot, Pageable pageable);
    Page<SignRequest> findByCreateByAndStatusAndStatusNot(String createBy, SignRequestStatus status, SignRequestStatus statusNot, Pageable pageable);
    Page<SignRequest> findById(Long id, Pageable pageable);
    Page<SignRequest> findAll(Pageable pageable);
    List<SignRequest> findByCreateByAndStatus(String createBy, SignRequestStatus status);

}
