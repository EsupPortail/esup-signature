package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface DataRepository extends CrudRepository<Data, Long>, PagingAndSortingRepository<Data, Long> {
    Long countByNameStartsWith(String name);

    List<Data> findByForm(Form form);
    Page<Data> findByCreateBy(String createBy, Pageable pageable);
    Page<Data> findByFormNameAndOwner(String form, String owner, Pageable pageable);
    List<Data> findByCreateByAndStatus(String createBy, SignRequestStatus status);
    Page<Data> findByCreateByAndStatus(String createBy, SignRequestStatus status, Pageable pageable);
    List<Data> findBySignBook(SignBook signBook);
    Long countBySignBook(SignBook signBook);
}
