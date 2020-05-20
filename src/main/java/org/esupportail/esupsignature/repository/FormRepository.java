package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long>, FormRepositoryCustom {
	Long countById(Long id);
	List<Form> findByDocument(Document document);
	List<Form> findFormByNameAndActiveVersion(String name, Boolean activeVersion);
	List<Form> findFormByName(String name);

}
