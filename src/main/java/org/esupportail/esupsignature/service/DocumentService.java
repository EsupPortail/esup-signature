package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DocumentService {
	
	@Autowired
	private DocumentRepository documentRepository;
	
	@Resource
	private BigFileService bigFileService;
	
	@Resource
	private FileService fileService;

	public Document createDocument(String base64File, String name) throws IOException {
		return createDocument(fileService.fromBase64Image(base64File, name), name);
    }

	public Document createDocument(File file, String name) throws IOException {
		return createDocument(new FileInputStream(file), name, Files.probeContentType(file.toPath()));
    }
	
	public List<Document> createDocuments(MultipartFile[] multipartFiles) throws IOException {
		List<Document> documents = new ArrayList<>();
		for(MultipartFile multipartFile : multipartFiles) {
			documents.add(createDocument(multipartFile, multipartFile.getOriginalFilename()));
		}
		return documents;
    }
	
	public Document createDocument(MultipartFile multipartFile, String name) throws IOException {
		if ((multipartFile != null) && !multipartFile.isEmpty()) {
			return createDocument(multipartFile.getInputStream(), name, multipartFile.getContentType());
		}
		return null;
    }
	
	public Document createDocument(InputStream inputStream, String name, String contentType) throws IOException {
        return persistDocument(inputStream, name, inputStream.available(), contentType);
    }
	
	public Document persistDocument(InputStream inputStream, String name, long size, String contentType) {
		Document document = new Document();
		document.setCreateDate(new Date());
		document.setFileName(name);
		BigFile bigFile = new BigFile();
		bigFileService.setBinaryFileStream(bigFile, inputStream, size);
		document.setBigFile(bigFile);
		document.setSize(size);
		document.setContentType(contentType);
		documentRepository.save(document);
		return document;
	}
	
	public void deleteDocument(Document document) {
		BigFile bigFile = document.getBigFile();
		document.setBigFile(null);
		documentRepository.save(document);
		bigFileService.deleteBigFile(bigFile.getId());
		documentRepository.delete(document);
	}
	
}
