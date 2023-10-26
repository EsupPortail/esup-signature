package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.NexuSignature;
import org.springframework.data.repository.CrudRepository;

public interface NexuSignatureRepository extends CrudRepository<NexuSignature, Long> {
    NexuSignature findBySignRequestId(Long signRequestId);
}
