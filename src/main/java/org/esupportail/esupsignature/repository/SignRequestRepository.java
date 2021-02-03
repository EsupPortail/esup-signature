package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.ActionType;
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
    @Query("select s from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn and r.signed is false")
    List<SignRequest> findByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);
    @Query("select count(s.id) from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn and r.signed is false")
    Long countByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);
    List<SignRequest> findByCreateByEppn(String CreateByEppn);
    List<SignRequest> findByCreateByEppnAndStatus(String CreateByEppn, SignRequestStatus status);
    Long countByCreateByEppnAndStatus(String CreateByEppn, SignRequestStatus status);
    Page<SignRequest> findById(Long id, Pageable pageable);
    Page<SignRequest> findAll(Pageable pageable);
    @Query("select s from SignRequest s join s.recipientHasSigned rhs where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType")
    List<SignRequest> findByRecipientAndActionType(String recipientUserEppn, ActionType actionType);
    @Query("select s from SignRequest s join s.recipientHasSigned rhs where key(rhs).user.eppn = :recipientUserEppn")
    List<SignRequest> findByRecipient(String recipientUserEppn);
    @Query("select s from SignRequest s join s.recipientHasSigned rhs where s.id = :id and key(rhs).user.eppn = :recipientUserEppn")
    List<SignRequest> findByIdAndRecipient(Long id, String recipientUserEppn);
    SignRequest findSignRequestByCommentsContains(Comment comment);
    List<SignRequest> findByTitle(String title);
    List<SignRequest> findByParentSignBookTitle(String title);
}
