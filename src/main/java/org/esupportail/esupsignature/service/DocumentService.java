package org.esupportail.esupsignature.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
	
	@Resource
	private FileService fileService;

	public Document addFile(String base64File, String name, String type) throws IOException {
		return addFile(fileService.fromBase64Image(base64File, name), name, type);
    }

	
	public Document addFile(File file, String name, String type) throws FileNotFoundException, IOException {
		return addFile(new FileInputStream(file), name, file.length(), type);
    }
	
	public Document addFile(MultipartFile multipartFile, String name) throws IOException {
		return addFile(multipartFile.getInputStream(), name, multipartFile.getSize(), multipartFile.getContentType());
    }
	
	public Document addFile(InputStream inputStream, String name, long size, String contentType) throws IOException {
        Document document = persistDocument(inputStream, name, size, contentType);
        return document;
    }

	
	public Document persistDocument(InputStream inputStream, String name, long size, String contentType) throws IOException {
		Document document = new Document();
        document.setFileName(name);
        document.getBigFile().setBinaryFileStream(inputStream, size);
        document.getBigFile().persist();
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
