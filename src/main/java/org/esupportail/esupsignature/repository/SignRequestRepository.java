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
    @Query("select count(distinct  s) from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn")
    Long countByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);
    List<SignRequest> findByIdIn(List<Long> ids);
    List<SignRequest> findByToken(String token);
    @Query("select distinct s from SignRequest s join s.parentSignBook.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn")
    List<SignRequest> findByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);
    List<SignRequest> findByCreateByEppn(String createByEppn);
    List<SignRequest> findByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);
    Long countByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status);
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
    @Query("select s from SignRequest s join s.parentSignBook.liveWorkflow.workflow w where w.description = :workflowDescription")
    List<SignRequest> findByWorkflowDescription(@Param("workflowDescription") String workflowDescription);
    @Query("select s from SignRequest s where s.parentSignBook.liveWorkflow.workflow is null and s.parentSignBook.title = ''")
    List<SignRequest> findByByParentSignBookTitleEmptyAndWorflowIsNull();
}

