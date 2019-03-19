package org.esupportail.esupsignature.domain;

import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(versionField = "", table = "UserAccount", finders={"findUsersByEppnEquals", "findUsersByEmailEquals"})
public class User {
	
	private String name;
	
	private String firstname;
	
    @Column(unique=true)
    private String eppn;
    
    private String email;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private Document signImage = new Document();

    @Transient
    private String ip;
    
    @Transient
    private String signImageBase64;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private Document keystore = new Document();
	
    private boolean ready;
    
    private EmailAlertFrequency emailAlertFrequency;

    public enum EmailAlertFrequency {
		never, immediately, daily, weekly;
	}
    
    private String emailAlertHour;
    
    private String emailAlertDay;
    
    public void setEmailAlertFrequency(EmailAlertFrequency emailAlertFrequency) {
        this.emailAlertFrequency = emailAlertFrequency;
    }
}
