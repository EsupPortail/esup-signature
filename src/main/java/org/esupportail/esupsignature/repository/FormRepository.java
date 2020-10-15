package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.custom.FormRepositoryCustom;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long>, FormRepositoryCustom {
	Long countById(Long id);
	List<Form> findByDocument(Document document);
	List<Form> findFormByNameAndActiveVersion(String name, Boolean activeVersion);
	@Query("select f from Form f join f.managers m where m = :email")
	List<Form> findFormByManagersContains(@Param("email") String email);
	List<Form> findDistinctByAuthorizedShareTypesIsNotNull();
	List<Form> findFormByName(String name);

}
