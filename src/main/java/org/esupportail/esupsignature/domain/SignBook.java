package org.esupportail.esupsignature.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
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
    
    @OneToOne
    private User userSource;
    
    @OneToOne
    private User userSourceSecretary;
    
    @OneToOne
    private User userDestination;

    @OneToOne
    private User userDestinationSecretary;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<SignRequest> documents = new ArrayList<SignRequest>();
    
    @Enumerated(EnumType.STRING)
	private SignType signType;
    
    public enum SignType {
		validation, simpleSign, certSign, nexuSign
	}
    
    @Enumerated(EnumType.STRING)
	private BookStatus status;	
	
	public enum BookStatus {
		started, pending, canceled, completed, deleted;
	}
	
}
