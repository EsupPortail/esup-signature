package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long> {
	List<Form> findFormByDeletedIsNullOrDeletedIsFalse();
	List<Form> findFormByNameAndActiveVersionAndDeletedNot(String name, Boolean activeVersion, Boolean deleted);
	@Query("select distinct f from Form f where (f.deleted is null or f.deleted = false) and f.authorizedShareTypes.size > 0")
	List<Form> findDistinctByAuthorizedShareTypesIsNotNullAndDeletedIsNullOrDeletedIsFalse();
	@Query("select distinct f from Form f where f.name = :name and (f.deleted is null or f.deleted = false)")
	List<Form> findFormByNameAndDeletedIsNullOrDeletedIsFalse(String name);
	@Query("select distinct f from Form f where (f.deleted is null or f.deleted = false) and f.activeVersion = true and (f.publicUsage = true or :role member of f.roles) order by f.name")
	List<Form> findAuthorizedForms(String role);
	List<Form> findByRolesIn(List<String> role);
    List<Form> findByManagerRole(String role);
	List<Form> findByWorkflowIdEquals(Long workflowId);
}
