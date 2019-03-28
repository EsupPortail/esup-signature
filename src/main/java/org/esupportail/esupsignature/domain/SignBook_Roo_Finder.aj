// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import org.esupportail.esupsignature.domain.SignBook;

privileged aspect SignBook_Roo_Finder {
    
    public static Long SignBook.countFindSignBooksByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.createBy = :createBy", Long.class);
        q.setParameter("createBy", createBy);
        return ((Long) q.getSingleResult());
    }
    
    public static Long SignBook.countFindSignBooksByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.name = :name", Long.class);
        q.setParameter("name", name);
        return ((Long) q.getSingleResult());
    }
    
    public static Long SignBook.countFindSignBooksByRecipientEmailEquals(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail", Long.class);
        q.setParameter("recipientEmail", recipientEmail);
        return ((Long) q.getSingleResult());
    }
    
    public static Long SignBook.countFindSignBooksBySignBookTypeEquals(SignBookType signBookType) {
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.signBookType = :signBookType", Long.class);
        q.setParameter("signBookType", signBookType);
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.createBy = :createBy", SignBook.class);
        q.setParameter("createBy", createBy);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByCreateByEquals(String createBy, String sortFieldName, String sortOrder) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignBook.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.createBy = :createBy");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        q.setParameter("createBy", createBy);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.name = :name", SignBook.class);
        q.setParameter("name", name);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByNameEquals(String name, String sortFieldName, String sortOrder) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignBook.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.name = :name");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        q.setParameter("name", name);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByRecipientEmailAndSignBookTypeEquals(String recipientEmail, SignBookType signBookType, String sortFieldName, String sortOrder) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail AND o.signBookType = :signBookType");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        q.setParameter("recipientEmail", recipientEmail);
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByRecipientEmailEquals(String recipientEmail) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail", SignBook.class);
        q.setParameter("recipientEmail", recipientEmail);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksByRecipientEmailEquals(String recipientEmail, String sortFieldName, String sortOrder) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        EntityManager em = SignBook.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        q.setParameter("recipientEmail", recipientEmail);
        return q;
    }
    
    public static TypedQuery<SignBook> SignBook.findSignBooksBySignBookTypeEquals(SignBookType signBookType, String sortFieldName, String sortOrder) {
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.signBookType = :signBookType");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
}
