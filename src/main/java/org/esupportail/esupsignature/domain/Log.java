package org.esupportail.esupsignature.domain;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PersistenceContext;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.Version;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Entity
public class Log {

	@Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy - HH:mm")
    private Date logDate;
	
	private String eppn;

	private String action;
	
	private String initialStatus;
	
	private String finalStatus;
	
	private String returnCode;
	
	private String ip;
	
	private String comment;
	
	private Long signRequestId;


	public Date getLogDate() {
        return this.logDate;
    }

	public void setLogDate(Date logDate) {
        this.logDate = logDate;
    }

	public String getEppn() {
        return this.eppn;
    }

	public void setEppn(String eppn) {
        this.eppn = eppn;
    }

	public String getAction() {
        return this.action;
    }

	public void setAction(String action) {
        this.action = action;
    }

	public String getInitialStatus() {
        return this.initialStatus;
    }

	public void setInitialStatus(String initialStatus) {
        this.initialStatus = initialStatus;
    }

	public String getFinalStatus() {
        return this.finalStatus;
    }

	public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

	public String getReturnCode() {
        return this.returnCode;
    }

	public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

	public String getIp() {
        return this.ip;
    }

	public void setIp(String ip) {
        this.ip = ip;
    }

	public String getComment() {
        return this.comment;
    }

	public void setComment(String comment) {
        this.comment = comment;
    }

	public long getSignRequestId() {
        return this.signRequestId;
    }

	public void setSignRequestId(long signRequestId) {
        this.signRequestId = signRequestId;
    }

	@PersistenceContext
    transient EntityManager entityManager;

	public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("logDate", "eppn", "action", "initialStatus", "finalStatus", "returnCode", "ip", "comment", "signRequestId");

	public static final EntityManager entityManager() {
        EntityManager em = new Log().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countLogs() {
        return entityManager().createQuery("SELECT COUNT(o) FROM Log o", Long.class).getSingleResult();
    }

	public static List<Log> findAllLogs() {
        return entityManager().createQuery("SELECT o FROM Log o", Log.class).getResultList();
    }

	public static List<Log> findAllLogs(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM Log o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, Log.class).getResultList();
    }

	public static Log findLog(Long id) {
        if (id == null) return null;
        return entityManager().find(Log.class, id);
    }

	public static List<Log> findLogEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM Log o", Log.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public static List<Log> findLogEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM Log o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, Log.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	@Transactional
    public void persist() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.persist(this);
    }

	@Transactional
    public void remove() {
        if (this.entityManager == null) this.entityManager = entityManager();
        if (this.entityManager.contains(this)) {
            this.entityManager.remove(this);
        } else {
            Log attached = Log.findLog(this.id);
            this.entityManager.remove(attached);
        }
    }

	@Transactional
    public void flush() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.flush();
    }

	@Transactional
    public void clear() {
        if (this.entityManager == null) this.entityManager = entityManager();
        this.entityManager.clear();
    }

	@Transactional
    public Log merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        Log merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

	public static Long countFindLogsByEppnAndActionEquals(String eppn, String action) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        if (action == null || action.length() == 0) throw new IllegalArgumentException("The action argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Log AS o WHERE o.eppn = :eppn AND o.action = :action", Long.class);
        q.setParameter("eppn", eppn);
        q.setParameter("action", action);
        return ((Long) q.getSingleResult());
    }

	public static Long countFindLogsByEppnAndSignRequestIdEquals(String eppn, long signRequestId) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Log AS o WHERE o.eppn = :eppn AND o.signRequestId = :signRequestId", Long.class);
        q.setParameter("eppn", eppn);
        q.setParameter("signRequestId", signRequestId);
        return ((Long) q.getSingleResult());
    }

	public static Long countFindLogsByEppnEquals(String eppn) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Log AS o WHERE o.eppn = :eppn", Long.class);
        q.setParameter("eppn", eppn);
        return ((Long) q.getSingleResult());
    }

	public static Long countFindLogsBySignRequestIdEquals(long signRequestId) {
        EntityManager em = Log.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Log AS o WHERE o.signRequestId = :signRequestId", Long.class);
        q.setParameter("signRequestId", signRequestId);
        return ((Long) q.getSingleResult());
    }

	public static TypedQuery<Log> findLogsByEppnAndActionEquals(String eppn, String action) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        if (action == null || action.length() == 0) throw new IllegalArgumentException("The action argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery<Log> q = em.createQuery("SELECT o FROM Log AS o WHERE o.eppn = :eppn AND o.action = :action", Log.class);
        q.setParameter("eppn", eppn);
        q.setParameter("action", action);
        return q;
    }

	public static TypedQuery<Log> findLogsByEppnAndActionEquals(String eppn, String action, String sortFieldName, String sortOrder) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        if (action == null || action.length() == 0) throw new IllegalArgumentException("The action argument is required");
        EntityManager em = Log.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Log AS o WHERE o.eppn = :eppn AND o.action = :action");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Log> q = em.createQuery(queryBuilder.toString(), Log.class);
        q.setParameter("eppn", eppn);
        q.setParameter("action", action);
        return q;
    }

	public static TypedQuery<Log> findLogsByEppnAndSignRequestIdEquals(String eppn, long signRequestId) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery<Log> q = em.createQuery("SELECT o FROM Log AS o WHERE o.eppn = :eppn AND o.signRequestId = :signRequestId", Log.class);
        q.setParameter("eppn", eppn);
        q.setParameter("signRequestId", signRequestId);
        return q;
    }

	public static TypedQuery<Log> findLogsByEppnAndSignRequestIdEquals(String eppn, long signRequestId, String sortFieldName, String sortOrder) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Log AS o WHERE o.eppn = :eppn AND o.signRequestId = :signRequestId");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Log> q = em.createQuery(queryBuilder.toString(), Log.class);
        q.setParameter("eppn", eppn);
        q.setParameter("signRequestId", signRequestId);
        return q;
    }

	public static TypedQuery<Log> findLogsByEppnEquals(String eppn) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        TypedQuery<Log> q = em.createQuery("SELECT o FROM Log AS o WHERE o.eppn = :eppn", Log.class);
        q.setParameter("eppn", eppn);
        return q;
    }

	public static TypedQuery<Log> findLogsByEppnEquals(String eppn, String sortFieldName, String sortOrder) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = Log.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Log AS o WHERE o.eppn = :eppn");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Log> q = em.createQuery(queryBuilder.toString(), Log.class);
        q.setParameter("eppn", eppn);
        return q;
    }

	public static TypedQuery<Log> findLogsBySignRequestIdEquals(long signRequestId) {
        EntityManager em = Log.entityManager();
        TypedQuery<Log> q = em.createQuery("SELECT o FROM Log AS o WHERE o.signRequestId = :signRequestId", Log.class);
        q.setParameter("signRequestId", signRequestId);
        return q;
    }

	public static TypedQuery<Log> findLogsBySignRequestIdEquals(long signRequestId, String sortFieldName, String sortOrder) {
        EntityManager em = Log.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM Log AS o WHERE o.signRequestId = :signRequestId");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<Log> q = em.createQuery(queryBuilder.toString(), Log.class);
        q.setParameter("signRequestId", signRequestId);
        return q;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
