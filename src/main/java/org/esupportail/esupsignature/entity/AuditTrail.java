package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @Column(unique=true)
    private String token;

    @OneToMany
    @JoinTable(
            indexes = @Index(name = "idx_audit_steps_audit_trail_id", columnList = "audit_trail_id")
    )
    private List<AuditStep> auditSteps = new ArrayList<>();

    private String documentId;

    private String documentName;

    private String documentType;

    private Long documentSize = 0L;

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

    public Long getDocumentSize() {
        return documentSize;
    }

    public void setDocumentSize(Long documentSize) {
        this.documentSize = documentSize;
    }

    public String getDocumentCheckSum() {
        return documentCheckSum;
    }

    public void setDocumentCheckSum(String documentCheckSum) {
        this.documentCheckSum = documentCheckSum;
    }
}
