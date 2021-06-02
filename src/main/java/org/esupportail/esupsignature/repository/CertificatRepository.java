package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Certificat;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CertificatRepository extends CrudRepository<Certificat, Long>  {
    List<Certificat> findByRolesIn(List<String> roles);
}
