package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DataRepository extends CrudRepository<Data, Long>, PagingAndSortingRepository<Data, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Data> findWithLockingById(Long id);

    List<Data> findByFormId(Long formId);
    List<Data> findByCreateByAndStatus(User createBy, SignRequestStatus status);
    List<Data> findByCreateBy(User createBy);
    List<Data> findByUpdateBy(User updateBy);
    Long countByCreateByAndStatus(User createBy, SignRequestStatus status);
    Data findBySignBook(SignBook signBook);

}
