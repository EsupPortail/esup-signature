package org.esupportail.esupsignature.domain;

import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.web.multipart.MultipartFile;

@RooJavaBean
@RooToString
@RooJpaActiveRecord
public class File {

	private String fileName;

    @Transient
    @Size(max = 255)
    private String url;

    @Transient
    private MultipartFile file;

    private Long size;

    private String contentType;

    public String getUrl() {
        return "./file/show/" + getId();
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST }, orphanRemoval = true)
    private BigFile fichier = new BigFile();
    
}
