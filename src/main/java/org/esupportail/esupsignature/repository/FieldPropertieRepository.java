package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.FieldPropertie;
import org.springframework.data.repository.CrudRepository;

public interface FieldPropertieRepository extends CrudRepository<FieldPropertie, Long> {
    public FieldPropertie findByFieldIdAndUserEppn(Long id, String eppn);
}
