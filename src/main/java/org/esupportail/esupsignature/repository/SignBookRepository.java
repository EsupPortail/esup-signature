package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.custom.SignBookRepositoryCustom;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom {
    List<SignBook> findByName(String name);
    List<SignBook> findByCreateBy(User createBy);
    List<SignBook> findByStatus(SignRequestStatus signRequestStatus);
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    Long countByName(String name);
    Long countById(Long id);
    List<SignBook> findByExternal(Boolean external);
}

