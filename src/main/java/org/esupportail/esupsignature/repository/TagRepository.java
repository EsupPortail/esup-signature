package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Page<Tag> findAll(Pageable pageable);

    /** Tags racines : parentTag == null */
    List<Tag> findByParentTagIsNull();

    /** Enfants directs d'un tag */
    List<Tag> findByParentTag(Tag parentTag);

    /** Recherche par nom exact et parent (null = racine) */
    Optional<Tag> findByNameAndParentTag(String name, Tag parentTag);

    /** Recherche par nom exact parmi les racines */
    Optional<Tag> findByNameAndParentTagIsNull(String name);
}
