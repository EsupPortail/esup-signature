package org.esupportail.esupsignature.repository;

import java.util.List;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.springframework.data.repository.CrudRepository;

public interface SignBookRepository extends CrudRepository<SignBook, Long>, SignBookRepositoryCustom  {
	Long countById(Long id);
    List<SignBook> findByName(String name);
    Long countByName(String name);
    List<SignBook> findByRecipientEmailsAndSignBookType(List<String> recipientEmails, SignBookType signBookType);
    Long countByRecipientEmailsAndSignBookType(List<String> recipientEmails, SignBookType signBookType);
    List<SignBook> findBySignRequests(List<SignRequest> signRequests);
    List<SignBook> findByRecipientEmails(List<String> recipientEmails);
    Long countByRecipientEmails(List<String> recipientEmails);
    List<SignBook> findBySignBookType(SignBookType signBookType);
    List<SignBook> findByExternal(Boolean external);
}
