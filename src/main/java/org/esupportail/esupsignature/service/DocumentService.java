package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.BigFile;
import org.esupportail.esupsignature.domain.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
	
	@Resource
	private FileService fileService;

	public Document createDocument(String base64File, String name, String contentType) throws IOException {
		return createDocument(fileService.fromBase64Image(base64File, name), name, contentType);
    }

	
	public Document createDocument(File file, String name, String contentType) throws FileNotFoundException, IOException {
		return createDocument(new FileInputStream(file), name, file.length(), contentType);
    }
	
	public Document createDocument(MultipartFile multipartFile, String name) throws IOException {
		if ((multipartFile != null) && !multipartFile.isEmpty()) {
			return createDocument(multipartFile.getInputStream(), name, multipartFile.getSize(), multipartFile.getContentType());
		}
		return null;
    }
	
	public Document createDocument(InputStream inputStream, String name, long size, String contentType) throws IOException {
        return persistDocument(inputStream, name, size, contentType);
    }

	
	public Document persistDocument(InputStream inputStream, String name, long size, String contentType) throws IOException {
		Document document = new Document();
		document.setCreateDate(new Date());
        document.setFileName(name);
        BigFile bigFile = new BigFile();
        bigFile.setBinaryFileStream(inputStream, size);
        bigFile.persist();
        document.setBigFile(bigFile);
        document.setSize(size);
        document.setContentType(contentType);
        document.persist();
        return document;
	}
	
	/* copy and close inputstream */  
	public static InputStream clone(final InputStream inputStream) {
        try {
            inputStream.mark(0);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int readLength = 0;
            while ((readLength = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readLength);
            }
            outputStream.flush();
            inputStream.close();
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
	
}
