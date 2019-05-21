package org.esupportail.esupsignature.domain;

import java.time.DayOfWeek;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Table(name = "UserAccount")
@Configurable
public class User {
	
	private String name;
	
	private String firstname;
	
    @Column(unique=true)
    private String eppn;
    
    private String email;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private Document signImage = new Document();

    @Transient
    private String ip;
    
    @Transient
    private String signImageBase64;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private Document keystore = new Document();
	
    private boolean ready;
    
    @Enumerated(EnumType.STRING)
    private EmailAlertFrequency emailAlertFrequency;

    public enum EmailAlertFrequency {
		never, immediately, daily, weekly;
	}
    
    private String emailAlertHour;
    
    @Enumerated(EnumType.STRING)
    private DayOfWeek emailAlertDay;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSendAlertDate = new Date(0);
    
    public void setEmailAlertFrequency(EmailAlertFrequency emailAlertFrequency) {
        this.emailAlertFrequency = emailAlertFrequency;
    }

	public static Long countFindUsersByEmailEquals(String email) {
        if (email == null || email.length() == 0) throw new IllegalArgumentException("The email argument is required");
        EntityManager em = User.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM User AS o WHERE o.email = :email", Long.class);
        q.setParameter("email", email);
        return ((Long) q.getSingleResult());
    }

	public static Long countFindUsersByEppnEquals(String eppn) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = User.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM User AS o WHERE o.eppn = :eppn", Long.class);
        q.setParameter("eppn", eppn);
        return ((Long) q.getSingleResult());
    }

	public static TypedQuery<User> findUsersByEmailEquals(String email) {
        if (email == null || email.length() == 0) throw new IllegalArgumentException("The email argument is required");
        EntityManager em = User.entityManager();
        TypedQuery<User> q = em.createQuery("SELECT o FROM User AS o WHERE o.email = :email", User.class);
        q.setParameter("email", email);
        return q;
    }

	public static TypedQuery<User> findUsersByEmailEquals(String email, String sortFieldName, String sortOrder) {
        if (email == null || email.length() == 0) throw new IllegalArgumentException("The email argument is required");
        EntityManager em = User.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM User AS o WHERE o.email = :email");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<User> q = em.createQuery(queryBuilder.toString(), User.class);
        q.setParameter("email", email);
        return q;
    }

	public static TypedQuery<User> findUsersByEppnEquals(String eppn) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = User.entityManager();
        TypedQuery<User> q = em.createQuery("SELECT o FROM User AS o WHERE o.eppn = :eppn", User.class);
        q.setParameter("eppn", eppn);
        return q;
    }

	public static TypedQuery<User> findUsersByEppnEquals(String eppn, String sortFieldName, String sortOrder) {
        if (eppn == null || eppn.length() == 0) throw new IllegalArgumentException("The eppn argument is required");
        EntityManager em = User.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM User AS o WHERE o.eppn = :eppn");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<User> q = em.createQuery(queryBuilder.toString(), User.class);
        q.setParameter("eppn", eppn);
        return q;
    }

	@PersistenceContext(unitName = "persistenceUnit")
    transient EntityManager entityManager;

	public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("name", "firstname", "eppn", "email", "signImage", "ip", "signImageBase64", "keystore", "ready", "emailAlertFrequency", "emailAlertHour", "emailAlertDay", "lastSendAlertDate");

	public static final EntityManager entityManager() {
        EntityManager em = new User().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countUsers() {
        return entityManager().createQuery("SELECT COUNT(o) FROM User o", Long.class).getSingleResult();
    }

	public static List<User> findAllUsers() {
        return entityManager().createQuery("SELECT o FROM User o", User.class).getResultList();
    }

	public static List<User> findAllUsers(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM User o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, User.class).getResultList();
    }

	public static User findUser(Long id) {
        if (id == null) return null;
        return entityManager().find(User.class, id);
    }

	public static List<User> findUserEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM User o", User.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public static List<User> findUserEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM User o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, User.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
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
            User attached = User.findUser(this.id);
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
    public User merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        User merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

	public String getName() {
        return this.name;
    }

	public void setName(String name) {
        this.name = name;
    }

	public String getFirstname() {
        return this.firstname;
    }

	public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

	public String getEppn() {
        return this.eppn;
    }

	public void setEppn(String eppn) {
        this.eppn = eppn;
    }

	public String getEmail() {
        return this.email;
    }

	public void setEmail(String email) {
        this.email = email;
    }

	public Document getSignImage() {
        return this.signImage;
    }

	public void setSignImage(Document signImage) {
        this.signImage = signImage;
    }

	public String getIp() {
        return this.ip;
    }

	public void setIp(String ip) {
        this.ip = ip;
    }

	public String getSignImageBase64() {
        return this.signImageBase64;
    }

	public void setSignImageBase64(String signImageBase64) {
        this.signImageBase64 = signImageBase64;
    }

	public Document getKeystore() {
        return this.keystore;
    }

	public void setKeystore(Document keystore) {
        this.keystore = keystore;
    }

	public boolean isReady() {
        return this.ready;
    }

	public void setReady(boolean ready) {
        this.ready = ready;
    }

	public EmailAlertFrequency getEmailAlertFrequency() {
        return this.emailAlertFrequency;
    }

	public String getEmailAlertHour() {
        return this.emailAlertHour;
    }

	public void setEmailAlertHour(String emailAlertHour) {
        this.emailAlertHour = emailAlertHour;
    }

	public DayOfWeek getEmailAlertDay() {
        return this.emailAlertDay;
    }

	public void setEmailAlertDay(DayOfWeek emailAlertDay) {
        this.emailAlertDay = emailAlertDay;
    }

	public Date getLastSendAlertDate() {
        return this.lastSendAlertDate;
    }

	public void setLastSendAlertDate(Date lastSendAlertDate) {
        this.lastSendAlertDate = lastSendAlertDate;
    }
}
