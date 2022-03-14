package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.AuditTrail;
import org.springframework.data.repository.CrudRepository;

public interface AuditTrailRepository extends CrudRepository<AuditTrail, Long>  {

    AuditTrail findByToken(String token);
    AuditTrail findByDocumentCheckSum(String checksum);

}
