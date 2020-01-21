package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.BigFileRepository;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DocumentService {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private FileService fileService;

	@Resource
	private BigFileService bigFileService;

	@Resource
	private BigFileRepository bigFileRepository;

	public Document createDocument(InputStream inputStream, String name, String contentType) throws IOException {
		Document document = new Document();
		document.setCreateDate(new Date());
		document.setFileName(name);
		document.setContentType(contentType);
		BigFile bigFile = new BigFile();
		long size = inputStream.available();
		bigFileService.setBinaryFileStream(bigFile, inputStream, size);
		document.setBigFile(bigFile);
		document.setSize(size);
		documentRepository.save(document);
		return document;
	}
	
	public void deleteDocument(Document document) {
		BigFile bigFile = document.getBigFile();
		if(bigFile != null) {
			document.setBigFile(null);
			documentRepository.save(document);
			bigFileRepository.delete(bigFile);
		}
		documentRepository.delete(document);
	}

	public String getFormatedName(String originalName, int order) {
		String name = "";
		name += String.format("%02d", order);
		name += "_";
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		name += format.format(new Date());
		name += "_";
		name += fileService.getNameOnly(originalName).replaceAll(" ", "-");
		name += "." + fileService.getExtension(originalName);
		return name;
	}

	public String getSignedName(String originalName) {
		String name = "";
		name += fileService.getNameOnly(originalName).replaceAll(" ", "-");
		name += "_signed";
		name += "_";
		name += "." + fileService.getExtension(originalName);
		return name;
	}

}
