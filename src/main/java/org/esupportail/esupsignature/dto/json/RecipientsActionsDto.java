package org.esupportail.esupsignature.dto.json;

import java.util.Date;

public class RecipientsActionsDto {

    String userEppn;
    String userEmail;
    String userName;
    String userFirstname;
    Integer stepNumber;
    Date actionDate;
    String actionType;
    String refuseComment;
    Integer signPageNumber;
    Integer signPosX;
    Integer signPosY;
    String signType;

    public String getUserEppn() {
        return userEppn;
    }

    public void setUserEppn(String userEppn) {
        this.userEppn = userEppn;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserFirstname() {
        return userFirstname;
    }

    public void setUserFirstname(String userFirstname) {
        this.userFirstname = userFirstname;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getRefuseComment() {
        return refuseComment;
    }

    public void setRefuseComment(String refuseComment) {
        this.refuseComment = refuseComment;
    }

    public Integer getSignPageNumber() {
        return signPageNumber;
    }

    public void setSignPageNumber(Integer signPageNumber) {
        this.signPageNumber = signPageNumber;
    }

    public Integer getSignPosX() {
        return signPosX;
    }

    public void setSignPosX(Integer signPosX) {
        this.signPosX = signPosX;
    }

    public Integer getSignPosY() {
        return signPosY;
    }

    public void setSignPosY(Integer signPosY) {
        this.signPosY = signPosY;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }
}
