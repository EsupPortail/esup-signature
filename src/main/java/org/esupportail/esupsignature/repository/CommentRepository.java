package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Comment;
import org.springframework.data.repository.CrudRepository;

public interface CommentRepository extends CrudRepository<Comment, Long> {
}