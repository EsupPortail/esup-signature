package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.*;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

@Entity
@Configurable
public class Document {

	private static final Logger logger = LoggerFactory.getLogger(Document.class);

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

	private String fileName;

    private Long size;

    private Long nbPages;

    private Long parentId;

    private String contentType;

    private String pdfaCheck = "";

    @ManyToOne
    private User createBy;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm")
    private Date createDate;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
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

    public Long getNbPages() {
        return nbPages;
    }

    public void setNbPages(Long nbPages) {
        this.nbPages = nbPages;
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

    public String getPdfaCheck() {
        if(pdfaCheck == null) return "";
        return pdfaCheck;
    }

    public void setPdfaCheck(String pdfaCheck) {
        this.pdfaCheck = pdfaCheck;
    }

    public User getCreateBy() {
        return createBy;
    }

    public void setCreateBy(User createBy) {
        this.createBy = createBy;
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

	

	

    public InputStream getTransientInputStream() {
        return transientInputStream;
    }

    public void setTransientInputStream(InputStream transientInputStream) {
        this.transientInputStream = transientInputStream;
    }
}
