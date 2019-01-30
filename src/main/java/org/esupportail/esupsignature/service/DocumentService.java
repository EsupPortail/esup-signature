package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {
	
	@Resource
	private FileService fileService;

	public Document addFile(Document file) {
		file.persist();
		return file;
    }
	
	public Document addFile(String base64File, String name, String type) throws IOException {
		return addFile(fileService.fromBase64Image(base64File, name), name, type);
    }

	
	public Document addFile(File file, String name, String type) throws FileNotFoundException, IOException {
		return addFile(new FileInputStream(file), name, file.length(), type);
    }
	
	public Document addFile(MultipartFile multipartFile, String name) throws IOException {
		return addFile(multipartFile.getInputStream(), name, multipartFile.getSize(), multipartFile.getContentType());
    }
	
	@Transactional
	public Document addFile(InputStream inputStream, String name, long size, String contentType) throws IOException {
        Document document = new Document();
        document.setFileName(name);
        document.getBigFile().setBinaryFileStream(inputStream, size);
        document.getBigFile().persist();
        document.setSize(size);
        document.setContentType(contentType);
        document.persist();
        return document;
    }
	
}
