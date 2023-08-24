package org.esupportail.esupsignature.entity;

import org.springframework.beans.factory.annotation.Configurable;

import jakarta.persistence.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Configurable
public class FieldPropertie {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Field field;

    @ElementCollection
    private Map<String, Date> favorites = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public Map<String, Date> getFavorites() {
        return favorites;
    }

    public void setFavorites(Map<String, Date> favorites) {
        this.favorites = favorites;
    }
}
