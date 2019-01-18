package org.esupportail.esupsignature.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders={"findSignRequestsByCreateByEquals", "findSignRequestsByRecipientEmailEquals"})
public class SignRequest {

	String name;
	
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private String createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date updateDate;

    private String updateBy;

    private String recipientEmail;
    
    @Size(max = 500)
    private String description;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Document originalFile = new Document();
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Document signedFile = new Document();
    
    @Enumerated(EnumType.STRING)
    private SignRequestStatus status;	
    
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> params = new HashMap<String, String>();
    
	public enum SignRequestStatus {
		start, pending, canceled, checked, signed, deleted;
	}

	@Transient
	private String signType;
    
	@Transient
	private String newPageType;

	@Transient
	private String signPageNumber;
	
	@Transient
	private String xPos;

	@Transient
	private String yPos;
	
	public enum SignType {
		imageStamp, certPAdES;
		//certXAdES, nexuPAdES, nexuXAdES;
	}

	public enum NewPageType {
		none, onBegin, onEnd;
	}
	
    public void setStatus(SignRequestStatus status) {
        this.status = status;
    }
}
