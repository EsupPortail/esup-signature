package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.SignType;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class LiveWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Recipient> recipients = new ArrayList<>();

    private Boolean allSignToComplete = false;

    private Boolean repeatable = false;

    @Enumerated(EnumType.STRING)
    private SignType signType;

    @OneToOne(cascade = CascadeType.REMOVE)
    private SignRequestParams signRequestParams;

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

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public Boolean getAllSignToComplete() {
        return allSignToComplete;
    }

    public void setAllSignToComplete(Boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public SignType getSignType() {
        return signType;
    }

    public void setSignType(SignType signType) {
        this.signType = signType;
    }

    public SignRequestParams getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
}
