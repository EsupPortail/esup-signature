package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.esupportail.esupsignature.service.SignBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders={"findSignRequestsByNameEquals", "findSignRequestsByCreateByEquals", "findSignRequestsByCreateByAndStatusEquals"})
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
    
}
