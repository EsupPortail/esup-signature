package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.charts.CountByYears;
import org.esupportail.esupsignature.entity.AuditStep;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AuditStepRepository extends CrudRepository<AuditStep, Long>  {
    @Query(nativeQuery = true, value = """
            select cast(date_part('Year', aus.time_stamp_date) as int) as year, count(*) as count from audit_trail at
            join public.audit_trail_audit_steps atas on at.id = atas.audit_trail_id
            join audit_step aus on atas.audit_steps_id = aus.id
            group by date_part('Year', aus.time_stamp_date) order by date_part('Year', aus.time_stamp_date) desc
            """)
    List<CountByYears> countAllByYears();
    @Query(nativeQuery = true, value = """
            select cast(date_part('Year', aus.time_stamp_date) as int) as year, count(*) as count from audit_trail at
            join public.audit_trail_audit_steps atas on at.id = atas.audit_trail_id
            join audit_step aus on atas.audit_steps_id = aus.id
            where aus.sign_certificat like 'Token Id%'
            group by date_part('Year', aus.time_stamp_date) order by date_part('Year', aus.time_stamp_date) desc
            """)
    List<CountByYears> countAllCertByYears();
}
