package org.esupportail.esupsignature.repository;

import java.util.List;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.springframework.data.repository.CrudRepository;

public interface SignRequestRepository extends CrudRepository<SignRequest, Long>  {
	Long countById(Long id);
    List<SignRequest> findByName(String title);
    List<SignRequest> findDistinctByStatus(String category);
    Long countByCreateByAndStatus(String createBy, SignRequestStatus status);
}
