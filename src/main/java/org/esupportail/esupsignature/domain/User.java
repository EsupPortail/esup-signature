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
@RooJpaActiveRecord(versionField = "", table = "UserAccount", finders={"findUsersByEppnEquals" })
public class User {
	
	private String name;
	
	private String firstname;
	
    @Column(unique=true)
    private String eppn;
    
    private String email;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Content signImage = new Content();
    
    @Transient
    private String publicKey;

    @Transient
    private String password;
    
    @Transient
    private String signImageBase64;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private Content keystore = new Content();
	
}
