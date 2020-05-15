package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long>  {
    List<Message> findByUsersNotContainsAndEndDateAfter(User user, Date now);
    Long countByUsersNotContainsAndEndDateAfter(User user, Date now);
}
