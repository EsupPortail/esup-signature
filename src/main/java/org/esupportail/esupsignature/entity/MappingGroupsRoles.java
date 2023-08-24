package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;

@Entity
public class MappingGroupsRoles {

    @Id
    @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "hibernate_sequence"
    )
    @SequenceGenerator(
        name = "hibernate_sequence",
        allocationSize = 1
    )
    private Long id;

    private String groupe;

    private String role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupe() {
        return groupe;
    }

    public void setGroupe(String group) {
        this.groupe = group;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
