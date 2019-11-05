package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Map;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>, PagingAndSortingRepository<SignRequest, Long>, SignRequestRepositoryCustom  {
	Long countById(Long id);
    List<SignRequest> findByName(String name);
    Long countByName(String title);
    List<SignRequest> findDistinctByStatus(String category);
    List<SignRequest> findByWorkflowSteps(List<WorkflowStep> workflowSteps);
    Long countByCreateByAndStatus(String createBy, SignRequestStatus status);
    Page<SignRequest> findByCreateBy(String createBy, Pageable pageable);
    Page<SignRequest> findById(Long id, Pageable pageable);
    Page<SignRequest> findAll(Pageable pageable);
}
