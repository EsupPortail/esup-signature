package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Entity
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    private Boolean signature;

    private String urlId;

    private String phoneNumber;

    @ManyToOne
    private User user;

    private String password;

    @ManyToOne(cascade = CascadeType.DETACH)
    private SignBook signBook;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    private Boolean smsSended = false;

    private Integer tries = 0;

    private Boolean forceSms = false;

    @Transient
    transient private String mailFrom;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean isSignature() {
        if(signature == null) {
            return true;
        }
        return signature;
    }

    public void setSignature(boolean signature) {
        this.signature = signature;
    }

    public String getUrlId() {
        return urlId;
    }

    public void setUrlId(String urlId) {
        this.urlId = urlId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public SignBook getSignBook() {
        return signBook;
    }

    public void setSignBook(SignBook signBook) {
        this.signBook = signBook;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public boolean getSmsSended() {
        return smsSended;
    }

    public void setSmsSended(boolean smsSended) {
        this.smsSended = smsSended;
    }

    public int getTries() {
        return tries;
    }

    public void setTries(int tries) {
        this.tries = tries;
    }

    public boolean isForceSms() {
        return forceSms;
    }

    public void setForceSms(boolean forceSms) {
        this.forceSms = forceSms;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }
}
