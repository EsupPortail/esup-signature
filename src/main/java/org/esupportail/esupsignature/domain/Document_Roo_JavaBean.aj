// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;
import org.esupportail.esupsignature.domain.BigFile;
import org.esupportail.esupsignature.domain.Document;

privileged aspect Document_Roo_JavaBean {
    
    public String Document.getFileName() {
        return this.fileName;
    }
    
    public void Document.setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Long Document.getSize() {
        return this.size;
    }
    
    public void Document.setSize(Long size) {
        this.size = size;
    }
    
    public Long Document.getSignParentId() {
        return this.signParentId;
    }
    
    public void Document.setSignParentId(Long signParentId) {
        this.signParentId = signParentId;
    }
    
    public String Document.getContentType() {
        return this.contentType;
    }
    
    public void Document.setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public Date Document.getCreateDate() {
        return this.createDate;
    }
    
    public void Document.setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public BigFile Document.getBigFile() {
        return this.bigFile;
    }
    
    public void Document.setBigFile(BigFile bigFile) {
        this.bigFile = bigFile;
    }
    
}
