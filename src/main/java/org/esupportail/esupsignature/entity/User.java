package org.esupportail.esupsignature.entity;

import org.esupportail.esupsignature.entity.enums.UserType;

import javax.persistence.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "user_account")
public class User {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	private String name;
	
	private String firstname;
	
    @Column(unique=true)
    private String eppn;

    @Column(unique=true)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    @OneToMany(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    @OrderColumn
    private List<Document> signImages = new ArrayList<>();

    @Transient
    private String ip;
    
    @Transient
    private String signImageBase64;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private Document keystore = new Document();

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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public List<Document> getSignImages() {
        return signImages;
    }

    public void setSignImages(List<Document> signImages) {
        this.signImages = signImages;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
