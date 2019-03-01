package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders={"findSignRequestsByCreateByEquals", "findSignRequestsByRecipientEmailEquals"})
public class SignRequest {

	protected final static Logger log = LoggerFactory.getLogger(SignRequest.class);

	private String name;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    private String recipientEmail;
    
    @Size(max = 500)
    private String description;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Document originalFile = new Document();
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Document signedFile = new Document();
    
    @ManyToOne(fetch = FetchType.LAZY)
    private SignRequestParams signRequestParams = new SignRequestParams();
    
    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;	
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Long, Boolean> signBooks = new HashMap<Long, Boolean>();
    
    private boolean allSignToComplete = false;
    
    public enum SignRequestStatus {
		uploaded, pending, canceled, checked, signed, refused, deleted, completed;
	}
	
    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }

	public static TypedQuery<SignRequest> findSignRequests(String createBy, String recipientEmail, SignRequestStatus status, Long signBookId, String searchString, Integer page, Integer size, String sortFieldName, String sortOrder) {
    	EntityManager em = SignRequest.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<SignRequest> query = criteriaBuilder.createQuery(SignRequest.class);
        Root<SignRequest> signRequestRoot = query.from(SignRequest.class);

    	List<Predicate> predicates = new ArrayList<Predicate>();
    	Predicate predicatesOwner;
    	Predicate createByPredicates = null;
    	Predicate recipientEmailPredicates = null;
        if(!createBy.isEmpty()) {
        	createByPredicates = criteriaBuilder.equal(signRequestRoot.get("createBy"), createBy);
        }
        if(!recipientEmail.isEmpty()) {
        	recipientEmailPredicates = criteriaBuilder.equal(signRequestRoot.get("recipientEmail"), recipientEmail);
        }
        
        predicatesOwner = criteriaBuilder.or(createByPredicates, recipientEmailPredicates);
        predicates.add(predicatesOwner);
        
        if(signBookId != null) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("signBookId"), signBookId));
        }
        
        if(status != null) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("status"), status));
        }
        
        if(searchString != null && searchString != ""){
	        Expression<Boolean> fullTestSearchExpression = criteriaBuilder.function("fts", Boolean.class, criteriaBuilder.literal(searchString));
	        predicates.add(criteriaBuilder.isTrue(fullTestSearchExpression));
        }else{
        	searchString = "";
        }
        
        List<Order> orders = new ArrayList<Order>();
        orders.add(criteriaBuilder.asc(signRequestRoot.get("status")));
        if(sortOrder.equals("asc")){
        	orders.add(criteriaBuilder.asc(signRequestRoot.get(sortFieldName)));
        } else {
        	orders.add(criteriaBuilder.desc(signRequestRoot.get(sortFieldName)));
        }
        query.orderBy(orders);
        query.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        query.select(signRequestRoot);

        int sizeNo = size == null ? 10 : size.intValue();
        final int firstResult = page == null ? 0 : (page.intValue() - 1) * sizeNo;
        
        return em.createQuery(query).setFirstResult(firstResult).setMaxResults(sizeNo);
    }

    public static long countFindSignRequests(String createBy, String recipientEmail, SignRequestStatus status, String searchString) {
    	EntityManager em = SignRequest.entityManager();
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<SignRequest> signRequestRoot = query.from(SignRequest.class);

    	List<Predicate> predicates = new ArrayList<Predicate>();

        if(!createBy.isEmpty()) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("createBy"), createBy));
        }
    	
        if(!recipientEmail.isEmpty()) {
        	predicates.add(criteriaBuilder.equal(signRequestRoot.get("recipientEmail"), recipientEmail));
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

    
}
