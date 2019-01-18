package org.esupportail.esupsignature.service;

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

	public Document addFile(Document file) {
		file.persist();
		return file;
    }
	
	public Document addFile(String base64File, String name, String type) throws IOException {
		return addFile(fileService.fromBase64Image(base64File, name), type);
    }

	
	public Document addFile(File file, String type) throws FileNotFoundException, IOException {
		return addFile(new FileInputStream(file), file.getName(), file.length(), type);
    }
	
	public Document addFile(MultipartFile multipartFile) throws IOException {
		return addFile(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getSize(), multipartFile.getContentType());
    }

	public Document addFile(InputStream inputStream, String name, long size, String contentType) throws IOException {
		//TODO : à tester sans persist
        Document file = new Document();
        file.setFileName(name);
        file.getBigFile().setBinaryFileStream(inputStream, size);
        file.getBigFile().persist();
        file.setSize(size);
        file.setContentType(contentType);
        file.persist();
        return file;
    }
	
}
