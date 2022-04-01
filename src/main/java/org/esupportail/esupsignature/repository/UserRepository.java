package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long>  {
    List<User> findByEmail(String email);
    List<User> findByEppn(String eppn);
    @Query("select u from User u where u.eppn like :eppn%")
    List<User> findByEppnStartingWith(String eppn);
    @Query("select u from User u where u.name like :name%")
    List<User> findByNameStartingWithIgnoreCase(String name);
    @Query("select u from User u where u.email like :email%")
    List<User> findByEmailStartingWith(String email);
    @Query(value = "select distinct roles from user_roles", nativeQuery = true)
    List<String> getAllRoles();
    List<User> findByManagersRolesIn(List<String> role);
    Long countByEppn(String eppn);
    Long countByEmail(String email);
    User findByPhone(String phone);
}
