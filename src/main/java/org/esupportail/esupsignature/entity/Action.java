package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
public class Action {

    public Action() {
    }

    public Action(ActionType actionType, Date date) {
        this.actionType = actionType;
        this.date = date;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @Version
    private Integer version;

    @Enumerated(EnumType.STRING)
    private ActionType actionType = ActionType.none;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date date;

    private String userIp;

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

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }
}
