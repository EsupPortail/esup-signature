package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.SignType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    private String name;

    private String description;

    private Integer maxRecipients = 99;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    private Boolean changeable = false;

    private Boolean allSignToComplete = false;

    @Enumerated(EnumType.STRING)
    private SignType signType;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getChangeable() {
        return changeable;
    }

    public void setChangeable(Boolean changeable) {
        this.changeable = changeable;
    }

    public Integer getMaxRecipients() {
        return maxRecipients;
    }

    public void setMaxRecipients(Integer maxRecipients) {
        this.maxRecipients = maxRecipients;
    }
}
