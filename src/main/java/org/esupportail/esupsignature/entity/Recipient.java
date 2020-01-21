package org.esupportail.esupsignature.entity;

import javax.persistence.*;

@Entity
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @ManyToOne
    private SignRequest signRequest;

    @ManyToOne
    private User user;

    private Boolean signed = false;

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

    public SignRequest getSignRequest() {
        return signRequest;
    }

    public void setSignRequest(SignRequest signRequest) {
        this.signRequest = signRequest;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getSigned() {
        return signed;
    }

    public void setSigned(Boolean signed) {
        this.signed = signed;
    }
}
