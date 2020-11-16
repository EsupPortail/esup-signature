package org.esupportail.esupsignature.entity;

import org.springframework.beans.factory.annotation.Configurable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Configurable
public class UserPropertie {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer score = 0;

    @ManyToOne
    private User user;

    private String workflowName;

    private Integer step;

    @OneToMany
    private List<User> users = new ArrayList<>();

    private String targetEmail;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public String getTargetEmail() {
        return targetEmail;
    }

    public void setTargetEmail(String targetEmail) {
        this.targetEmail = targetEmail;
    }
}
