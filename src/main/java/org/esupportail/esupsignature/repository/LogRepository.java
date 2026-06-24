package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.projection.chart.CountByYearsChartProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.AdminLogProjectionDto;
import org.esupportail.esupsignature.entity.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogRepository extends CrudRepository<Log, Long>  {
    Page<Log> findAll(Pageable pageable);
    List<Log> findByEppn(String eppn);
    List<Log> findBySignRequestIdAndFinalStatus(Long signResquestId, String finalStatus);
    List<Log> findBySignRequestId(Long signResquestId);
    List<Log> findBySignRequestToken(String token);
    List<Log> findBySignRequestIdAndPageNumberIsNotNullAndStepNumberIsNullAndCommentIsNotNull(Long signResquestId);
    @Query("""
            select l.logDate as logDate,
                   l.eppn as eppn,
                   l.action as action,
                   l.initialStatus as initialStatus,
                   l.finalStatus as finalStatus,
                   l.comment as comment
            from Log l
            where l.signRequestId = :signRequestId
            order by l.logDate desc
            """)
    List<AdminLogProjectionDto> findAdminLogsBySignRequestId(@Param("signRequestId") Long signRequestId);
    @Query(nativeQuery = true, value = "select cast(date_part('Year', log_date) as integer) as year, count(*) as count from log where initial_status = 'completed' and final_status = 'completed' group by date_part('Year', log_date) order by date_part('Year', log_date) desc")
    List<CountByYearsChartProjectionDto> countAllByYears();
    @Query(nativeQuery = true, value = "select cast(date_part('Year', log_date) as integer) as year, count(*) as count from log where final_status = 'refused' and sign_request_id in (select id from sign_request) group by date_part('Year', log_date) order by date_part('Year', log_date) desc")
    List<CountByYearsChartProjectionDto> countAllRefusedByYears();
}
