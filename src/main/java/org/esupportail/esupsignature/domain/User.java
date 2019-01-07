package org.esupportail.esupsignature.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.web.multipart.MultipartFile;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class User {
	
	private String name;
	
	private String firstname;
	
    @Column(unique=true)
    private String eppn;
    
    @Transient
    private MultipartFile file;

    @Transient
    private String fileTitle;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private File signImage = new File();
    
    private String publicKey;
	
}
