package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.UploadActionType;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class DocumentService {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private FileService fileService;

	@Resource
	private BigFileService bigFileService;

	@Resource
	private FsAccessFactoryService fsAccessFactoryService;

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

	public String getSignedName(String originalName) {
		String suffix = globalProperties.getSignedSuffix();
		String name = "";
		name += fileService.getNameOnly(originalName).replaceAll(" ", "-");
		if(name.endsWith(suffix)) {
			name = name.replace(suffix, "");
		}
		name += suffix;
		name += "." + fileService.getExtension(originalName);
		return name;
	}

	public String archiveDocument(Document signedFile, String path, String subPath, String name) throws EsupSignatureFsException, EsupSignatureException {
		return exportDocument(fsAccessFactoryService.getPathIOType(path), path + subPath, signedFile, name);
	}

	public String exportDocument(DocumentIOType documentIOType, String targetUrl, Document signedFile, String name) throws EsupSignatureFsException {
		String documentUri;
		FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(targetUrl);
		if(fsAccessService != null) {
			try {
				fsAccessService.createURITree(targetUrl);
				InputStream inputStream = signedFile.getInputStream();
				if(name == null) {
					name = signedFile.getFileName();
				} else {
					name = name + "." + fileService.getExtension(signedFile.getFileName());
				}
				logger.info("send to " + documentIOType.name() + " in " + targetUrl + "/" + name);
				if (fsAccessService.putFile(targetUrl, name, inputStream, UploadActionType.OVERRIDE)) {
					documentUri = targetUrl + "/" + name;
					return documentUri;
				} else {
					throw new EsupSignatureFsException("file is not exported");
				}
			} catch (EsupSignatureFsException e) {
				throw new EsupSignatureFsException("write fsaccess error : ", e);
			}
		} else {
			throw new EsupSignatureFsException("aucun fsService configuré");
		}
	}

	public Document getById(Long id) {
		return documentRepository.findById(id).get();
	}

	public void delete(Long id) {
		Document document = documentRepository.findById(id).get();
		delete(document);
	}

	public void delete(Document document) {
		documentRepository.delete(document);
	}

	public Document addSignedFile(SignRequest signRequest, InputStream signedInputStream, String originalName, String mimeType) throws IOException {
		String docName = getSignedName(originalName);
		Document document = createDocument(signedInputStream, docName, mimeType);
		document.setParentId(signRequest.getId());
		signRequest.getSignedDocuments().add(document);
		return document;
	}
}
