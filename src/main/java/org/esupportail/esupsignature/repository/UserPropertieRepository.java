package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.UserPropertie;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUserEppn(String userEppn);
}
