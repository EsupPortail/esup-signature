package org.esupportail.esupsignature.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.BigFile;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.UploadActionType;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DocumentService {

	private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

	private final GlobalProperties globalProperties;

	private final DocumentRepository documentRepository;

	private final FileService fileService;

	private final BigFileService bigFileService;

	private final FsAccessFactoryService fsAccessFactoryService;

    public DocumentService(GlobalProperties globalProperties, DocumentRepository documentRepository, FileService fileService, BigFileService bigFileService, FsAccessFactoryService fsAccessFactoryService) {
        this.globalProperties = globalProperties;
        this.documentRepository = documentRepository;
        this.fileService = fileService;
        this.bigFileService = bigFileService;
        this.fsAccessFactoryService = fsAccessFactoryService;
    }


    public List<Document> getAll() {
		List<Document> documents = new ArrayList<>();
		documentRepository.findAll().forEach(documents::add);
		return documents;
	}

	@Transactional
	public Document createDocument(InputStream inputStream, User createBy, String name, String contentType) throws IOException {
		long size = inputStream.available();
		if(size == 0) {
			logger.warn("upload aborted cause file size is 0");
			throw new EsupSignatureRuntimeException("File size is 0");
		}
		byte[] bytes = inputStream.readAllBytes();
		Document document = new Document();
		document.setCreateBy(createBy);
		document.setCreateDate(new Date());
		document.setFileName(name);
		document.setContentType(contentType);
		if(contentType.equals("application/pdf")) {
			document.setNbPages(getNbPages(new ByteArrayInputStream(bytes)));
		}
		BigFile bigFile = new BigFile();
		bigFileService.setBinaryFileStream(bigFile, new ByteArrayInputStream(bytes), size);
		document.setBigFile(bigFile);
		document.setSize(size);
		documentRepository.save(document);
		return document;
	}

	public void updateNbPages(Document document) throws IOException {
		document.setNbPages(getNbPages(document.getInputStream()));
	}

	public String getSignedName(String originalName) {
		String suffix = globalProperties.getSignedSuffix();
		String name = "";
		name += fileService.getNameOnly(originalName).replaceAll(" ", globalProperties.getFileNameSpacesReplaceBy());
		if(name.endsWith(suffix)) {
			name = name.replace(suffix, "");
		}
		name += suffix;
		name += "." + fileService.getExtension(originalName);
		return name;
	}

	public String archiveDocument(Document signedFile, String path, String subPath, String name) throws EsupSignatureFsException {
		try {
			URI baseURI = new URI(path.replace(" ", "%20")).normalize();
			URI resolvedURI = baseURI.resolve(subPath.replace(" ", "%20")).normalize();
			return exportDocument(path + resolvedURI.getPath(), signedFile.getInputStream(), signedFile.getFileName(), name.replaceAll("[^a-zA-Z0-9]", "_"));
		} catch (EsupSignatureRuntimeException | URISyntaxException e) {
			logger.error(e.getMessage());
		}
        return null;
	}

	public String exportDocument(String targetUrl, InputStream inputStream, String fileName, String name) throws EsupSignatureRuntimeException {
		String documentUri;
		FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(targetUrl);
		if(fsAccessService != null) {
			try {
				fsAccessService.createURITree(targetUrl);
				if(name == null) {
					name = fileName;
				} else {
					String extension = fileService.getExtension(name);
					if(!StringUtils.hasText(extension) || !extension.equals(fileService.getExtension(fileName))) {
						name = name + "." + fileService.getExtension(fileName);
					}
				}
				name = sanitizeFileName(name);
				logger.info("sending to : " + targetUrl + "/" + name);
				if (fsAccessService.putFile(targetUrl, name, inputStream, UploadActionType.OVERRIDE)) {
					if(!targetUrl.endsWith("/")) {
						targetUrl += "/";
					}
					documentUri = targetUrl + name;
					return documentUri;
				} else {

					throw new EsupSignatureRuntimeException("file is not exported");
				}
			} catch (EsupSignatureFsException  e) {
				throw new EsupSignatureRuntimeException("write fsaccess error : ", e);
			}
		} else {
			throw new EsupSignatureRuntimeException("aucun fsService configuré");
		}
	}

	public String sanitizeFileName(String fileName) {
		return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
	}

	public Document getById(Long id) {
		return documentRepository.findById(id).orElseThrow();
	}

	public void delete(Long id) {
		Document document = documentRepository.findById(id).orElseThrow();
		delete(document);
	}

	public void delete(Document document) {
		documentRepository.delete(document);
	}

	@Transactional
	public Document addSignedFile(SignRequest signRequest, InputStream signedInputStream, String originalName, String mimeType, User user) throws IOException {
		String docName = getSignedName(originalName);
		Document document = createDocument(signedInputStream, user, docName, mimeType);
		document.setParentId(signRequest.getId());
		signRequest.getSignedDocuments().add(document);
		return document;
	}

	public long getNbPages(InputStream inputStream) {
		try {
			PDDocument pdDocument = Loader.loadPDF(inputStream.readAllBytes());
			return pdDocument.getNumberOfPages();
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
		}
		return 0;
	}

	@Transactional
    public void anoymize(String eppn, User anonymous) {
		List<Document> documents = documentRepository.findByCreateByEppn(eppn);
		for(Document document : documents) {
			document.setCreateBy(anonymous);
		}
    }
}
