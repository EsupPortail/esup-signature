package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.UserPropertie;
import org.springframework.data.repository.CrudRepository;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	UserPropertie findByUserEppn(String userEppn);
}
