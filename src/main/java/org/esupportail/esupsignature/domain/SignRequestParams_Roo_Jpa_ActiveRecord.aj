// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.springframework.transaction.annotation.Transactional;

privileged aspect SignRequestParams_Roo_Jpa_ActiveRecord {
    
    @PersistenceContext
    transient EntityManager SignRequestParams.entityManager;
    
    public static final List<String> SignRequestParams.fieldNames4OrderClauseFilter = java.util.Arrays.asList("signType", "newPageType", "signPageNumber", "xPos", "yPos");
    
    public static final EntityManager SignRequestParams.entityManager() {
        EntityManager em = new SignRequestParams().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }
    
    public static long SignRequestParams.countSignRequestParamses() {
        return entityManager().createQuery("SELECT COUNT(o) FROM SignRequestParams o", Long.class).getSingleResult();
    }
    
    public static List<SignRequestParams> SignRequestParams.findAllSignRequestParamses() {
        return entityManager().createQuery("SELECT o FROM SignRequestParams o", SignRequestParams.class).getResultList();
    }
    
    public static List<SignRequestParams> SignRequestParams.findAllSignRequestParamses(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequestParams o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequestParams.class).getResultList();
    }
    
    public static SignRequestParams SignRequestParams.findSignRequestParams(Long id) {
        if (id == null) return null;
        return entityManager().find(SignRequestParams.class, id);
    }
    
    public static List<SignRequestParams> SignRequestParams.findSignRequestParamsEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM SignRequestParams o", SignRequestParams.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    public static List<SignRequestParams> SignRequestParams.findSignRequestParamsEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequestParams o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequestParams.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    @Transactional
    public void SignRequestParams.persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }
    
    @Transactional
    public void SignRequestParams.remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            SignRequestParams attached = SignRequestParams.findSignRequestParams(this.id);
            this.entityManager.remove(attached);
        }
    }
    
    @Transactional
    public void SignRequestParams.flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }
    
    @Transactional
    public void SignRequestParams.clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }
    
    @Transactional
    public SignRequestParams SignRequestParams.merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        SignRequestParams merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
}
