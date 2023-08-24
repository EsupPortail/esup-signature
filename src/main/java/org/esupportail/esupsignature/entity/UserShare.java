package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.hibernate.annotations.GenericGenerator;

import java.util.*;

@Entity
public class UserShare {

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

    @ManyToMany
    private List<User> toUsers = new ArrayList<>();

    private Boolean signWithOwnSign = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private Form form;

    @ManyToOne(fetch = FetchType.LAZY)
    private Workflow workflow;

    private Boolean allSignRequests = false;

    @Temporal(TemporalType.TIMESTAMP)
    private Date beginDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date endDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate = new Date();

    @ElementCollection(targetClass = ShareType.class, fetch = FetchType.EAGER)
    private Set<ShareType> shareTypes = new HashSet<>();

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

    public List<User> getToUsers() {
        return toUsers;
    }

    public void setToUsers(List<User> toUsers) {
        this.toUsers = toUsers;
    }


    public Boolean getSignWithOwnSign() {
        return signWithOwnSign;
    }

    public void setSignWithOwnSign(Boolean signWithOwnSign) {
        this.signWithOwnSign = signWithOwnSign;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Boolean getAllSignRequests() {
        return allSignRequests;
    }

    public void setAllSignRequests(Boolean all) {
        this.allSignRequests = all;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Set<ShareType> getShareTypes() {
        return shareTypes;
    }

    public void setShareTypes(Set<ShareType> shareTypes) {
        this.shareTypes = shareTypes;
    }
}
