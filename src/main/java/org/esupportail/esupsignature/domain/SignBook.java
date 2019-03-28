package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
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
@RooJpaActiveRecord(finders={"findSignBooksByCreateByEquals", "findSignBooksByRecipientEmailEquals", "findSignBooksByNameEquals", "findSignBooksByRecipientEmailAndSignBookTypeEquals"})
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
    
    private String recipientEmail;

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
		user, model, group;
	}
	
    public enum DocumentIOType {
		none, cifs, vfs, cmis, mail;
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

    public static Long countFindSignBooksByRecipientEmailAndSignBookTypeEquals(String recipientEmail, SignBookType signBookType) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery<Long> q = em.createQuery("SELECT COUNT(o) FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail AND o.signBookType = :signBookType", Long.class);
        q.setParameter("recipientEmail", recipientEmail);
        q.setParameter("signBookType", signBookType);
        return ((Long) q.getSingleResult());
    }
 
    public static TypedQuery<SignBook> findSignBooksByRecipientEmailAndSignBookTypeEquals(String recipientEmail, SignBookType signBookType) {
        if (recipientEmail == null || recipientEmail.length() == 0) throw new IllegalArgumentException("The recipientEmail argument is required");
        if (signBookType == null) throw new IllegalArgumentException("The signBookType argument is required");
        EntityManager em = SignBook.entityManager();
        TypedQuery<SignBook> q = em.createQuery("SELECT o FROM SignBook AS o WHERE o.recipientEmail = :recipientEmail AND o.signBookType = :signBookType", SignBook.class);
        q.setParameter("recipientEmail", recipientEmail);
        q.setParameter("signBookType", signBookType);
        return q;
    }
    
}
