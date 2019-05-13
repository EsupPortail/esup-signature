package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders={"findSignBooksBySignRequestsEquals", "findSignBooksByRecipientEmailsEquals", "findSignBooksByModeratorEmailsEquals", "findSignBooksByCreateByEquals", "findSignBooksByNameEquals", "findSignBooksBySignBookTypeEquals", "findSignBooksByRecipientEmailsAndSignBookTypeEquals"})
public class SignBook {

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

    //TODO : mini workflow
    @ManyToMany(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.ALL })
    private List<SignBook> SignBooks = new ArrayList<SignBook>();
    
    private boolean autoRemove = false;
    
    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;
    
    private String documentsTargetUri;    

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.ALL }, orphanRemoval = true)
    private Document modelFile = new Document();
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.ALL })
    private List<SignRequest> signRequests = new ArrayList<SignRequest>();

    @ManyToOne(fetch = FetchType.LAZY)
    private SignRequestParams signRequestParams = new SignRequestParams();
	
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
        EntityManager em = SignBook.entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.signBookType = :signBookType", SignBook.class);
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
    public static TypedQuery<SignBook> findSignBooksBySignBookTypeEquals(SignBookType signBookType, String sortFieldName, String sortOrder) {
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
    
    public static Long countFindSignBooksByRecipientEmailsAndSignBookTypeEquals(List<String> recipientEmails, SignBookType signBookType) {
        if (recipientEmails == null) throw new IllegalArgumentException("The recipientEmails argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
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
        EntityManager em = SignBook.entityManager();
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
    
}
