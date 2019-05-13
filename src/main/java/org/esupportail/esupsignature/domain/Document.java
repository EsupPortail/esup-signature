package org.esupportail.esupsignature.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

import com.google.common.io.Files;

@RooJavaBean
@RooToString(excludeFields = { "bigFile", "file" })
@RooJpaActiveRecord
public class Document {

	private static final Logger logger = LoggerFactory.getLogger(Document.class);

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
			return targetFile;
    	} catch (SQLException | IOException e) {
    		logger.error("error to convert BigFile to java.io.File", e);
		}
    	return null;
	}
}
