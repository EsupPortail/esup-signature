package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
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
@RooJpaActiveRecord(finders={"findSignBooksByCreateByEquals", "findSignBooksByRecipientEmailEquals"})
public class SignBook {

	String name;

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

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;
    
    private String documentsTargetUri;    

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Document modelFile = new Document();
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<SignRequest> signRequests = new ArrayList<SignRequest>();

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> params = new HashMap<String, String>();
	
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
		certSign, pdfImageStamp, nexuSign, validate;
	}
	
	public enum NewPageType {
		none, onBegin, onEnd;
	}
	/*
	@Enumerated(EnumType.STRING)
	private SignBookType signBookType;
	
	public enum SignBookType {
		oneShot, fixedParams, fixedIO, fixedParamsAndIO, custom;
	}
	*/
    public enum DocumentIOType {
		cifs, vfs, opencmis, mail;
	}
    
    public void setSourceType(DocumentIOType sourceType) {
        this.sourceType = sourceType;
    }
    
    public void setTargetType(DocumentIOType targetType) {
        this.targetType = targetType;
    }
}
