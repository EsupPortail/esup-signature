package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.BigFile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public interface BigFileRepository extends CrudRepository<BigFile, Long>, BigFileRepositoryCustom {

}
