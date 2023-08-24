package org.esupportail.esupsignature.entity;

import org.springframework.beans.factory.annotation.Configurable;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Configurable
@Table(indexes =  {
        @Index(name = "user_properties_user", columnList = "user_id")
})
public class UserPropertie {

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

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<User, Date> favorites = new HashMap<>();

    public Long getId() {
        return this.id;
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

    public Map<User, Date> getFavorites() {
        return favorites;
    }

    public void setFavorites(Map<User, Date> users) {
        this.favorites = users;
    }
}
