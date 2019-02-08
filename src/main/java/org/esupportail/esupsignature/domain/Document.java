package org.esupportail.esupsignature.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

import com.google.common.io.Files;

@RooJavaBean
@RooToString(excludeFields = { "bigFile", "file" })
@RooJpaActiveRecord
public class Document {

	private static final Logger log = LoggerFactory.getLogger(BigFile.class);

	private String fileName;

    private Long size;

    private String contentType;

    public String getUrl() {
        return "/manager/documents/getfile/" + getId();
    }

    @OneToOne(fetch = FetchType.LAZY, cascade = {javax.persistence.CascadeType.ALL}, orphanRemoval = true)
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
    		log.error("error to convert BigFile to java.io.File", e);
		}
    	return null;
	}
}
