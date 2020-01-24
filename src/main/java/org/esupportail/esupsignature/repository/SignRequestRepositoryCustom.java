package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SignRequestRepositoryCustom {
    Page<SignRequest> findBySignResquestByStatus(SignRequestStatus status, Pageable pageable);
}
