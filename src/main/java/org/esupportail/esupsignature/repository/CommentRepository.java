package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, Long> {
    List<Comment> findCommentByCreateBy(User user);
}