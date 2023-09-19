package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.charts.CountByYears;
import org.esupportail.esupsignature.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LogRepository extends CrudRepository<Log, Long>  {
    Page<Log> findAll(Pageable pageable);
    List<Log> findByEppn(String eppn);
    List<Log> findBySignRequestIdAndFinalStatus(Long signResquestId, String finalStatus);
    List<Log> findBySignRequestId(Long signResquestId);
    List<Log> findBySignRequestIdAndPageNumberIsNotNullAndStepNumberIsNullAndCommentIsNotNull(Long signResquestId);
    @Query(nativeQuery = true, value = "select date_part('Year', log_date) as year, count(*) as count from log where initial_status = 'completed' and final_status = 'completed' group by date_part('Year', log_date)")
    List<CountByYears> countAllByYears();
    @Query(nativeQuery = true, value = "select date_part('Year', log_date) as year, count(*) as count from log where final_status = 'refused' and sign_request_id in (select id from sign_request) group by date_part('Year', log_date)")
    List<CountByYears> countAllRefusedByYears();
}
