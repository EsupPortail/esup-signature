package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUserEppn(String userEppn);
	@Query("select up from UserPropertie up where key(up.favorites) = :user")
	List<UserPropertie> findByFavoritesContains(User user);
}
