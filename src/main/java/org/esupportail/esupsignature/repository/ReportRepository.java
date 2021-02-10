package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Report;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends CrudRepository<Report, Long> {

    public List<Report> findByUserEppn(String eppn);

    public Optional<Report> findById(Long id);

    public int countByUserEppn(String eppn);
}
