package org.esupportail.esupsignature.entity;

import javax.persistence.*;
import java.util.List;

@Entity
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String token;

    @OneToMany
    private List<AuditStep> auditSteps;

    private String documentId;

    private String documentName;

    private String documentType;

    private String documentCheckSum;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<AuditStep> getAuditSteps() {
        return auditSteps;
    }

    public void setAuditSteps(List<AuditStep> auditSteps) {
        this.auditSteps = auditSteps;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentCheckSum() {
        return documentCheckSum;
    }

    public void setDocumentCheckSum(String documentCheckSum) {
        this.documentCheckSum = documentCheckSum;
    }
}
