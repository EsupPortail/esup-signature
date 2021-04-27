package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.repository.custom.FormRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long>, FormRepositoryCustom {
	List<Form> findFormByNameAndActiveVersion(String name, Boolean activeVersion);
	@Query("select distinct f from Form f join f.managers m where m = :email")
	List<Form> findFormByManagersContains(@Param("email") String email);
	List<Form> findDistinctByAuthorizedShareTypesIsNotNull();
	List<Form> findFormByName(String name);
	@Query("select distinct f from Form f where f.activeVersion = true and (f.publicUsage = true or :role member of f.roles) order by f.name")
	List<Form> findAuthorizedForms(String role);

}
