package org.esupportail.esupsignature.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignRequest;
import org.esupportail.esupsignature.domain.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.service.fs.cifs.CifsAccessImpl;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.DSSUtils;
import eu.europa.esig.dss.EncryptionAlgorithm;
import eu.europa.esig.dss.FileDocument;
import eu.europa.esig.dss.InMemoryDocument;
import eu.europa.esig.dss.MimeType;
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

	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document, SignRequestParams signRequestParams, long[] signBookIds) {
		document.setCreateDate(new Date());
		signRequest.setName(document.getFileName());
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.getDocuments().add(document);
		signRequest.setStatus(SignRequestStatus.uploaded);
		signRequest.setSignRequestParams(signRequestParams);
		for(long signBookId : signBookIds) {
			SignBook signBook = SignBook.findSignBook(signBookId);
			signRequest.getSignBooks().put(signBook.getId(), false);
			signBook.getSignRequests().add(signRequest);
		}
		signRequest.persist();
		updateInfo(signRequest, SignRequestStatus.pending, "create", user, "SUCCESS");
		return signRequest;
	}
	
	public void validate(SignRequest signRequest, User user) throws EsupSignatureIOException, EsupSignatureException {
		updateInfo(signRequest, SignRequestStatus.checked, "validate", user, "SUCCESS");		
		applySignBookRules(signRequest, user);
	}

	public void sign(SignRequest signRequest, User user, String password) throws EsupSignatureIOException, EsupSignatureException {
		File toSignFile = getLastDocument(signRequest).getJavaIoFile();
		//TODO getparams from signBook si non surchargés
		SignRequestParams.SignType signType = signRequest.getSignRequestParams().getSignType();
		File signedFile = null;
		if (fileService.getContentType(toSignFile).equals("application/pdf")) {
			if (signType.equals(SignRequestParams.SignType.pdfImageStamp)) {
				logger.info(user.getEppn() + " launch add imageStamp for signRequest : " + signRequest.getId());
				signedFile = pdfService.stampImage(toSignFile, signRequest.getSignRequestParams(), user);
			} else if (signType.equals(SignRequestParams.SignType.certSign)) {
				logger.info(user.getEppn() + " launch cades visible signature for signRequest : " + signRequest.getId());
				signedFile = padesSign(signRequest, user, password);
			}
		} else {
			if (signType.equals(SignRequestParams.SignType.pdfImageStamp)) {
				logger.warn("only pdf can get visible sign");
			} else if (signType.equals(SignRequestParams.SignType.certSign)) {
				logger.info(user.getEppn() + " launch xades signature for signRequest : " + signRequest.getId());
				signedFile = xadesSign(signRequest, user, password);
				// mime type application/vnd.etsi.asic-e+zip
				signedFile = fileService.renameFile(signedFile, fileService.getNameOnly(signedFile) + ".ascis");
			}
		}
		if (signedFile != null) {
			addSignedFile(signRequest, signedFile, user);
			applySignBookRules(signRequest, user);
		} else {
			throw new EsupSignatureException("enable to sign document");
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
		File toSignFile = getLastDocument(signRequest).getJavaIoFile();
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
		File toSignFile = getLastDocument(signRequest).getJavaIoFile();

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

	public void addSignedFile(SignRequest signRequest, File signedFile, User user) throws EsupSignatureIOException {
		try {
			Document toaddDocument = documentService.addFile(signedFile, "signed_" + signRequest.getSignRequestParams().getSignType().toString() + "_" + user.getEppn() + "_" + signedFile.getName(), fileService.getContentType(signedFile));
			signRequest.getDocuments().add(toaddDocument);
			signRequest.merge();
		} catch (IOException e) {
			throw new EsupSignatureIOException("error on save signed file", e);
		}
	}

	public void applySignBookRules(SignRequest signRequest, User user) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		signRequest.getSignBooks().put(signBook.getId(), true);
		signRequest.merge();
		if (isSignRequestCompleted(signRequest)) {
			updateInfo(signRequest, SignRequestStatus.signed, "sign", user, "SUCCESS");
			if (signBook != null) {
				if (!signBook.getTargetType().equals(DocumentIOType.none)) {
					try {
						//TODO a retester
						signBookService.exportFileToTarget(signBook, signRequest, user);
						updateInfo(signRequest, SignRequestStatus.exported, "export to target " + signBook.getTargetType() + " : " + signBook.getDocumentsTargetUri(), user, "SUCCESS");
						signBookService.removeSignRequestFromAllSignBooks(signRequest, signBook, user);
					} catch (EsupSignatureException e) {
						logger.error("error on export file to fs", e);
					}
				} else {
					signBookService.removeSignRequestFromAllSignBooks(signRequest, signBook, user);
				}
			}
		} else {
			updateInfo(signRequest, SignRequestStatus.pending, "sign", user, "SUCCESS");
		}
	}
	
	public Document getLastDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getDocuments();
		return documents.get(documents.size() - 1);
	}

	public Document getPreviousDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getDocuments();
		if (documents.size() > 1) {
			return documents.get(documents.size() - 2);
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
		signRequest.merge();
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
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		signRequest.getSignBooks().put(signBook.getId(), true);
		signBookService.removeSignRequestFromAllSignBooks(signRequest, signBook, user);
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
		if (signRequest.getStatus().equals(SignRequestStatus.pending) 
				&& signBook != null
				&& !signRequest.getSignBooks().get(signBook.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		SignBook signBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
		List<Log> log = Log.findLogsBySignRequestIdEquals(signRequest.getId()).getResultList();
		if (signRequest.getCreateBy().equals(user.getEppn()) || signBook != null || log.size() > 0) {
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
		signRequestParams.setSignType(SignType.validate);
		signRequestParams.persist();
		return signRequestParams;
	}

}

