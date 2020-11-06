package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.custom.SignBookRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom {
    List<SignBook> findByName(String name);
    List<SignBook> findByCreateBy(User createBy);
    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);
    @Query("select s from SignBook s join s.liveWorkflow lw where s.status = :signRequestStatus and lw.documentsTargetUri is not null")
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    List<SignBook> findByWorkflowId(Long workflowId);
    List<SignBook> findByWorkflowIdAndStatus(Long workflowId, SignRequestStatus signRequestStatus);
    @Query("select s from SignBook s join s.liveWorkflow lw where s.status = :signRequestStatus and lw.name = :workflowName")
    List<SignBook> findByWorkflowNameAndStatus(String workflowName, SignRequestStatus signRequestStatus);
    Long countByName(String name);
    Long countById(Long id);
    List<SignBook> findByExternal(Boolean external);
}