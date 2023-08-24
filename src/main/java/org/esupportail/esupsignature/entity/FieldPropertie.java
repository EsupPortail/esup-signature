package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Configurable
public class FieldPropertie {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence-generator")
    @GenericGenerator(
            name = "sequence-generator",
            type = org.hibernate.id.enhanced.SequenceStyleGenerator.class,
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "hibernate_sequence"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
            }
    )
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
