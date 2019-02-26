// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequestParams;

privileged aspect SignRequest_Roo_JavaBean {
    
    public String SignRequest.getName() {
        return this.name;
    }
    
    public void SignRequest.setName(String name) {
        this.name = name;
    }
    
    public Date SignRequest.getCreateDate() {
        return this.createDate;
    }
    
    public void SignRequest.setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public String SignRequest.getCreateBy() {
        return this.createBy;
    }
    
    public void SignRequest.setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public String SignRequest.getRecipientEmail() {
        return this.recipientEmail;
    }
    
    public void SignRequest.setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public String SignRequest.getDescription() {
        return this.description;
    }
    
    public void SignRequest.setDescription(String description) {
        this.description = description;
    }
    
    public Document SignRequest.getOriginalFile() {
        return this.originalFile;
    }
    
    public void SignRequest.setOriginalFile(Document originalFile) {
        this.originalFile = originalFile;
    }
    
    public Document SignRequest.getSignedFile() {
        return this.signedFile;
    }
    
    public void SignRequest.setSignedFile(Document signedFile) {
        this.signedFile = signedFile;
    }
    
    public SignRequestParams SignRequest.getSignRequestParams() {
        return this.signRequestParams;
    }
    
    public void SignRequest.setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
    
    public SignRequestStatus SignRequest.getStatus() {
        return this.status;
    }
    
    public long SignRequest.getSignBookId() {
        return this.signBookId;
    }
    
    public void SignRequest.setSignBookId(long signBookId) {
        this.signBookId = signBookId;
    }
    
    public String SignRequest.getSignBookName() {
        return this.signBookName;
    }
    
    public void SignRequest.setSignBookName(String signBookName) {
        this.signBookName = signBookName;
    }
    
    public String SignRequest.getSignType() {
        return this.signType;
    }
    
    public void SignRequest.setSignType(String signType) {
        this.signType = signType;
    }
    
    public String SignRequest.getNewPageType() {
        return this.newPageType;
    }
    
    public void SignRequest.setNewPageType(String newPageType) {
        this.newPageType = newPageType;
    }
    
    public String SignRequest.getSignPageNumber() {
        return this.signPageNumber;
    }
    
    public void SignRequest.setSignPageNumber(String signPageNumber) {
        this.signPageNumber = signPageNumber;
    }
    
    public String SignRequest.getXPos() {
        return this.xPos;
    }
    
    public void SignRequest.setXPos(String xPos) {
        this.xPos = xPos;
    }
    
    public String SignRequest.getYPos() {
        return this.yPos;
    }
    
    public void SignRequest.setYPos(String yPos) {
        this.yPos = yPos;
    }
    
}
