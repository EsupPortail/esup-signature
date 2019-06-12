package org.esupportail.esupsignature.entity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

import com.google.common.io.Files;

@Entity
@Configurable
public class Document {

	private static final Logger logger = LoggerFactory.getLogger(Document.class);

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;
	
	private String fileName;

    private Long size;
    
    private Long parentId;

    private String contentType;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;
    
    public String getUrl() {
        return "/user/documents/getfile/" + getId();
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST}, orphanRemoval = true)
    private BigFile bigFile = new BigFile();
 
    public File getJavaIoFile() {
    	try {
			InputStream inputStream = bigFile.getBinaryFile().getBinaryStream();
		    File targetFile = new File(Files.createTempDir(), fileName);
		    OutputStream outputStream = new FileOutputStream(targetFile);
		    IOUtils.copy(inputStream, outputStream);
		    outputStream.close();
		    inputStream.close();
			return targetFile;
    	} catch (SQLException | IOException e) {
    		logger.error("error to convert BigFile to java.io.File", e);
		}
    	return null;
	}

	public String getFileName() {
        return this.fileName;
    }

	public void setFileName(String fileName) {
        this.fileName = fileName;
    }

	public Long getSize() {
        return this.size;
    }

	public void setSize(Long size) {
        this.size = size;
    }

	public Long getParentId() {
        return this.parentId;
    }

	public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

	public String getContentType() {
        return this.contentType;
    }

	public void setContentType(String contentType) {
        this.contentType = contentType;
    }

	public Date getCreateDate() {
        return this.createDate;
    }

	public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

	public BigFile getBigFile() {
        return this.bigFile;
    }

	public void setBigFile(BigFile bigFile) {
        this.bigFile = bigFile;
    }

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }


}
