package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom  {
	Long countById(Long id);
    List<SignBook> findByName(String name);
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    List<SignBook> findByWorkflowStepsContains(WorkflowStep workflowStep);
    Long countByName(String name);
    Page<SignBook> findByCreateBy(String createBy, Pageable pageable);
    List<SignBook> findBySignBookType(SignBookType signBookType);
    List<SignBook> findByExternal(Boolean external);
}
