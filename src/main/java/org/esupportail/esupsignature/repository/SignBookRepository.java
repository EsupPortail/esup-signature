package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long> {

    Page<SignBook> findByCreateByEppn(String createByEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.signRequests sr join sr.recipientHasSigned rhs where (key(rhs).user.eppn = :recipientUserEppn or sb.createBy.eppn = :createByEppn) and sr.hidedBy is empty ")
    Page<SignBook> findByRecipientAndCreateByEppn(String recipientUserEppn, String createByEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.liveWorkflow.currentStep.recipients r where sb.status = 'pending' and r.user.eppn = :recipientUserEppn")
    Page<SignBook> findToSign(String recipientUserEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.signRequests sr join sr.recipientHasSigned rhs where key(rhs).user.eppn = :recipientUserEppn and rhs.actionType = :actionType")
    Page<SignBook> findByRecipientAndActionType(String recipientUserEppn, ActionType actionType, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.signRequests sr join sr.recipientHasSigned rhs where key(rhs).user.eppn = :recipientUserEppn")
    Page<SignBook> findByRecipient(String recipientUserEppn, Pageable pageable);

    @Query("select distinct sb from SignBook sb join sb.signRequests sr join sr.hidedBy hb where hb.eppn = :hidedByEppn")
    Page<SignBook> findByHidedByEppn(String hidedByEppn, Pageable pageable);

    Page<SignBook> findByCreateByEppnAndStatus(String createByEppn, SignRequestStatus status, Pageable pageable);

    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);

    @Query("select count(s.id) from SignBook s join s.liveWorkflow.currentStep.recipients r where s.status = 'pending' and r.user.eppn = :recipientUserEppn and r.signed is false")
    Long countByRecipientUserToSign(@Param("recipientUserEppn") String recipientUserEppn);

    @Query("select s from SignBook s where s.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);

    @Query("select s from SignBook s where s.liveWorkflow.workflow = :workflow order by s.id")
    List<SignBook> findByLiveWorkflowWorkflow(Workflow workflow);

    @Query("select count(s) from SignBook s where s.liveWorkflow.workflow = :workflow")
    int countByLiveWorkflowWorkflow(Workflow workflow);

    List<SignBook> findByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus);

    Page<SignBook> findByViewersContaining(User user, Pageable pageable);
}