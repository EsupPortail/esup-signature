package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureNexuException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters.VisualSignatureRotation;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private CifsAccessImpl cifsAccessImpl;

	@Resource
	private DocumentService documentService;

	@Resource
	private SignBookService signBookService;

	@Autowired
	private SigningService signingService;

	@Resource
	private FileService fileService;

	
	@Value("${sign.pades.xFirstPos}")
	private int xFirstPos;
	@Value("${sign.pades.yFirstPos}")
	private int yFirstPos;	
	
	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, SignRequestStatus status) {
		return findSignRequestByUserAndStatusEquals(user, status, null, null);
	}
	public List<SignRequest> findSignRequestByUserAndStatusEquals(User user, SignRequestStatus status, Integer page, Integer size) {
		List<SignBook> signBooks = SignBook.findSignBooksByRecipientEmailEquals(user.getEmail()).getResultList();
		List<SignRequest> signRequests = new ArrayList<>();
		for(SignBook signBook : signBooks) {
			for(SignRequest signRequest : signBook.getSignRequests()) {
				if(status == null || signRequest.getStatus().equals(status)) {
					signRequests.add(signRequest);							
				}
			}
		}
		List<Log> logs = Log.findLogsByEppnAndActionEquals(user.getEppn(), "sign").getResultList();
		for(Log log : logs) {
			SignRequest signRequest = SignRequest.findSignRequest(log.getSignRequestId());
			if(signRequest != null && !signRequests.contains(signRequest) && (status == null || signRequest.getStatus().equals(status))) {
				signRequests.add(signRequest);
			}
		}
		signRequests = signRequests.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		
		if(page != null) {
			return signRequests.stream().skip((page - 1) * size).limit(size).collect(Collectors.toList());
		} else {
			return signRequests;
		}
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document, SignRequestParams signRequestParams, long[] signBookIds) {
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		return createSignRequest(signRequest, user, documents, signRequestParams, signBookIds );
		
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, List<Document> documents, SignRequestParams signRequestParams, long[] signBookIds) {
		signRequest.setName(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequest.setSignRequestParams(signRequestParams);
		for(long signBookId : signBookIds) {
			SignBook signBook = SignBook.findSignBook(signBookId);
			if(signBook.getSignBookType().equals(SignBookType.group)) {
				List<SignBook> signBooksFromGroup = signBook.getSignBooksGroup();
				for(SignBook signBookFromGroup : signBooksFromGroup) {
					signRequest.getSignBooks().put(signBookFromGroup.getId(), false);
					signBookFromGroup.getSignRequests().add(signRequest);

				}
			} else {
				signRequest.getSignBooks().put(signBook.getId(), false);
				signBook.getSignRequests().add(signRequest);
			}
		}
		signRequest.persist();
		for(Document document : documents) {
			document.setCreateDate(new Date());
			signRequest.getOriginalDocuments().add(document);
			document.setSignRequestId(signRequest.getId());
		}
		return signRequest;
	}
	
	public void addOriginalDocuments(SignRequest signRequest, List<Document> documents) {
		for(Document document : documents) {
			signRequest.getOriginalDocuments().add(document);
			document.setSignRequestId(signRequest.getId());
		}
	}
	
	public void addOriginalDocuments(SignRequest signRequest, Document document) {
		signRequest.getOriginalDocuments().add(document);
		document.setSignRequestId(signRequest.getId());
	}
	
	public void validate(SignRequest signRequest, User user) throws EsupSignatureIOException {
		updateInfo(signRequest, SignRequestStatus.checked, "visa", user, "SUCCESS");		
		applySignBookRules(signRequest, user);
	}

	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureIOException, EsupSignatureSignException, EsupSignatureNexuException, EsupSignatureKeystoreException {
		SignBook currentSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if(!signRequest.isOverloadSignBookParams()) {
			signRequest.getSignRequestParams().setSignType(currentSignBook.getSignRequestParams().getSignType());
		}
		if(signRequest.getSignRequestParams().getSignType().equals(SignType.nexuSign)) {
			throw new EsupSignatureNexuException("redirect to nexuSign");
		}
		if(signRequest.countSign() == 0) {
			if(!SignRequestParams.NewPageType.none.equals(signRequest.getSignRequestParams().getNewPageType())) {
				signRequest.getSignRequestParams().setXPos(xFirstPos);
				signRequest.getSignRequestParams().setYPos(yFirstPos);
			}
		} else {
			signRequest.getSignRequestParams().setNewPageType(NewPageType.none);
		}
		
		SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
		File toSignFile = null;
		if(signRequest.getSignedDocuments().size() > 0) {
			toSignFile = getLastSignedDocument(signRequest).getJavaIoFile();
		}else {
			if(signRequest.getOriginalDocuments().size() == 1) {
				toSignFile = signRequest.getOriginalDocuments().get(0).getJavaIoFile();
			}
		}
		File signedFile = null;
		if(toSignFile != null) {
			if (fileService.getContentType(toSignFile).equals("application/pdf")) {
				if (signType.equals(SignRequestParams.SignType.pdfImageStamp) || signType.equals(SignType.visa)) {
					logger.info(user.getEppn() + " launch add imageStamp for signRequest : " + signRequest.getId());
					signedFile = pdfService.stampImage(toSignFile, signRequest.getSignRequestParams(), user);
				} else if (signType.equals(SignRequestParams.SignType.certSign)) {
					logger.info(user.getEppn() + " launch cades visible signature for signRequest : " + signRequest.getId());
					signedFile = padesSign(signRequest, user, password);
				}
			} else {
				if (signType.equals(SignRequestParams.SignType.pdfImageStamp)) {
					logger.warn("stamp image only work on pdf");
				} else if (signType.equals(SignRequestParams.SignType.certSign)) {
					logger.info(user.getEppn() + " launch xades signature for signRequest : " + signRequest.getId());
					signedFile = xadesSign(signRequest, user, password);
					// mime type application/vnd.etsi.asic-e+zip
					signedFile = fileService.renameFile(signedFile, fileService.getNameOnly(signedFile) + ".ascis");
				}
			}
		} else {
			//TODO multiple sign
			signedFile = xadesMultipleSign(signRequest, user, password);
		}
		if (signedFile != null) {
			addSignedFile(signRequest, signedFile, user);
			applySignBookRules(signRequest, user);
		} else {
			throw new EsupSignatureSignException("enable to sign document");
		}
	}

	public void nexuSign(SignRequest signRequest, User user, SignatureDocumentForm signatureDocumentForm) throws EsupSignatureKeystoreException, EsupSignatureIOException {
		logger.info(user.getEppn() + " launch cades nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument = signingService.signDocument(signatureDocumentForm);
		InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		try {
			File signedFile = fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
			if (signedFile != null) {
				addSignedFile(signRequest, signedFile, user);
				applySignBookRules(signRequest, user);
			}
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
	}

	public File padesSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {
		File signImage = user.getSignImage().getJavaIoFile();

		File keyStoreFile = user.getKeystore().getJavaIoFile();
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);
		File toSignFile = getToSignDocument(signRequest).getJavaIoFile();
		
		File toSignFormatedFile = pdfService.formatPdf(toSignFile, signRequest.getSignRequestParams());
		
		SignatureDocumentForm signatureDocumentForm = signingService.getPadesSignatureDocumentForm();
		signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(toSignFormatedFile, "application/pdf"));

		signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for (CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);

		SignatureImageParameters imageParameters = new SignatureImageParameters();

		FileDocument fileDocumentImage = new FileDocument(signImage);
		fileDocumentImage.setMimeType(MimeType.PNG);
		imageParameters.setImage(fileDocumentImage);

		imageParameters.setPage(signRequest.getSignRequestParams().getSignPageNumber());
		imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFormatedFile);
		if (pdfParameters.getRotation() == 0) {
			imageParameters.setWidth(100);
			imageParameters.setHeight(75);
			imageParameters.setxAxis(signRequest.getSignRequestParams().getXPos());
			imageParameters.setyAxis(signRequest.getSignRequestParams().getYPos());
		} else {
			imageParameters.setWidth(75);
			imageParameters.setHeight(100);
			imageParameters.setxAxis(signRequest.getSignRequestParams().getXPos() - 50);
			imageParameters.setyAxis(signRequest.getSignRequestParams().getYPos());
		}

		PAdESSignatureParameters parameters = new PAdESSignatureParameters();
		parameters.setSigningCertificate(certificateToken);
		parameters.setCertificateChain(certificateTokenChain);
		parameters.setSignatureImageParameters(imageParameters);
		// TODO ajuster signature size
		parameters.setSignatureSize(100000);

		DSSDocument dssDocument = signingService.certSignDocument(signatureDocumentForm, parameters, signatureTokenConnection);
		InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		try {
			return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
		return null;
	}

	public File xadesSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {
		File toSignFile = getLastSignedDocument(signRequest).getJavaIoFile();

		SignatureDocumentForm signatureDocumentForm = signingService.getXadesSignatureDocumentForm();
		signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(toSignFile, fileService.getContentType(toSignFile)));

		File keyStoreFile = user.getKeystore().getJavaIoFile();
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);

		signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for (CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);

		ASiCWithXAdESSignatureParameters parameters = new ASiCWithXAdESSignatureParameters();
		parameters.setSigningCertificate(certificateToken);
		parameters.setCertificateChain(certificateTokenChain);
		DSSDocument dssDocument = signingService.certSignDocument(signatureDocumentForm, parameters, signatureTokenConnection);
		InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		try {
			return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
		return null;
	}

	public File xadesMultipleSign(SignRequest signRequest, User user, String password) throws EsupSignatureKeystoreException {

		SignatureMultipleDocumentsForm signatureDocumentForm = signingService.getCadesSignatureMultipleDocumentsForm();
		signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
		
		List<MultipartFile> multipartFiles = new ArrayList<>();
		for(Document document : signRequest.getOriginalDocuments()) {
			File toSignFile = document.getJavaIoFile();
			multipartFiles.add(fileService.toMultipartFile(toSignFile, fileService.getContentType(toSignFile)));
		}
		signatureDocumentForm.setDocumentsToSign(multipartFiles);
		
		File keyStoreFile = user.getKeystore().getJavaIoFile();
		
		SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(keyStoreFile, password);
		CertificateToken certificateToken = userKeystoreService.getCertificateToken(keyStoreFile, password);
		CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(keyStoreFile, password);

		signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
		List<String> base64CertificateChain = new ArrayList<>();
		for (CertificateToken token : certificateTokenChain) {
			base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
		}
		signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);

		ASiCWithCAdESSignatureParameters parameters = new ASiCWithCAdESSignatureParameters();
		parameters.setSigningCertificate(certificateToken);
		parameters.setCertificateChain(certificateTokenChain);
		parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
		DSSDocument dssDocument = signingService.certSignDocument(signatureDocumentForm, parameters, signatureTokenConnection);
		InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		try {
			return fileService.inputStreamToFile(signedPdfDocument.openStream(), signedPdfDocument.getName());
		} catch (IOException e) {
			logger.error("error to read signed file", e);
		}
		return null;
	}
	
	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			Document document = documentService.createDocument(signedFile, "signed_" + signRequest.getSignRequestParams().getSignType().toString() + "_" + user.getEppn() + "_" + signedFile.getName(), fileService.getContentType(signedFile));
			signRequest.getSignedDocuments().add(document);
			document.setSignRequestId(signRequest.getId());
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}

	public void applySignBookRules(SignRequest signRequest, User user) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
		signRequest.getSignBooks().put(signBook.getId(), true);
		if (isSignRequestCompleted(signRequest)) {
			if(signBook.getSignBookType().equals(SignBookType.user)) {
				signBookService.resetSignBookParams(signBook);
			}
			if(signType.equals(SignType.visa)) {
				updateInfo(signRequest, SignRequestStatus.checked, "visa", user, "SUCCESS");
			} else {
				updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS");
			}
			if (!signBook.getTargetType().equals(DocumentIOType.none)) {
				try {
					signBookService.exportFileToTarget(signBook, signRequest, user);
					updateInfo(signRequest, SignRequestStatus.exported, "export to target " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS");
				} catch (EsupSignatureException e) {
					logger.error("error on export file to fs", e);
				}
			}
			if(signBook.isAutoRemove()) {
				signBookService.removeSignRequestFromSignBook(signRequest, signBook, user);
				updateInfo(signRequest, SignRequestStatus.completed, "auto remove", user, "SUCCESS");
			}
		} else {
			updateInfo(signRequest, SignRequestStatus.pending, "sign", user, "SUCCESS");
		}
	}
	
	public Document getToSignDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getSignedDocuments();
		if(documents.size() >0 ) {
			return documents.get(documents.size() - 1);
		} else {
			return getLastOriginalsDocument(signRequest);
		}
	}
	
	public Document getLastSignedDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getSignedDocuments();
		return documents.get(documents.size() - 1);
	}

	public Document getLastOriginalsDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getOriginalDocuments();
		if (documents.size() > 1) {
			return null;
		} else {
			return documents.get(0);
		}
	}

	public void updateInfo(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setIp(user.getIp());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setFinalStatus(signRequestStatus.toString());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.persist();
		signRequest.setStatus(signRequestStatus);
		//signRequest.merge();
	}

	public boolean isSignRequestCompleted(SignRequest signRequest) {
		if (signRequest.getSignBooks() != null) {
			for (Map.Entry<Long, Boolean> signBookId : signRequest.getSignBooks().entrySet()) {
				if (!signRequest.getSignBooks().get(signBookId.getKey()) && signRequest.isAllSignToComplete()) {
					return false;
				}
				if (signRequest.getSignBooks().get(signBookId.getKey()) && !signRequest.isAllSignToComplete()) {
					return true;
				}
			}
			if (signRequest.isAllSignToComplete()) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void refuse(SignRequest signRequest, User user) {
		signBookService.removeSignRequestFromAllSignBooks(signRequest);
		updateInfo(signRequest, SignRequestStatus.refused, "refuse", user, "SUCCESS");
	}
	
	public void toggleNeedAllSign(SignRequest signRequest) {
		if(signRequest.isAllSignToComplete()) {
			signRequest.setAllSignToComplete(false);
		} else {
			signRequest.setAllSignToComplete(true);
		}
		signRequest.merge();
	}
	
	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft)) 
				&& signBook != null
				&& !signRequest.getSignBooks().get(signBook.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		List<Log> log = Log.findLogsByEppnAndSignRequestIdEquals(user.getEppn(), signRequest.getId()).getResultList();
		if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setXPos(0);
		signRequestParams.setYPos(0);
		signRequestParams.setNewPageType(NewPageType.none);
		signRequestParams.setSignType(SignType.visa);
		signRequestParams.persist();
		return signRequestParams;
	}

	public List<SignBook> getSignBooksList(SignRequest signRequest) {
		List<SignBook> signBooks = new ArrayList<>();
		Set<Long> signBookIds = signRequest.getSignBooks().keySet();
		for(long signBookId : signBookIds) {
			signBooks.add(SignBook.findSignBook(signBookId));
		}
		return signBooks;
	}
	
	public long generateUniqueId() {
        long val = -1;
        while (val < 0) {
        	final UUID uid = UUID.randomUUID();
            final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
            buffer.putLong(uid.getLeastSignificantBits());
            buffer.putLong(uid.getMostSignificantBits());
            final BigInteger bi = new BigInteger(buffer.array());
            val = bi.longValue();
        } 
        return val;
    }

}

