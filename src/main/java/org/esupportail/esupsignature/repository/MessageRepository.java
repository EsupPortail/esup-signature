package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.List;

public interface MessageRepository extends CrudRepository<Message, Long>, PagingAndSortingRepository<Message, Long> {
    Page<Message> findAll(Pageable pageable);
    List<Message> findByUsersNotContainsAndEndDateAfter(User user, Date now);
    List<Message> findByUsersContains(User user);
    Long countByUsersNotContainsAndEndDateAfter(User user, Date now);
}
