package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SignRequestRepositoryCustom {

    Page<SignRequest> findBySignResquestByCreateBy(String createBy, Pageable pageable);
    Page<SignRequest> findBySignResquestByCreateByAndStatus(String createBy, SignRequestStatus status, Pageable pageable);
    Page<SignRequest> findBySignResquestByStatus(SignRequestStatus status, Pageable pageable);
   
}
