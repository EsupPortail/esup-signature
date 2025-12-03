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

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private Map<String, String> mappingFiltersGroups = new HashMap<>();

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private Map<String, String> mappingGroupsRoles = new HashMap<>();

    @ElementCollection
    @Column(columnDefinition = "TEXT")
    private Map<String, String> groupMappingSpel = new HashMap<>();

    private Boolean hideAutoSign;

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

    public Map<String, String> getMappingGroupsRoles() {
        return mappingGroupsRoles;
    }

    public void setMappingGroupsRoles(Map<String, String> mappingGroupsRoles) {
        this.mappingGroupsRoles = mappingGroupsRoles;
    }

    public Map<String, String> getGroupMappingSpel() {
        return groupMappingSpel;
    }

    public void setGroupMappingSpel(Map<String, String> groupMappingSpel) {
        this.groupMappingSpel = groupMappingSpel;
    }

    public Boolean getHideAutoSign() {
        return hideAutoSign;
    }

    public void setHideAutoSign(Boolean hideAutoSign) {
        this.hideAutoSign = hideAutoSign;
    }
}
