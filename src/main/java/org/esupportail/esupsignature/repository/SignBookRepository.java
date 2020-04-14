package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom  {
    List<SignBook> findByName(String name);
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    Long countByName(String name);
    List<SignBook> findByExternal(Boolean external);
}
