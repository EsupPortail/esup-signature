// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.esupportail.esupsignature.domain.Document;

privileged aspect Document_Roo_Finder {
    
    public static Long Document.countFindDocumentsByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = Document.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Document AS o WHERE o.createBy = :createBy", Long.class);
        q.setParameter("createBy", createBy);
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<Document> Document.findDocumentsByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = Document.entityManager();
        TypedQuery<Document> q = em.createQuery("SELECT o FROM Document AS o WHERE o.createBy = :createBy", Document.class);
        q.setParameter("createBy", createBy);
        return q;
    }
    
    public static TypedQuery<Document> Document.findDocumentsByCreateByEquals(String createBy, String sortFieldName, String sortOrder) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = Document.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Document AS o WHERE o.createBy = :createBy");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Document> q = em.createQuery(queryBuilder.toString(), Document.class);
        q.setParameter("createBy", createBy);
        return q;
    }
    
}
