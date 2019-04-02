// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;
import java.util.List;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequestParams;

privileged aspect SignBook_Roo_JavaBean {
    
    public String SignBook.getName() {
        return this.name;
    }
    
    public void SignBook.setName(String name) {
        this.name = name;
    }
    
    public Date SignBook.getCreateDate() {
        return this.createDate;
    }
    
    public void SignBook.setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public String SignBook.getCreateBy() {
        return this.createBy;
    }
    
    public void SignBook.setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public Date SignBook.getUpdateDate() {
        return this.updateDate;
    }
    
    public void SignBook.setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    public String SignBook.getUpdateBy() {
        return this.updateBy;
    }
    
    public void SignBook.setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String SignBook.getDescription() {
        return this.description;
    }
    
    public void SignBook.setDescription(String description) {
        this.description = description;
    }
    
    public DocumentIOType SignBook.getSourceType() {
        return this.sourceType;
    }
    
    public String SignBook.getDocumentsSourceUri() {
        return this.documentsSourceUri;
    }
    
    public void SignBook.setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }
    
    public String SignBook.getRecipientEmail() {
        return this.recipientEmail;
    }
    
    public void SignBook.setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
    
    public List<String> SignBook.getModeratorEmails() {
        return this.moderatorEmails;
    }
    
    public void SignBook.setModeratorEmails(List<String> moderatorEmails) {
        this.moderatorEmails = moderatorEmails;
    }
    
    public List<SignBook> SignBook.getSignBooksGroup() {
        return this.signBooksGroup;
    }
    
    public void SignBook.setSignBooksGroup(List<SignBook> signBooksGroup) {
        this.signBooksGroup = signBooksGroup;
    }
    
    public boolean SignBook.isAutoRemove() {
        return this.autoRemove;
    }
    
    public void SignBook.setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }
    
    public DocumentIOType SignBook.getTargetType() {
        return this.targetType;
    }
    
    public String SignBook.getDocumentsTargetUri() {
        return this.documentsTargetUri;
    }
    
    public void SignBook.setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }
    
    public Document SignBook.getModelFile() {
        return this.modelFile;
    }
    
    public void SignBook.setModelFile(Document modelFile) {
        this.modelFile = modelFile;
    }
    
    public List<SignRequest> SignBook.getSignRequests() {
        return this.signRequests;
    }
    
    public void SignBook.setSignRequests(List<SignRequest> signRequests) {
        this.signRequests = signRequests;
    }
    
    public SignRequestParams SignBook.getSignRequestParams() {
        return this.signRequestParams;
    }
    
    public void SignBook.setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
    
    public SignBookType SignBook.getSignBookType() {
        return this.signBookType;
    }
    
}
