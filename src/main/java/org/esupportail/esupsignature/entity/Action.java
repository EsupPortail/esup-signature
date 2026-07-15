package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
@Table(indexes = @Index(name = "idx_action_action_type", columnList = "action_type"))
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

    @Enumerated(EnumType.STRING)
    private ActionType actionType = ActionType.none;

    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date date;

    private String userIp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
