package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SignRequestRepositoryCustom {

    Page<SignRequest> findBySignResquestByCreateBy(String createBy, Pageable pageable);

   
}
