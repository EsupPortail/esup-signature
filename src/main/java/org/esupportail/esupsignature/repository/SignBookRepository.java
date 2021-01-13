package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.custom.SignBookRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom {
    List<SignBook> findByName(String name);
    List<SignBook> findByCreateByEppn(String createByEppn);
    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);
    @Query("select s from SignBook s where s.status = :signRequestStatus and s.liveWorkflow.documentsTargetUri is not null")
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    @Query("select s from SignBook s where s.status = :signRequestStatus and s.liveWorkflow.workflow.id = :workflowId")
    List<SignBook> findByWorkflowId(Long workflowId);
    @Query("select s from SignBook s where s.status = :signRequestStatus and s.liveWorkflow.workflow.name = :workflowName")
    List<SignBook> findByWorkflowNameAndStatus(String workflowName, SignRequestStatus signRequestStatus);
    List<SignBook> findByLiveWorkflowAndStatus(LiveWorkflow liveWorkflow, SignRequestStatus signRequestStatus);
    Long countByName(String name);
    Long countById(Long id);
}