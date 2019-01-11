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
@RooToString(excludeFields = { "bigFile", "file" })
@RooJpaActiveRecord
public class File {

	private String fileName;

    @Transient
    @Size(max = 255)
    private String url;

    @Transient
    private MultipartFile multipartFile;

    private Long size;

    private String contentType;

    public String getUrl() {
        return "/manager/documents/getfile/" + getId();
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = { javax.persistence.CascadeType.ALL}, orphanRemoval = true)
    private BigFile bigFile = new BigFile();
    
}
