package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;


public interface TagRepository extends CrudRepository<Tag, Long>  {

    Page<Tag> findAll(Pageable pageable);

}
