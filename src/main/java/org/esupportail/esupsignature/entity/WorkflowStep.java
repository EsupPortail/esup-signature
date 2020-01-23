package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.SignType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    private String name;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Recipient> recipients = new ArrayList<>();

    transient Map<String, Boolean> recipientsNames;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public Boolean isAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public SignType getSignType() {
        return this.signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public Map<String, Boolean> getRecipientsNames() {
        return recipientsNames;
    }

    public void setRecipientsNames(Map<String, Boolean> recipientsNames) {
        this.recipientsNames = recipientsNames;
    }

}
