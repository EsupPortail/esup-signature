package org.esupportail.esupsignature.entity;

import java.time.DayOfWeek;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Entity
@Table(name = "UserAccount")
public class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;
	
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
    
    @Enumerated(EnumType.STRING)
    private EmailAlertFrequency emailAlertFrequency;

    public enum EmailAlertFrequency {
		never, immediately, daily, weekly;
	}
    
    private String emailAlertHour;
    
    @Enumerated(EnumType.STRING)
    private DayOfWeek emailAlertDay;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSendAlertDate = new Date(0);
    
    public void setEmailAlertFrequency(EmailAlertFrequency emailAlertFrequency) {
        this.emailAlertFrequency = emailAlertFrequency;
    }

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
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

	public String getFirstname() {
        return this.firstname;
    }

	public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

	public String getEppn() {
        return this.eppn;
    }

	public void setEppn(String eppn) {
        this.eppn = eppn;
    }

	public String getEmail() {
        return this.email;
    }

	public void setEmail(String email) {
        this.email = email;
    }

	public Document getSignImage() {
        return this.signImage;
    }

	public void setSignImage(Document signImage) {
        this.signImage = signImage;
    }

	public String getIp() {
        return this.ip;
    }

	public void setIp(String ip) {
        this.ip = ip;
    }

	public String getSignImageBase64() {
        return this.signImageBase64;
    }

	public void setSignImageBase64(String signImageBase64) {
        this.signImageBase64 = signImageBase64;
    }

	public Document getKeystore() {
        return this.keystore;
    }

	public void setKeystore(Document keystore) {
        this.keystore = keystore;
    }

	public boolean isReady() {
        return this.ready;
    }

	public void setReady(boolean ready) {
        this.ready = ready;
    }

	public EmailAlertFrequency getEmailAlertFrequency() {
        return this.emailAlertFrequency;
    }

	public String getEmailAlertHour() {
        return this.emailAlertHour;
    }

	public void setEmailAlertHour(String emailAlertHour) {
        this.emailAlertHour = emailAlertHour;
    }

	public DayOfWeek getEmailAlertDay() {
        return this.emailAlertDay;
    }

	public void setEmailAlertDay(DayOfWeek emailAlertDay) {
        this.emailAlertDay = emailAlertDay;
    }

	public Date getLastSendAlertDate() {
        return this.lastSendAlertDate;
    }

	public void setLastSendAlertDate(Date lastSendAlertDate) {
        this.lastSendAlertDate = lastSendAlertDate;
    }
}
