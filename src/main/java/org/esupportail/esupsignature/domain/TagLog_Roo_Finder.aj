// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.esupportail.esupsignature.domain;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

privileged aspect TagLog_Roo_Finder {
    
    public static Long TagLog.countFindTagLogsByDateBetween(Date minDate, Date maxDate) {
        if (minDate == null) throw new IllegalArgumentException("The minDate argument is required");
        if (maxDate == null) throw new IllegalArgumentException("The maxDate argument is required");
        EntityManager em = TagLog.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM TagLog AS o WHERE o.date BETWEEN :minDate AND :maxDate", Long.class);
        q.setParameter("minDate", minDate);
        q.setParameter("maxDate", maxDate);
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<TagLog> TagLog.findTagLogsByDateBetween(Date minDate, Date maxDate) {
        if (minDate == null) throw new IllegalArgumentException("The minDate argument is required");
        if (maxDate == null) throw new IllegalArgumentException("The maxDate argument is required");
        EntityManager em = TagLog.entityManager();
        TypedQuery<TagLog> q = em.createQuery("SELECT o FROM TagLog AS o WHERE o.date BETWEEN :minDate AND :maxDate", TagLog.class);
        q.setParameter("minDate", minDate);
        q.setParameter("maxDate", maxDate);
        return q;
    }
    
    public static TypedQuery<TagLog> TagLog.findTagLogsByDateBetween(Date minDate, Date maxDate, String sortFieldName, String sortOrder) {
        if (minDate == null) throw new IllegalArgumentException("The minDate argument is required");
        if (maxDate == null) throw new IllegalArgumentException("The maxDate argument is required");
        EntityManager em = TagLog.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM TagLog AS o WHERE o.date BETWEEN :minDate AND :maxDate");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<TagLog> q = em.createQuery(queryBuilder.toString(), TagLog.class);
        q.setParameter("minDate", minDate);
        q.setParameter("maxDate", maxDate);
        return q;
    }
    
}
