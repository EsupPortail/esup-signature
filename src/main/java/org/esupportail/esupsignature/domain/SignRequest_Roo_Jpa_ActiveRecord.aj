// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.esupportail.esupsignature.domain.SignRequest;
import org.springframework.transaction.annotation.Transactional;

privileged aspect SignRequest_Roo_Jpa_ActiveRecord {
    
    @PersistenceContext
    transient EntityManager SignRequest.entityManager;
    
    public static final List<String> SignRequest.fieldNames4OrderClauseFilter = java.util.Arrays.asList("name", "createDate", "createBy", "updateDate", "updateBy", "recipientEmail", "description", "originalFile", "signedFile", "status", "params", "signType", "newPageType", "signPageNumber", "xPos", "yPos");
    
    public static final EntityManager SignRequest.entityManager() {
        EntityManager em = new SignRequest().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }
    
    public static long SignRequest.countSignRequests() {
        return entityManager().createQuery("SELECT COUNT(o) FROM SignRequest o", Long.class).getSingleResult();
    }
    
    public static List<SignRequest> SignRequest.findAllSignRequests() {
        return entityManager().createQuery("SELECT o FROM SignRequest o", SignRequest.class).getResultList();
    }
    
    public static List<SignRequest> SignRequest.findAllSignRequests(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequest o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequest.class).getResultList();
    }
    
    public static SignRequest SignRequest.findSignRequest(Long id) {
        if (id == null) return null;
        return entityManager().find(SignRequest.class, id);
    }
    
    public static List<SignRequest> SignRequest.findSignRequestEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM SignRequest o", SignRequest.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    public static List<SignRequest> SignRequest.findSignRequestEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequest o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequest.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    @Transactional
    public void SignRequest.persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }
    
    @Transactional
    public void SignRequest.remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            SignRequest attached = SignRequest.findSignRequest(this.id);
            this.entityManager.remove(attached);
        }
    }
    
    @Transactional
    public void SignRequest.flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }
    
    @Transactional
    public void SignRequest.clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }
    
    @Transactional
    public SignRequest SignRequest.merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        SignRequest merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
}
