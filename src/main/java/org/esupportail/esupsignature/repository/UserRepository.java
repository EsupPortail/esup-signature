package org.esupportail.esupsignature.repository;

import java.util.List;

import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long>  {
    List<User> findByEmail(String email);
    List<User> findByEppn(String eppn);
    Long countByEppn(String eppn);
}
