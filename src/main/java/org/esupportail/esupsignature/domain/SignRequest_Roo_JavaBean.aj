// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
    
    public String SignRequest.getTitle() {
        return this.title;
    }
    
    public void SignRequest.setTitle(String title) {
        this.title = title;
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
    
    public String SignRequest.getDescription() {
        return this.description;
    }
    
    public void SignRequest.setDescription(String description) {
        this.description = description;
    }
    
    public List<Document> SignRequest.getOriginalDocuments() {
        return this.originalDocuments;
    }
    
    public void SignRequest.setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }
    
    public List<Document> SignRequest.getSignedDocuments() {
        return this.signedDocuments;
    }
    
    public void SignRequest.setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }
    
    public boolean SignRequest.isOverloadSignBookParams() {
        return this.overloadSignBookParams;
    }
    
    public void SignRequest.setOverloadSignBookParams(boolean overloadSignBookParams) {
        this.overloadSignBookParams = overloadSignBookParams;
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
    
    public Map<Long, Boolean> SignRequest.getSignBooks() {
        return this.signBooks;
    }
    
    public void SignRequest.setSignBooks(Map<Long, Boolean> signBooks) {
        this.signBooks = signBooks;
    }
    
    public boolean SignRequest.isAllSignToComplete() {
        return this.allSignToComplete;
    }
    
    public void SignRequest.setAllSignToComplete(boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }
    
}
