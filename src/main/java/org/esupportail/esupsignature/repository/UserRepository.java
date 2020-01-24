package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long>  {
    List<User> findByEmail(String email);
    List<User> findByEppn(String eppn);
    List<User> findByEppnStartingWith(String eppn);
    List<User> findByNameStartingWithIgnoreCase(String name);
    List<User> findByEmailStartingWith(String email);
    Long countByEppn(String eppn);
    Long countByEmail(String email);
}
