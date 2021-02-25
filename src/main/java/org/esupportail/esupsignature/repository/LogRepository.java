package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Log;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LogRepository extends CrudRepository<Log, Long>  {
    List<Log> findByEppn(String eppn);
    List<Log> findByEppnAndFinalStatus(String eppn, String finalStatus);
    List<Log> findByEppnForAndFinalStatus(String eppn, String finalStatus);
    List<Log> findByEppnAndSignRequestId(String eppn, Long signResquestId);
    List<Log> findByEppnForAndSignRequestId(String eppn, Long signResquestId);
    List<Log> findBySignRequestIdAndFinalStatus(Long signResquestId, String finalStatus);
    List<Log> findBySignRequestIdAndReturnCode(Long signResquestId, String returnCode);
    List<Log> findBySignRequestIdAndStepNumberIsNotNullAndCommentIsNotNull(Long signResquestId);
    List<Log> findBySignRequestIdAndStepNumber(Long signResquestId, Integer stepNumber);
    List<Log> findBySignRequestId(Long signResquestId);
    List<Log> findBySignRequestIdAndPageNumberIsNotNullAndStepNumberIsNullAndCommentIsNotNull(Long signResquestId);

}
