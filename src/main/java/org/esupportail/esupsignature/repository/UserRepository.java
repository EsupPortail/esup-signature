package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.UserDto;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long>  {
    Page<User> findAll(Pageable pageable);
    @Query("select distinct u.name as name, u.firstname as firstname, u.eppn as eppn, u.email as email from User u")
    List<UserDto> findAllUsersDto();
    Optional<User> findByEmail(String email);
    List<User> findByReplaceByUser(User user);
    List<User> findByEmailAndUserType(String email, UserType userType);
    List<User> findByUserType(UserType userType);
    @Query("select u from User u where u.userType != 'system'")
    Page<User> findByUserTypeNot(UserType userType, Pageable pageable);
    @Query("select u from User u where (u.eppn = :eppn or u.phone = :phone or u.email = :email) and u.userType != 'system'")
    Page<User> findByEppnOrPhoneOrEmailAndUserTypeNot(String eppn, String phone, String email, Pageable pageable);
    Optional<User> findByEppn(String eppn);
    @Query("select u from User u where u.eppn like :eppn%")
    List<User> findByEppnStartingWith(String eppn);
    @Query("select u from User u where upper(u.name) like :name%")
    List<User> findByNameStartingWithIgnoreCase(String name);
    @Query("select u from User u where u.email like :email%")
    List<User> findByEmailStartingWith(String email);
    @Query(value = "select distinct roles from user_roles", nativeQuery = true)
    List<String> getAllRoles();
    List<User> findByManagersRolesIn(List<String> role);
    List<User> findByManagersRolesNotNull();
    Long countByEmailIgnoreCaseAndUserType(String email, UserType userType);
    User findByPhone(String phone);
}
