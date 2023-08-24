package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LogRepository extends CrudRepository<Log, Long>  {
    Page<Log> findAll(Pageable pageable);
    List<Log> findByEppn(String eppn);
    List<Log> findBySignRequestIdAndFinalStatus(Long signResquestId, String finalStatus);
    List<Log> findBySignRequestId(Long signResquestId);
    List<Log> findBySignRequestIdAndPageNumberIsNotNullAndStepNumberIsNullAndCommentIsNotNull(Long signResquestId);
}
