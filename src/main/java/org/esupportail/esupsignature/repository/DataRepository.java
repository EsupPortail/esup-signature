package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface DataRepository extends CrudRepository<Data, Long>, PagingAndSortingRepository<Data, Long> {
	Long countByNameStartsWith(String name);
	Long countByFormAndCreateByAndStatus(Form form, String createBy, SignRequestStatus status);
	Page<Data> findByCreateBy(String createBy, Pageable pageable);
	Page<Data> findByOwner(String owner, Pageable pageable);
	Page<Data> findByCreateByAndStatus(String createBy, SignRequestStatus status, Pageable pageable);
	List<Data> findByStatus(SignRequestStatus status);
	Page<Data> findByForm(Form form, Pageable pageable);
	Page<Data> findByFormAndCreateBy(Form form, String createBy, Pageable pageable);
	Page<Data> findByFormNameAndCreateBy(String form, String createBy, Pageable pageable);
	Page<Data> findByFormNameAndOwner(String form, String owner, Pageable pageable);
}
