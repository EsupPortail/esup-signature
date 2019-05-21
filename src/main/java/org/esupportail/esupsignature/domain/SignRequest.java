package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.Version;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;

@Entity
@Configurable
public class SignRequest {
	
	protected final static Logger log = LoggerFactory.getLogger(SignRequest.class);

	@Column(unique=true)
	private String name;
	
	private String title;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;
    
    @Transient
    private String comment;
    
    @OneToMany
    private List<Document> originalDocuments = new ArrayList<Document>();
    
    @OneToMany
    private List<Document> signedDocuments = new ArrayList<Document>();
    
    private boolean overloadSignBookParams = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private SignRequestParams signRequestParams = new SignRequestParams();
    
    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;	
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, Boolean> signBooks = new HashMap<Long, Boolean>();
    
    private Integer signBooksWorkflowStep = 1;
    
    private Integer nbSign = 0;
    
    private boolean allSignToComplete = false;
    
    public enum SignRequestStatus {
		draft, pending, canceled, checked, signed, refused, deleted, exported, completed;
	}
	
    public List<SignBook> getOriginalSignBooks() {
    	return SignBook.findSignBooksBySignRequestsEquals(Arrays.asList(this)).getResultList();
    }
    
    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

    public Map<String, Boolean> getSignBooksLabels() {
    	Map<String, Boolean> signBookNames = new HashMap<>();
		for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			signBookNames.put(SignBook.findSignBook(signBookId.getKey()).getName(), signBookId.getValue());
		}
		return signBookNames;
		
    }
    
    public int countSignOk() {
    	int nbSign = 0;
		for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			if(signBookId.getValue()) {
				nbSign++;
			}
		}
		nbSign += signedDocuments.size();
		return nbSign;
    }
    
    public List<String> getSignBooksJson() {
    	List<String> result = new ArrayList<>();
    	for(Map.Entry<Long, Boolean> signBookId : signBooks.entrySet()) {
			result.add(signBookId.getKey().toString());
		}
    	return result;
    }
    
	public static TypedQuery<SignRequest> findSignRequests(String createBy, SignRequestStatus status, String searchString, Integer page, Integer size, String sortFieldName, String sortOrder) {
    	EntityManager em = SignRequest.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<SignRequest> query = criteriaBuilder.createQuery(SignRequest.class);
        Root<SignRequest> signRequestRoot = query.from(SignRequest.class);

        List<Predicate> predicates = new ArrayList<Predicate>();
        if(!createBy.isEmpty()) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("createBy"), createBy));
        }

        if(status != null) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("status"), status));
        }
        
        List<Order> orders = new ArrayList<Order>();
        if(sortOrder.equals("asc")){
        	orders.add(criteriaBuilder.asc(signRequestRoot.get(sortFieldName)));
        } else {
        	orders.add(criteriaBuilder.desc(signRequestRoot.get(sortFieldName)));
        }
        orders.add(criteriaBuilder.asc(signRequestRoot.get("status")));
        query.select(signRequestRoot);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.orderBy(orders);
        int sizeNo = size == null ? 10 : size.intValue();
        final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
        
        return em.createQuery(query).setFirstResult(firstResult).setMaxResults(sizeNo);
    }

    public static long countFindSignRequests(String createBy, SignRequestStatus status, String searchString) {
    	EntityManager em = SignRequest.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<SignRequest> signRequestRoot = query.from(SignRequest.class);

    	List<Predicate> predicates = new ArrayList<Predicate>();

        if(!createBy.isEmpty()) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("createBy"), createBy));
        }
    	
        if(searchString != null && searchString != ""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }else{
        	searchString = "";
        }
        
        if(status != null) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("status"), status));
        }
        
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(criteriaBuilder.count(signRequestRoot));
        
        return em.createQuery(query).getSingleResult();
    }

    public static Long countFindSignRequestsByCreateByAndStatusEquals(String createBy, SignRequestStatus status) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        if (status == null) throw new IllegalArgumentException("The status argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery<Long> q = em.createQuery("SELECT COUNT(o) FROM SignRequest AS o WHERE o.createBy = :createBy AND o.status = :status", Long.class);
        q.setParameter("createBy", createBy);
        q.setParameter("status", status);
        return ((Long) q.getSingleResult());
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

	@PersistenceContext
    transient EntityManager entityManager;

	public static final List<String> fieldNames4OrderClauseFilter = java.util.Arrays.asList("log", "name", "title", "createDate", "createBy", "comment", "originalDocuments", "signedDocuments", "overloadSignBookParams", "signRequestParams", "status", "signBooks", "signBooksWorkflowStep", "nbSign", "allSignToComplete");

	public static final EntityManager entityManager() {
        EntityManager em = new SignRequest().entityManager;
        if (em == null) throw new IllegalStateException("Entity manager has not been injected (is the Spring Aspects JAR configured as an AJC/AJDT aspects library?)");
        return em;
    }

	public static long countSignRequests() {
        return entityManager().createQuery("SELECT COUNT(o) FROM SignRequest o", Long.class).getSingleResult();
    }

	public static List<SignRequest> findAllSignRequests() {
        return entityManager().createQuery("SELECT o FROM SignRequest o", SignRequest.class).getResultList();
    }

	public static List<SignRequest> findAllSignRequests(String sortFieldName, String sortOrder) {
        String jpaQuery = "SELECT o FROM SignRequest o";
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            jpaQuery = jpaQuery + " ORDER BY " + sortFieldName;
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                jpaQuery = jpaQuery + " " + sortOrder;
            }
        }
        return entityManager().createQuery(jpaQuery, SignRequest.class).getResultList();
    }

	public static SignRequest findSignRequest(Long id) {
        if (id == null) return null;
        return entityManager().find(SignRequest.class, id);
    }

	public static List<SignRequest> findSignRequestEntries(int firstResult, int maxResults) {
        return entityManager().createQuery("SELECT o FROM SignRequest o", SignRequest.class).setFirstResult(firstResult).setMaxResults(maxResults).getResultList();
    }

	public static List<SignRequest> findSignRequestEntries(int firstResult, int maxResults, String sortFieldName, String sortOrder) {
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
            SignRequest attached = SignRequest.findSignRequest(this.id);
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
    public SignRequest merge() {
        if (this.entityManager == null) this.entityManager = entityManager();
        SignRequest merged = this.entityManager.merge(this);
        this.entityManager.flush();
        return merged;
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

	public String getTitle() {
        return this.title;
    }

	public void setTitle(String title) {
        this.title = title;
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

	public String getComment() {
        return this.comment;
    }

	public void setComment(String comment) {
        this.comment = comment;
    }

	public List<Document> getOriginalDocuments() {
        return this.originalDocuments;
    }

	public void setOriginalDocuments(List<Document> originalDocuments) {
        this.originalDocuments = originalDocuments;
    }

	public List<Document> getSignedDocuments() {
        return this.signedDocuments;
    }

	public void setSignedDocuments(List<Document> signedDocuments) {
        this.signedDocuments = signedDocuments;
    }

	public boolean isOverloadSignBookParams() {
        return this.overloadSignBookParams;
    }

	public void setOverloadSignBookParams(boolean overloadSignBookParams) {
        this.overloadSignBookParams = overloadSignBookParams;
    }

	public SignRequestParams getSignRequestParams() {
        return this.signRequestParams;
    }

	public void setSignRequestParams(SignRequestParams signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

	public SignRequestStatus getStatus() {
        return this.status;
    }

	public Map<Long, Boolean> getSignBooks() {
        return this.signBooks;
    }

	public void setSignBooks(Map<Long, Boolean> signBooks) {
        this.signBooks = signBooks;
    }

	public Integer getSignBooksWorkflowStep() {
        return this.signBooksWorkflowStep;
    }

	public void setSignBooksWorkflowStep(Integer signBooksWorkflowStep) {
        this.signBooksWorkflowStep = signBooksWorkflowStep;
    }

	public Integer getNbSign() {
        return this.nbSign;
    }

	public void setNbSign(Integer nbSign) {
        this.nbSign = nbSign;
    }

	public boolean isAllSignToComplete() {
        return this.allSignToComplete;
    }

	public void setAllSignToComplete(boolean allSignToComplete) {
        this.allSignToComplete = allSignToComplete;
    }

	public static Long countFindSignRequestsByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignRequest AS o WHERE o.createBy = :createBy", Long.class);
        q.setParameter("createBy", createBy);
        return ((Long) q.getSingleResult());
    }

	public static Long countFindSignRequestsByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM SignRequest AS o WHERE o.name = :name", Long.class);
        q.setParameter("name", name);
        return ((Long) q.getSingleResult());
    }

	public static TypedQuery<SignRequest> findSignRequestsByCreateByAndStatusEquals(String createBy, SignRequestStatus status) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        if (status == null) throw new IllegalArgumentException("The status argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery<SignRequest> q = em.createQuery("SELECT o FROM SignRequest AS o WHERE o.createBy = :createBy AND o.status = :status", SignRequest.class);
        q.setParameter("createBy", createBy);
        q.setParameter("status", status);
        return q;
    }

	public static TypedQuery<SignRequest> findSignRequestsByCreateByAndStatusEquals(String createBy, SignRequestStatus status, String sortFieldName, String sortOrder) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        if (status == null) throw new IllegalArgumentException("The status argument is required");
        EntityManager em = SignRequest.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignRequest AS o WHERE o.createBy = :createBy AND o.status = :status");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignRequest> q = em.createQuery(queryBuilder.toString(), SignRequest.class);
        q.setParameter("createBy", createBy);
        q.setParameter("status", status);
        return q;
    }

	public static TypedQuery<SignRequest> findSignRequestsByCreateByEquals(String createBy) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery<SignRequest> q = em.createQuery("SELECT o FROM SignRequest AS o WHERE o.createBy = :createBy", SignRequest.class);
        q.setParameter("createBy", createBy);
        return q;
    }

	public static TypedQuery<SignRequest> findSignRequestsByCreateByEquals(String createBy, String sortFieldName, String sortOrder) {
        if (createBy == null || createBy.length() == 0) throw new IllegalArgumentException("The createBy argument is required");
        EntityManager em = SignRequest.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignRequest AS o WHERE o.createBy = :createBy");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignRequest> q = em.createQuery(queryBuilder.toString(), SignRequest.class);
        q.setParameter("createBy", createBy);
        return q;
    }

	public static TypedQuery<SignRequest> findSignRequestsByNameEquals(String name) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignRequest.entityManager();
        TypedQuery<SignRequest> q = em.createQuery("SELECT o FROM SignRequest AS o WHERE o.name = :name", SignRequest.class);
        q.setParameter("name", name);
        return q;
    }

	public static TypedQuery<SignRequest> findSignRequestsByNameEquals(String name, String sortFieldName, String sortOrder) {
        if (name == null || name.length() == 0) throw new IllegalArgumentException("The name argument is required");
        EntityManager em = SignRequest.entityManager();
        StringBuilder queryBuilder = new StringBuilder("SELECT o FROM SignRequest AS o WHERE o.name = :name");
        if (fieldNames4OrderClauseFilter.contains(sortFieldName)) {
            queryBuilder.append(" ORDER BY ").append(sortFieldName);
            if ("ASC".equalsIgnoreCase(sortOrder) || "DESC".equalsIgnoreCase(sortOrder)) {
                queryBuilder.append(" ").append(sortOrder);
            }
        }
        TypedQuery<SignRequest> q = em.createQuery(queryBuilder.toString(), SignRequest.class);
        q.setParameter("name", name);
        return q;
    }
}
