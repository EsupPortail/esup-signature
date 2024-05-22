package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.NexuSignature;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NexuSignatureRepository extends CrudRepository<NexuSignature, Long> {
    List<NexuSignature> findBySignRequestId(Long signRequestId);
}
