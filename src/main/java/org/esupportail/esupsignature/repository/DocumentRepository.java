package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Document;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DocumentRepository extends CrudRepository<Document, Long>  {

    List<Document> findByCreateByEppn(String eppn);
}
