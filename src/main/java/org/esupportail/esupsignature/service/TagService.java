package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Page<Tag> getAllTags(Pageable pageable) {
        return tagRepository.findAll(pageable);
    }

    @Transactional
    public Tag createTag(String name, String color) {
        Tag tag = new Tag(name, color);
        tagRepository.save(tag);
        return tag;
    }

    @Transactional
    public Tag createTag(Tag toAdd) {
        tagRepository.save(toAdd);
        return toAdd;
    }

    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id).orElseThrow();
        tagRepository.delete(tag);
    }

}
