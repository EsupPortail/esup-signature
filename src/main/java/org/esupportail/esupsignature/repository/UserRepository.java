package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.projection.jpa.RoleManagerProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.UserProjectionDto;
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
    List<UserProjectionDto> findAllUsersDto();
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByReplaceByUser(User user);
    List<User> findByUserType(UserType userType);
    @Query("select u from User u where (u.eppn = :eppn or u.phone = :phone or u.email = :email) and u.userType != 'system'")
    Page<User> findByEppnOrPhoneOrEmailAndUserTypeNot(String eppn, String phone, String email, Pageable pageable);
    Optional<User> findByEppn(String eppn);
    Optional<User> findByAccessToken(String accessToken);
    @Query("select u from User u where u.eppn like :eppn escape '\\'")
    List<User> findByEppnStartingWith(String eppn);
    @Query("select u from User u where upper(u.name) like :name escape '\\'")
    List<User> findByNameStartingWithIgnoreCase(String name);
    @Query("select u from User u where u.email like :email escape '\\'")
    List<User> findByEmailStartingWith(String email);
    @Query(value = "select distinct roles from user_roles", nativeQuery = true)
    List<String> getAllRoles();
    @Query("select new org.esupportail.esupsignature.dto.projection.jpa.RoleManagerProjectionDto(role, u) from User u join u.managersRoles role")
    List<RoleManagerProjectionDto> findAllRoleManagers();
    @Query("select new org.esupportail.esupsignature.dto.projection.jpa.RoleManagerProjectionDto(role, u) from User u join u.managersRoles role where role in :roles")
    List<RoleManagerProjectionDto> findRoleManagersByRoles(List<String> roles);
    @Query("select role from User u join u.managersRoles role where u.eppn = :eppn")
    List<String> findManagersRolesByEppn(String eppn);
    @Query("select distinct u from User u join u.managersRoles role where role in :roles")
    List<User> findByManagersRolesIn(List<String> roles);
    @Query("select distinct u from User u join u.managersRoles role")
    List<User> findByManagersRolesNotNull();
    User findByPhone(String phone);

    List<User> findAllByUserType(UserType userType);
}
