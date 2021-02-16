package org.esupportail.esupsignature.entity;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date date;

    @ManyToOne
    private User user;

    @ManyToMany
    private List<SignRequest> signRequestsSigned = new ArrayList<>();

    @ManyToMany
    private List<SignRequest> signRequestsNoField = new ArrayList<>();

    @ManyToMany
    private List<SignRequest> signRequestUserNotInCurrentStep = new ArrayList<>();

    @ManyToMany
    private List<SignRequest> signRequestForbid = new ArrayList<>();

    @ManyToMany
    private List<SignRequest> signRequestsError = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<SignRequest> getSignRequestsSigned() {
        return signRequestsSigned;
    }

    public void setSignRequestsSigned(List<SignRequest> signRequestsSigned) {
        this.signRequestsSigned = signRequestsSigned;
    }

    public List<SignRequest> getSignRequestsNoField() {
        return signRequestsNoField;
    }

    public void setSignRequestsNoField(List<SignRequest> signRequestsNoField) {
        this.signRequestsNoField = signRequestsNoField;
    }

    public List<SignRequest> getSignRequestUserNotInCurrentStep() {
        return signRequestUserNotInCurrentStep;
    }

    public void setSignRequestUserNotInCurrentStep(List<SignRequest> signRequestUserNotInCurrentStep) {
        this.signRequestUserNotInCurrentStep = signRequestUserNotInCurrentStep;
    }

    public List<SignRequest> getSignRequestForbid() {
        return signRequestForbid;
    }

    public void setSignRequestForbid(List<SignRequest> signRequestForbid) {
        this.signRequestForbid = signRequestForbid;
    }

    public List<SignRequest> getSignRequestsError() {
        return signRequestsError;
    }

    public void setSignRequestsError(List<SignRequest> signRequestsError) {
        this.signRequestsError = signRequestsError;
    }
}
