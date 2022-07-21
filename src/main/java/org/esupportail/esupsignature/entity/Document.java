package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

@Entity
@Configurable
public class Document {

	private static final Logger logger = LoggerFactory.getLogger(Document.class);

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;
	
	private String fileName;

    private Long size;
    
    private Long parentId;

    private String contentType;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.REMOVE, javax.persistence.CascadeType.PERSIST}, orphanRemoval = true)
    private BigFile bigFile = new BigFile();

    @JsonIgnore
    public InputStream getInputStream() {
        try {
            if(this.bigFile != null && this.bigFile.getBinaryFile() != null) {
                return this.bigFile.getBinaryFile().getBinaryStream();
            }
        } catch (SQLException e) {
            logger.error("unable to get inputStream", e);
        }
        return null;
    }

    @Transient
    transient InputStream transientInputStream;

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

    public InputStream getTransientInputStream() {
        return transientInputStream;
    }

    public void setTransientInputStream(InputStream transientInputStream) {
        this.transientInputStream = transientInputStream;
    }
}
