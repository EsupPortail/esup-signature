package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom  {
	Long countById(Long id);
    List<SignBook> findByName(String name);
    List<SignBook> findByStatusAndDocumentsTargetUriIsNotNull(SignRequestStatus signRequestStatus);
    List<SignBook> findByWorkflowSteps(List<WorkflowStep> workflowSteps);
    Long countByName(String name);
    Page<SignBook> findByCreateByAndSignBookType(String createBy, SignBookType signBookType, Pageable pageable);
    List<SignBook> findByRecipientEmailsAndSignBookType(List<String> recipientEmails, SignBookType signBookType);
    Long countByRecipientEmailsAndSignBookType(List<String> recipientEmails, SignBookType signBookType);
    @Query("select s from SignBook s where :recipientEmail in elements(s.recipientEmails) and :signBookType = s.signBookType")
    List<SignBook> findByRecipientEmailsContainAndSignBookType(@Param("recipientEmail") String recipientEmail, @Param("signBookType") SignBookType signBookType);
    @Query("select count(s) from SignBook s where :recipientEmail in elements(s.recipientEmails) and :signBookType = s.signBookType")
    Long countByRecipientEmailsContainAndSignBookType(@Param("recipientEmail") String recipientEmail, @Param("signBookType") SignBookType signBookType);
    List<SignBook> findBySignBookType(SignBookType signBookType);
    List<SignBook> findByExternal(Boolean external);
}
