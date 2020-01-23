package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RecipientRepository extends CrudRepository<Recipient, Long> {
    List<Recipient> findByUser(User user);
}
