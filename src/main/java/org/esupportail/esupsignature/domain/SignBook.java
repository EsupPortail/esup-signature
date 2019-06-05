package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.Version;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Configurable
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignBook {

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
	
	@Column(unique=true)
	private String name;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;
    
    @Size(max = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    private DocumentIOType sourceType;
    
    private String documentsSourceUri;
    
    @ElementCollection(targetClass=String.class)
    private List<String> moderatorEmails = new ArrayList<String>();

    @ElementCollection(targetClass=String.class)
    private List<String> recipientEmails = new ArrayList<String>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST})
    private List<SignBook> SignBooks = new ArrayList<SignBook>();
    
    //TODO renomer autoWorkflow
    private boolean autoRemove = true;
    
    //TODO alerte au moment de la cloture
    
    //TODO alerte + validation de la demande par le gestionnaire
    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;
    
    private String documentsTargetUri;    

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.ALL }, orphanRemoval = true)
    private Document modelFile = new Document();
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL })
    private List<SignRequest> signRequests = new ArrayList<SignRequest>();

    //TODO multiple params + steps ou nb signatures
    @ManyToMany(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.MERGE, javax.persistence.CascadeType.PERSIST})
    private List<SignRequestParams> signRequestParams = new ArrayList<SignRequestParams>();
	
	@Enumerated(EnumType.STRING)
	private SignBookType signBookType;
	
	public enum SignBookType {
		system, user, group, workflow;
	}
	
    public enum DocumentIOType {
		none, cifs, vfs, cmis, mail;
	}
    
    public String getRecipientEmailsLabels() {
    	String recipientEmailsLabels = "";
		for(String recipientEmail : recipientEmails) {
			recipientEmailsLabels += "<li>" + recipientEmail + "</li>";
		}
		return recipientEmailsLabels;
    }
    
    public String getModeratorEmailsLabels() {
    	String modetatorEmailsLabels = "";
		for(String modetatorEmail : moderatorEmails) {
			modetatorEmailsLabels += "<li>" + modetatorEmail + "</li>";
		}
		return modetatorEmailsLabels;
    }
    
    public void setSourceType(DocumentIOType sourceType) {
        this.sourceType = sourceType;
    }
    
    public void setTargetType(DocumentIOType targetType) {
        this.targetType = targetType;
    }
    
    public void setSignBookType(SignBookType signBookType) {
        this.signBookType = signBookType;
    }
    
    public static TypedQuery<SignBook> findSignBooksBySignBookTypeEquals(SignBookType signBookType) {
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.signBookType = :signBookType", SignBook.class);
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksBySignBookTypeEquals(SignBookType signBookType, String sortFieldName, String sortOrder) {
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
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
    
    public static Long countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(List<String> recipientEmails, SignBookType signBookType) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(o) FROM SignBook AS o WHERE o.signBookType = :signBookType");
        queryBuilder.append(" AND");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        TypedQuery<Long> q = em.createQuery(queryBuilder.toString(), Long.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        q.setParameter("signBookType", signBookType);
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<SignBook> findSignBooksByRecipientEmailsAndSignBookTypeEquals(List<String> recipientEmails, SignBookType signBookType) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.signBookType = :signBookType");
        queryBuilder.append(" AND");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
    public static Long countFindSignBooksByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.createBy = :createBy", Long.class);
        q.setParameter("createBy", createBy);
        return ((Long) q.getSingleResult());
    }
    
    public static Long countFindSignBooksByModeratorEmailsEquals(List<String> moderatorEmails) {
        if (moderatorEmails == null) throw new IllegalArgumentException("The moderatorEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(o) FROM SignBook AS o WHERE");
        for (int i = 0; i < moderatorEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :moderatorEmails_item").append(i).append(" MEMBER OF o.moderatorEmails");
        }
        TypedQuery q = em.createQuery(queryBuilder.toString(), Long.class);
        int moderatorEmailsIndex = 0;
        for (String _string: moderatorEmails) {
            q.setParameter("moderatorEmails_item" + moderatorEmailsIndex++, _string);
        }
        return ((Long) q.getSingleResult());
    }
    
    public static Long countFindSignBooksByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.name = :name", Long.class);
        q.setParameter("name", name);
        return ((Long) q.getSingleResult());
    }
    
    public static Long countFindSignBooksByRecipientEmailsEquals(List<String> recipientEmails) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(o) FROM SignBook AS o WHERE");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        TypedQuery q = em.createQuery(queryBuilder.toString(), Long.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        return ((Long) q.getSingleResult());
    }
    
    public static Long countFindSignBooksBySignBookTypeEquals(SignBookType signBookType) {
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.signBookType = :signBookType", Long.class);
        q.setParameter("signBookType", signBookType);
        return ((Long) q.getSingleResult());
    }
    
    public static Long countFindSignBooksBySignRequestsEquals(List<SignRequest> signRequests) {
        if (signRequests == null) throw new IllegalArgumentException("The signRequests argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT COUNT(o) FROM SignBook AS o WHERE");
        for (int i = 0; i < signRequests.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :signRequests_item").append(i).append(" MEMBER OF o.signRequests");
        }
        TypedQuery q = em.createQuery(queryBuilder.toString(), Long.class);
        int signRequestsIndex = 0;
        for (SignRequest _signrequest: signRequests) {
            q.setParameter("signRequests_item" + signRequestsIndex++, _signrequest);
        }
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<SignBook> findSignBooksByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.createBy = :createBy", SignBook.class);
        q.setParameter("createBy", createBy);
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByCreateByEquals(String createBy, String sortFieldName, String sortOrder) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = entityManager();
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
    
    public static TypedQuery<SignBook> findSignBooksByModeratorEmailsEquals(List<String> moderatorEmails) {
        if (moderatorEmails == null) throw new IllegalArgumentException("The moderatorEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < moderatorEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :moderatorEmails_item").append(i).append(" MEMBER OF o.moderatorEmails");
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int moderatorEmailsIndex = 0;
        for (String _string: moderatorEmails) {
            q.setParameter("moderatorEmails_item" + moderatorEmailsIndex++, _string);
        }
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByModeratorEmailsEquals(List<String> moderatorEmails, String sortFieldName, String sortOrder) {
        if (moderatorEmails == null) throw new IllegalArgumentException("The moderatorEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < moderatorEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :moderatorEmails_item").append(i).append(" MEMBER OF o.moderatorEmails");
        }
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" " + sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int moderatorEmailsIndex = 0;
        for (String _string: moderatorEmails) {
            q.setParameter("moderatorEmails_item" + moderatorEmailsIndex++, _string);
        }
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.name = :name", SignBook.class);
        q.setParameter("name", name);
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByNameEquals(String name, String sortFieldName, String sortOrder) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = entityManager();
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
    
    public static TypedQuery<SignBook> findSignBooksByRecipientEmailsAndSignBookTypeEquals(List<String> recipientEmails, SignBookType signBookType, String sortFieldName, String sortOrder) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE o.signBookType = :signBookType");
        queryBuilder.append(" AND");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" " + sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByRecipientEmailsEquals(List<String> recipientEmails) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksByRecipientEmailsEquals(List<String> recipientEmails, String sortFieldName, String sortOrder) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < recipientEmails.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :recipientEmails_item").append(i).append(" MEMBER OF o.recipientEmails");
        }
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" " + sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int recipientEmailsIndex = 0;
        for (String _string: recipientEmails) {
            q.setParameter("recipientEmails_item" + recipientEmailsIndex++, _string);
        }
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksBySignRequestsEquals(List<SignRequest> signRequests) {
        if (signRequests == null) throw new IllegalArgumentException("The signRequests argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < signRequests.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :signRequests_item").append(i).append(" MEMBER OF o.signRequests");
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int signRequestsIndex = 0;
        for (SignRequest _signrequest: signRequests) {
            q.setParameter("signRequests_item" + signRequestsIndex++, _signrequest);
        }
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksBySignRequestsEquals(List<SignRequest> signRequests, String sortFieldName, String sortOrder) {
        if (signRequests == null) throw new IllegalArgumentException("The signRequests argument is required");
        EntityManager em = entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignBook AS o WHERE");
        for (int i = 0; i < signRequests.size(); i++) {
            if (i > 0) queryBuilder.append(" AND");
            queryBuilder.append(" :signRequests_item").append(i).append(" MEMBER OF o.signRequests");
        }
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" " + sortOrder);
            }
        }
        TypedQuery<SignBook> q = em.createQuery(queryBuilder.toString(), SignBook.class);
        int signRequestsIndex = 0;
        for (SignRequest _signrequest: signRequests) {
            q.setParameter("signRequests_item" + signRequestsIndex++, _signrequest);
        }
        return q;
    }
    
    @PersistenceContext(unitName = "persistenceUnit")
    transient EntityManager entityManager;
    
    public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("name", "createDate", "createBy", "updateDate", "updateBy", "description", "sourceType", "documentsSourceUri", "moderatorEmails", "recipientEmails", "SignBooks", "autoRemove", "targetType", "documentsTargetUri", "modelFile", "signRequests", "signRequestParams", "signBookType");
    
	public static final EntityManager entityManager() {
        EntityManager em = new SignBook().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }
    
    public static long countSignBooks() {
        return entityManager().createQuery("SELECT COUNT(o) FROM SignBook o", Long.class).getSingleResult();
    }
    
    public static List<SignBook> findAllSignBooks() {
        return entityManager().createQuery("SELECT o FROM SignBook o", SignBook.class).getResultList();
    }
    
    public static List<SignBook> findAllSignBooks(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignBook o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignBook.class).getResultList();
    }
    
    public static SignBook findSignBook(Long id) {
        if (id == null) return null;
        return entityManager().find(SignBook.class, id);
    }
    
    public static List<SignBook> findSignBookEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM SignBook o", SignBook.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }
    
    public static List<SignBook> findSignBookEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignBook o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignBook.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
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
            SignBook attached = findSignBook(this.id);
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
    public SignBook merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        SignBook merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Date getCreateDate() {
        return this.createDate;
    }
    
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public String getCreateBy() {
        return this.createBy;
    }
    
    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
    
    public Date getUpdateDate() {
        return this.updateDate;
    }
    
    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    public String getUpdateBy() {
        return this.updateBy;
    }
    
    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public DocumentIOType getSourceType() {
        return this.sourceType;
    }
    
    public String getDocumentsSourceUri() {
        return this.documentsSourceUri;
    }
    
    public void setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }
    
    public List<String> getModeratorEmails() {
        return this.moderatorEmails;
    }
    
    public void setModeratorEmails(List<String> moderatorEmails) {
        this.moderatorEmails = moderatorEmails;
    }
    
    public List<String> getRecipientEmails() {
        return this.recipientEmails;
    }
    
    public void setRecipientEmails(List<String> recipientEmails) {
        this.recipientEmails = recipientEmails;
    }
    
    public List<SignBook> getSignBooks() {
        return this.SignBooks;
    }
    
    public void setSignBooks(List<SignBook> SignBooks) {
        this.SignBooks = SignBooks;
    }
    
    public boolean isAutoRemove() {
        return this.autoRemove;
    }
    
    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }
    
    public DocumentIOType getTargetType() {
        return this.targetType;
    }
    
    public String getDocumentsTargetUri() {
        return this.documentsTargetUri;
    }
    
    public void setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }
    
    public Document getModelFile() {
        return this.modelFile;
    }
    
    public void setModelFile(Document modelFile) {
        this.modelFile = modelFile;
    }
    
    public List<SignRequest> getSignRequests() {
        return this.signRequests;
    }
    
    public void setSignRequests(List<SignRequest> signRequests) {
        this.signRequests = signRequests;
    }
    
    public List<SignRequestParams> getSignRequestParams() {
        return this.signRequestParams;
    }
    
    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }
    
    public SignBookType getSignBookType() {
        return this.signBookType;
    }
}
