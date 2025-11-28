package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;

import java.util.HashMap;
import java.util.Map;

@Entity
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @OrderColumn
    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private Map<String, String> mappingFiltersGroups = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, String> getMappingFiltersGroups() {
        return mappingFiltersGroups;
    }

    public void setMappingFiltersGroups(Map<String, String> mappingFiltersGroups) {
        this.mappingFiltersGroups = mappingFiltersGroups;
    }
}
