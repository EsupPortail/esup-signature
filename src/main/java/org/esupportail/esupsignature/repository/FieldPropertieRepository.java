package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.FieldPropertie;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FieldPropertieRepository extends CrudRepository<FieldPropertie, Long> {
    FieldPropertie findByFieldIdAndUserEppn(Long id, String eppn);
    List<FieldPropertie> findByFieldId(Long id);
    List<FieldPropertie> findByUserEppn(String eppn);
}
