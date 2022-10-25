package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long>  {
    Page<User> findAll(Pageable pageable);
    List<User> findByEmail(String email);
    List<User> findByReplaceByUser(User user);
    List<User> findByEmailAndUserType(String email, UserType userType);
    List<User> findByUserType(UserType userType);
    Page<User> findByEppn(String eppn, Pageable pageable);
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
    List<User> findByManagersRolesNotNull();
    @Query(value = "select count(*) from user_account as u where u.eppn = :eppn", nativeQuery = true)
    Long countByEppn(String eppn);
    @Query(value = "select count(*) from user_account as u where u.email = :email", nativeQuery = true)
    Long countByEmail(String email);
    Long countByEmailAndUserType(String email, UserType userType);
    User findByPhone(String phone);
}
