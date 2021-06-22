package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long> {
	List<Form> findFormByDeletedIsNullOrDeletedIsFalse();
	List<Form> findFormByNameAndActiveVersionAndDeletedNot(String name, Boolean activeVersion, Boolean deleted);
	@Query("select distinct f from Form f join f.managers m where m = :email and (f.deleted is null or f.deleted = false)")
	List<Form> findFormByManagersContainsAndDeletedIsNullOrDeletedIsFalse(@Param("email") String email);
	List<Form> findDistinctByAuthorizedShareTypesIsNotNullAndDeletedIsNullOrDeletedIsFalse();
	List<Form> findFormByNameAndDeletedIsNullOrDeletedIsFalse(String name);
	@Query("select distinct f from Form f where (f.deleted is null or f.deleted = false) and f.activeVersion = true and (f.publicUsage = true or :role member of f.roles) order by f.name")
	List<Form> findAuthorizedForms(String role);
	List<Form> findByRolesInAndDeletedIsNullOrDeletedIsFalse(List<String> role);

}
