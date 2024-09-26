package org.esupportail.esupsignature.service.interfaces.sms.impl;

import java.util.Date;
import java.util.List;

public class SmsuMessage {

    public Integer id;
    public Date date;
    public String content;
    public Integer nbRecipients;

    /**
     * senderName.
     */
    public String senderName;
    public String senderLogin;

    /**
     * group names.
     */
    public String groupSenderName;
    public String groupRecipientName;

    public String accountLabel;
    public String serviceName;

    public String stateMessage;

    public String stateMail;
    public List<String> supervisors;
    public List<String> recipients;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNbRecipients() {
        return nbRecipients;
    }

    public void setNbRecipients(Integer nbRecipients) {
        this.nbRecipients = nbRecipients;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getGroupSenderName() {
        return groupSenderName;
    }

    public void setGroupSenderName(String groupSenderName) {
        this.groupSenderName = groupSenderName;
    }

    public String getGroupRecipientName() {
        return groupRecipientName;
    }

    public void setGroupRecipientName(String groupRecipientName) {
        this.groupRecipientName = groupRecipientName;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public String getStateMail() {
        return stateMail;
    }

    public void setStateMail(String stateMail) {
        this.stateMail = stateMail;
    }

    public List<String> getSupervisors() {
        return supervisors;
    }

    public void setSupervisors(List<String> supervisors) {
        this.supervisors = supervisors;
    }

    @Override
    public String toString() {
        return "SmsuMessage [id=" + id + ", date=" + date + ", content=" + content + ", nbRecipients=" + nbRecipients
                + ", senderName=" + senderName + ", senderLogin=" + senderLogin + ", groupSenderName=" + groupSenderName
                + ", groupRecipientName=" + groupRecipientName + ", accountLabel=" + accountLabel + ", serviceName="
                + serviceName + ", stateMessage=" + stateMessage + ", stateMail=" + stateMail + ", supervisors="
                + supervisors + ", recipients=" + recipients + "]";
    }

}