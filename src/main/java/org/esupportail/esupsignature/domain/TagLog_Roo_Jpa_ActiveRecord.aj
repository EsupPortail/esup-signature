// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.esupportail.esupsignature.domain.TagLog;
import org.springframework.transaction.annotation.Transactional;

privileged aspect TagLog_Roo_Jpa_ActiveRecord {
    
    @PersistenceContext
    transient EntityManager TagLog.entityManager;
    
    public static final List<String> TagLog.fieldNames4OrderClauseFilter = java.util.Arrays.asList("log", "date", "tarif", "eppnInit");
    
    public static final EntityManager TagLog.entityManager() {
        EntityManager em = new TagLog().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }
    
    public static long TagLog.countTagLogs() {
        return entityManager().createQuery("SELECT COUNT(o) FROM TagLog o", Long.class).getSingleResult();
    }
    
    public static List<TagLog> TagLog.findAllTagLogs() {
        return entityManager().createQuery("SELECT o FROM TagLog o", TagLog.class).getResultList();
    }
    
    public static List<TagLog> TagLog.findAllTagLogs(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM TagLog o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, TagLog.class).getResultList();
    }
    
    public static TagLog TagLog.findTagLog(Long id) {
        if (id == null) return null;
        return entityManager().find(TagLog.class, id);
    }
    
    public static List<TagLog> TagLog.findTagLogEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM TagLog o", TagLog.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    public static List<TagLog> TagLog.findTagLogEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM TagLog o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, TagLog.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    //@Transactional
    public void TagLog.persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }
    
    //@Transactional
    public void TagLog.remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            TagLog attached = TagLog.findTagLog(this.id);
            this.entityManager.remove(attached);
        }
    }
    
    //@Transactional
    public void TagLog.flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }
    
    //@Transactional
    public void TagLog.clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }
    
    //@Transactional
    public TagLog TagLog.merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        TagLog merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
}
