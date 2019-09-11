package org.esupportail.esupsignature.repository;

import java.util.List;

import org.esupportail.esupsignature.entity.Log;
import org.springframework.data.repository.CrudRepository;

public interface LogRepository extends CrudRepository<Log, Long>  {
    List<Log> findByEppn(String eppn);
    List<Log> findByEppnAndAction(String eppn, String action);
    List<Log> findByEppnAndFinalStatus(String eppn, String finalStatus);
    List<Log> findByEppnAndSignRequestId(String eppn, Long signResquestId);
    List<Log> findBySignRequestId(Long signResquestId);
}
