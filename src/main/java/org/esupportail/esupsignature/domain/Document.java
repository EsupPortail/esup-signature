package org.esupportail.esupsignature.domain;

import java.util.Date;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
public class Document {

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

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private File originalFile = new File();
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private File signedFile = new File();
    
    @Enumerated(EnumType.STRING)
	private DocStatus status;	
	
	public enum DocStatus {
		start, pending, canceled, checked, signed, deleted;
	}
    
    public void setStatus(DocStatus status) {
        this.status = status;
    }
}
