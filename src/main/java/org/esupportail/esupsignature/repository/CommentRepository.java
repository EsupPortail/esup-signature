package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.dto.projection.jpa.AdminCommentProjectionDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends CrudRepository<Comment, Long> {
    List<Comment> findCommentByCreateBy(User user);

    @Query("""
            select c.id as id,
                   c.createDate as createDate,
                   c.text as text,
                   cb.id as createById,
                   cb.eppn as createByEppn,
                   cb.firstname as createByFirstname,
                   cb.name as createByName,
                   cb.email as createByEmail,
                   cb.phone as createByPhone,
                   cb.userType as createByUserType
            from SignRequest sr
            join sr.comments c
            left join c.createBy cb
            where sr.id = :signRequestId
              and (c.isPostit = false or c.isPostit is null)
              and c.stepNumber is null
            order by c.createDate asc, c.id asc
            """)
    List<AdminCommentProjectionDto> findAdminCommentsBySignRequestId(@Param("signRequestId") Long signRequestId);
}