package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long>, SignRequestRepositoryCustom  {
	Long countById(Long id);
    List<SignRequest> findByToken(String token);
    Long countByToken(String token);
    List<SignRequest> findDistinctByStatus(String category);
    List<SignRequest> findByWorkflowSteps(List<WorkflowStep> workflowSteps);
    Long countByCreateByAndStatus(String createBy, SignRequestStatus status);
    List<SignRequest> findByCreateBy(String createBy);
    Page<SignRequest> findByCreateBy(String createBy, Pageable pageable);
    Page<SignRequest> findById(Long id, Pageable pageable);
    Page<SignRequest> findAll(Pageable pageable);
    List<SignRequest> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus status);
    List<SignRequest> findByCreateByAndStatus(String createBy, SignRequestStatus status);

}
