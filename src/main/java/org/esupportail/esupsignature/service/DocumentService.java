package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class DocumentService {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private FileService fileService;

	@Resource
	private BigFileService bigFileService;

	@Resource
	private FsAccessFactory fsAccessFactory;

	public List<Document> getAllDocuments(){
		List<Document> list = new ArrayList<Document>();
		documentRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

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

	public String getFormatedName(String originalName, int order) {
		String name = "";
		name += String.format("%02d", order);
		name += "_";
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
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

	public String archiveDocument(Document signedFile, String path, String subPath) throws EsupSignatureException {
		return exportDocument(fsAccessFactory.getPathIOType(path), path + subPath, signedFile);
	}

	public String exportDocument(String targetUrl, Document signedFile) throws EsupSignatureException {
		return exportDocument(fsAccessFactory.getPathIOType(targetUrl), targetUrl, signedFile);
	}

	public String exportDocument(DocumentIOType documentIOType, String targetUrl, Document signedFile) throws EsupSignatureException {
		String documentUri;
		try {
			logger.info("send to " + documentIOType.name() + " in " + targetUrl);
			FsAccessService fsAccessService = fsAccessFactory.getFsAccessService(documentIOType);
			fsAccessService.createURITree(targetUrl);
			InputStream inputStream = signedFile.getInputStream();
			if(fsAccessService.putFile(targetUrl, signedFile.getFileName(), inputStream, UploadActionType.OVERRIDE)){
				documentUri = targetUrl + "/" + signedFile.getFileName();
				if(fsAccessService.getFileFromURI(documentUri) != null) {
					return documentUri;
				} else {
					throw new EsupSignatureException("file is not exported");
				}
			} else {
				throw new EsupSignatureException("file is not exported");
			}
		} catch (EsupSignatureFsException e) {
			throw new EsupSignatureException("write fsaccess error : ", e);
		}
	}

}
