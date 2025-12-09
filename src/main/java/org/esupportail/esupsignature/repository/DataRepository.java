package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface DataRepository extends CrudRepository<Data, Long>, PagingAndSortingRepository<Data, Long> {

    @Query("""
        select d from Data d
         left join fetch d.form f
         left join fetch f.workflow w
         left join fetch w.workflowSteps ws
         left join fetch ws.signRequestParams
        where d.id = :id
        """)
    Data findByIdWithWorkflowFull(Long id);
    List<Data> findByFormId(Long formId);
    List<Data> findByCreateByAndStatus(User createBy, SignRequestStatus status);
    List<Data> findByCreateBy(User createBy);
    List<Data> findByUpdateBy(User updateBy);
    Long countByCreateByAndStatus(User createBy, SignRequestStatus status);
    Data findBySignBook(SignBook signBook);

}
