package org.esupportail.esupsignature.entity;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Message {

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

    @Version
    private Integer version;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy - HH:mm")
    private Date endDate;

    @Column(columnDefinition = "TEXT")
    private String text;

    @ManyToMany
    private List<User> users = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
