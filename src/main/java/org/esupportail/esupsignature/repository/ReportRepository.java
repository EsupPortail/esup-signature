package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Report;
import org.esupportail.esupsignature.entity.SignRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends CrudRepository<Report, Long> {

    public List<Report> findByUserEppn(String eppn);

    public Optional<Report> findById(Long id);

    public int countByUserEppn(String eppn);

    public List<Report> findBySignRequestForbidContainsOrSignRequestsErrorContainsOrSignRequestsNoFieldContainsOrSignRequestsSignedContainsOrSignRequestUserNotInCurrentStepContains(SignRequest signRequest1, SignRequest signRequest2, SignRequest signRequest3, SignRequest signRequest4, SignRequest signRequest5);
}
