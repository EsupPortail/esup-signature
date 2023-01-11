package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Certificat;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface CertificatRepository extends CrudRepository<Certificat, Long>  {
    List<Certificat> findByRolesIn(Set<String> roles);
}
