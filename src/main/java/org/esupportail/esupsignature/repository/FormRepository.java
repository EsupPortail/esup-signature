package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.json.FormDto;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Set;

public interface FormRepository extends CrudRepository<Form, Long> {
	List<Form> findFormByNameAndActiveVersionAndDeletedNot(String name, Boolean activeVersion, Boolean deleted);
	@Query("select distinct f from Form f where (f.deleted is null or f.deleted = false) and size(f.authorizedShareTypes) > 0")
	List<Form> findDistinctByAuthorizedShareTypesIsNotNullAndDeletedIsNullOrDeletedIsFalse();
	@Query("select distinct f from Form f where f.name = :name and (f.deleted is null or f.deleted = false)")
	List<Form> findFormByNameAndDeletedIsNullOrDeletedIsFalse(String name);
    @Query("""
        select distinct f
        from Form f
        where f.publicUsage = true
           or exists (
                select r from f.roles r
                where r in :roles
           )
        order by f.id
    """)
    List<Form> findAuthorizedFormsByRoles(Set<String> roles);
    List<Form> findByManagerRole(String role);
	List<Form> findByWorkflowIdEquals(Long workflowId);
	List<Form> findByFieldsContaining(Field field);
	@Query("select f from Form f where f.id = :id")
    FormDto getByIdJson(Long id);
	@Query("select f from Form f")
	List<FormDto> findAllJson();
}
