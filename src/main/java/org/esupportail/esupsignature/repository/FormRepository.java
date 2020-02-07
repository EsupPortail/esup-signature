package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FormRepository extends CrudRepository<Form, Long>, FormRepositoryCustom {
	List<Form> findByDocument(Document document);
	List<Form> findFormByActiveVersion(Boolean activeVersion);
	List<Form> findFormByNameAndActiveVersion(String name, Boolean activeVersion);
	List<Form> findFormByUserAndActiveVersion(User user, Boolean activeVersion);
	List<Form> findFormByIdAndVersion(Long id, Integer version);
}
