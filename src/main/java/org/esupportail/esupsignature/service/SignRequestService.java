package org.esupportail.esupsignature.service;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.asic.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateToken;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.esupportail.esupsignature.dss.web.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.web.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.web.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureIOException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureSignException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessFactory;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.esupportail.esupsignature.service.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private WorkflowStepRepository workflowStepRepository;

	@Resource
	private LogRepository logRepository;

	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private PdfService pdfService;

	@Resource
	private DocumentService documentService;

	@Resource
	private SignRequestParamsRepository signRequestParamsRepository; 
	
	@Resource
	private SignBookRepository signBookRepository;
	
	@Resource
	private FsAccessFactory fsAccessFactory;
	
	@Resource
	private SignBookService signBookService;
	
	@Resource
	private SignService signService;

	@Resource
	private FileService fileService;

	@Resource
	private UserRepository userRepository;

	@Resource
	private UserService userService;

	@Resource
	private MailService mailService;

	private String step = "";

	public List<SignRequest> getTosignRequests(User user) {
		List<SignRequest> signRequestsToSign = new ArrayList<>();
		List<WorkflowStep> workflowSteps = workflowStepRepository.findByRecipients(user.getId());
		for(WorkflowStep workflowStep : workflowSteps) {
			if(!workflowStep.getRecipients().get(user.getId()) && signBookRepository.findByWorkflowSteps(Arrays.asList(workflowStep)).size() > 0) {
				SignBook signBook = signBookRepository.findByWorkflowSteps(Arrays.asList(workflowStep)).get(0);
				if (signBook.getCurrentWorkflowStep().equals(workflowStep) && signBook.getStatus().equals(SignRequestStatus.pending)) {
					signRequestsToSign.addAll(signBook.getSignRequests());
				}
			}
		}
		return signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
	}

	public SignRequest createSignRequest(String title, User user) throws EsupSignatureException {
		SignRequest signRequest = new SignRequest();
		signRequest.setTitle(title);
		createSignRequest(signRequest, user);
		SignBook signBook = signBookService.createSignBook(title, SignBook.SignBookType.workflow, user, false);
		signBook.getSignRequests().add(signRequest);
		signBookRepository.save(signBook);
		signRequest.setParentSignBook(signBook);
		signRequestRepository.save(signRequest);
		return signRequest;
	}

	public SignRequest createSignRequest(String title, SignBook signBook,  User user, List<Document> documents) {
		SignRequest signRequest = new SignRequest();
		signRequest.setTitle(title);
		createSignRequest(signRequest, user, documents);
		signBook.getSignRequests().add(signRequest);
		signBookRepository.save(signBook);
		signRequest.setParentSignBook(signBook);
		return signRequest;
	}

	public SignRequest createSignRequest(SignRequest signRequest, User user) {
			return createSignRequest(signRequest, user, new ArrayList<>());
	}


	public SignRequest createSignRequest(SignRequest signRequest, User user, Document document) {
		List<Document> documents = new ArrayList<Document>();
		documents.add(document);
		return createSignRequest(signRequest, user, documents);
	}
	
	public SignRequest createSignRequest(SignRequest signRequest, User user, List<Document> documents) {
		signRequest.setToken(String.valueOf(generateUniqueId()));
		signRequest.setCreateBy(user.getEppn());
		signRequest.setCreateDate(new Date());
		signRequest.setStatus(SignRequestStatus.draft);
		signRequest.setOriginalDocuments(documents);
		signRequestRepository.save(signRequest);
		updateStatus(signRequest, SignRequestStatus.draft, "Création de la demande", user, "SUCCESS", "");
		for(Document document : documents) {
			document.setParentId(signRequest.getId());
		}
		return signRequest;
	}
	
	public void addOriginalDocuments(SignRequest signRequest, List<Document> documents) {
		for(Document document : documents) {
			signRequest.getOriginalDocuments().add(document);
			document.setParentId(signRequest.getId());
		}
	}

	public void sign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureException, IOException {
		step = "Demarrage de la signature";
		Document signedFile;
		List<Document> toSignDocuments = getToSignDocuments(signRequest);
		SignType signType = signRequest.getParentSignBook().getCurrentWorkflowStep().getSignType();
		if (signType.equals(SignType.visa)) {
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, user, addDate);
			} else {
				signedFile = toSignDocuments.get(0);
			}
		} else if(signType.equals(SignType.pdfImageStamp)) {
			signedFile = pdfService.stampImage(toSignDocuments.get(0), signRequest, user, addDate);
		} else {
			signedFile = certSign(signRequest, user, password, addDate, visual);
		}
		
		if (signedFile != null) {
			signRequest.getSignedDocuments().add(signedFile);
			signedFile.setParentId(signRequest.getId());
			if(signType.equals(SignType.visa)) {
				updateStatus(signRequest, SignRequestStatus.checked, "Visa", user, "SUCCESS", signRequest.getComment());
			} else {
				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
			}
			signRequestRepository.save(signRequest);
			step = "end";
			applyEndOfStepRules(signRequest, user);
		}
	}

	public void nexuSign(SignRequest signRequest, User user, AbstractSignatureForm signatureDocumentForm, AbstractSignatureParameters parameters) throws EsupSignatureKeystoreException, EsupSignatureIOException, EsupSignatureSignException {
		logger.info(user.getEppn() + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;
		
		if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
			dssDocument = signService.signDocument((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			dssDocument = signService.nexuSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters);
		}
		
		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		addSignedFile(signRequest, signedDocument.openStream(), signedDocument.getName(), signedDocument.getMimeType().getMimeTypeString(), user);
		updateStatus(signRequest, SignRequestStatus.pending, "Signature", user, "SUCCESS", signRequest.getComment());
	}

	public Document certSign(SignRequest signRequest, User user, String password, boolean addDate, boolean visual) throws EsupSignatureKeystoreException, IOException {
		SignatureForm signatureForm;
		List<Document> toSignFiles = new ArrayList<>();
		for(Document document : getToSignDocuments(signRequest)) {
			toSignFiles.add(document);
		}
		step = "Préparation de la signature";
		try {
			AbstractSignatureForm signatureDocumentForm = signService.getSignatureDocumentForm(toSignFiles, signRequest, visual);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
			
			SignatureTokenConnection signatureTokenConnection = userKeystoreService.getSignatureTokenConnection(user.getKeystore().getInputStream(), password);
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(user.getKeystore().getInputStream(), password);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(user.getKeystore().getInputStream(), password);
	
			signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
			List<String> base64CertificateChain = new ArrayList<>();
			for (CertificateToken token : certificateTokenChain) {
				base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
			}
			signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);
			
			AbstractSignatureParameters parameters = null;
			if(signatureForm.equals(SignatureForm.CAdES)) {
				ASiCWithCAdESSignatureParameters aSiCWithCAdESSignatureParameters = new ASiCWithCAdESSignatureParameters();
				aSiCWithCAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithCAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.XAdES)) {
				ASiCWithXAdESSignatureParameters aSiCWithXAdESSignatureParameters = new ASiCWithXAdESSignatureParameters();
				aSiCWithXAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				parameters = aSiCWithXAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.PAdES)) {
				//TODO fix problème png (premier upload ?)
				step = "Formatage du PDF";
				InputStream toSignInputStream;
				Document toSignFile = toSignFiles.get(0);
				toSignInputStream = toSignFile.getInputStream();
				if(signRequest.getParentSignBook().getCurrentWorkflowStepNumber() == 1) {
					toSignInputStream = pdfService.convertGS(pdfService.writeMetadatas(toSignFile.getInputStream(), toSignFile.getFileName(), signRequest));
				}
				MultipartFile multipartFile = fileService.toMultipartFile(toSignInputStream, toSignFile.getFileName(), "application/pdf");
				parameters = signService.fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams().get(signRequest.getParentSignBook().getCurrentWorkflowStepNumber()), multipartFile, user, addDate);
				SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
				documentForm.setDocumentToSign(multipartFile);
				signatureDocumentForm = documentForm;
			}
			step = "Signature du/des documents(s)";

			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
			DSSDocument dssDocument;
			if(toSignFiles.size() > 1) {
				dssDocument = signService.certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, signatureTokenConnection);
			} else {
				dssDocument = signService.certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, signatureTokenConnection);
			}
			InMemoryDocument signedPdfDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
			step = "Enregistrement du/des documents(s)";
			return documentService.createDocument(signedPdfDocument.openStream(), signedPdfDocument.getName(), signedPdfDocument.getMimeType().getMimeTypeString());
		} catch (EsupSignatureKeystoreException e) {
			step = "security_bad_password";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			step = "sign_system_error";
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		}
	}
	
	public void addSignedFile(SignRequest signRequest, InputStream signedInputStream, String fileName, String mimeType, User user) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Document document = documentService.createDocument(signedInputStream, signRequest.getTitle() + "_" + signRequest.getParentSignBook().getCurrentWorkflowStep().getSignType() + "_" + user.getEppn() + "_" + simpleDateFormat.format(new Date()) + "." + fileService.getExtension(fileName), mimeType);
		signRequest.getSignedDocuments().add(document);
		document.setParentId(signRequest.getId());
	}


	public void applyEndOfStepRules(SignRequest signRequest, User user) {
		signRequest.getParentSignBook().getCurrentWorkflowStep().getRecipients().put(user.getId(), true);
		if(signBookService.isStepDone(signRequest.getParentSignBook())) {
			signBookService.nextWorkFlowStep(signRequest.getParentSignBook(), user);
			if (signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.completed)) {
				mailService.sendCompletedMail(signRequest.getParentSignBook());
			} else {
				for(SignRequest childSignRequest : signRequest.getParentSignBook().getSignRequests()) {
					updateStatus(childSignRequest, SignRequestStatus.pending, "Passage à l'étape " + signRequest.getParentSignBook().getCurrentWorkflowStepNumber(), user, "SUCCESS", "");
				}
			}
		}
	}

//
//	public void applyEndOfStepRules(SignRequest signRequest, User user) throws EsupSignatureException {
//		//SignBook recipientSignBook = signBookService.getSignBookBySignRequestAndUser(signRequest, user);
//		SignType signType = signRequest.getParentSignBook().getCurrentWorkflowStep().getSignType();
//		signRequest.getParentSignBook().getCurrentWorkflowStep().getRecipients().put(user.getId(), true);
//		if (isSignRequestCompleted(signRequest)) {
//			if(signType.equals(SignType.visa)) {
//				updateStatus(signRequest, SignRequestStatus.checked, "Visa" , user, "SUCCESS", signRequest.getComment());
//			} else {
//				updateStatus(signRequest, SignRequestStatus.signed, "Signature", user, "SUCCESS", signRequest.getComment());
//			}
//			//signBookService.removeSignRequestFromSignBook(signRequest, recipientSignBook);
//			if(signRequest.getParentSignBook().getCurrentWorkflowStepNumber() == signRequest.getParentSignBook().getWorkflowSteps().size()) {
//				completeSignRequest(signRequest, user);
//				setWorkflowsLabels(signRequest.getParentSignBook().getWorkflowSteps());
//				mailService.sendCompletedMail(signRequest);
//			} else {
//				nextWorkFlowStep(signRequest.getParentSignBook(), user);
//				updateStatus(signRequest, SignRequestStatus.pending, "Passage à l'étape suivante", user, "SUCCESS", signRequest.getComment());
//			}
//		} else {
//			if(signType.equals(SignType.visa)) {
//				updateStatus(signRequest, SignRequestStatus.pending, "Visa", user, "SUCCESS", signRequest.getComment());
//			} else {
//				updateStatus(signRequest, SignRequestStatus.pending, "Signature", user, "SUCCESS", signRequest.getComment());
//			}
//		}
//		//signRequest.setNbSign(signRequest.getNbSign() + 1);
//	}



	public void pendingSignRequest(SignRequest signRequest, User user) {
		if(!signRequest.getStatus().equals(SignRequestStatus.pending)) {
			updateStatus(signRequest, SignRequestStatus.pending, "Envoyé pour signature", user, "SUCCESS", signRequest.getComment());
			for (Long recipientId : signRequest.getParentSignBook().getCurrentWorkflowStep().getRecipients().keySet()) {
				User recipientUser = userRepository.findById(recipientId).get();
				if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
					userService.sendEmailAlert(recipientUser);
				}
			}
		} else {
			logger.warn("allready pending");
		}
	}

	public int scanSignatureFields(SignRequest signRequest, PDDocument pdDocument) {
		List<SignRequestParams> signRequestParamsList = pdfService.pdSignatureFieldsToSignRequestParams(pdDocument);
		for(SignRequestParams signRequestParams : signRequestParamsList) {
			signRequestParamsRepository.save(signRequestParams);
		}
		signRequest.getSignRequestParams().addAll(signRequestParamsList);
		return signRequestParamsList.size();
	}

	public void completeSignRequest(SignRequest signRequest, User user) {
		updateStatus(signRequest, SignRequestStatus.completed, "Terminé automatiquement", user, "SUCCESS", signRequest.getComment());
	}

	public List<Document> getToSignDocuments(SignRequest signRequest) {
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate).reversed()).collect(Collectors.toList()));
			documents.add(signRequest.getSignedDocuments().get(0));
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}
	
	public FsFile getLastSignedFile(SignRequest signRequest) throws Exception {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			FsAccessService fsAccessService = null;
			if (signRequest.getExportedDocumentURI().startsWith("smb")) {
				fsAccessService = fsAccessFactory.getFsAccessService(DocumentIOType.smb);
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getLastSignedDocument(signRequest);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}
	
	public Document getLastSignedDocument(SignRequest signRequest) {
		if(signRequest.getSignedDocuments().size() > 0) {
			signRequest.setSignedDocuments(signRequest.getSignedDocuments().stream().sorted(Comparator.comparing(Document::getCreateDate).reversed()).collect(Collectors.toList()));
			return signRequest.getSignedDocuments().get(0);
		} else {
			return getLastOriginalDocument(signRequest);
		}
	}

	public Document getLastOriginalDocument(SignRequest signRequest) {
		List<Document> documents = signRequest.getOriginalDocuments();
		if (documents.size() > 1) {
			return null;
		} else {
			return documents.get(0);
		}
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment) {
		updateStatus(signRequest, signRequestStatus, action, user, returnCode, comment, null, null, null);
	}

	public void updateStatus(SignRequest signRequest, SignRequestStatus signRequestStatus, String action, User user, String returnCode, String comment, Integer pageNumber, Integer posX, Integer posY ) {
		Log log = new Log();
		log.setSignRequestId(signRequest.getId());
		log.setEppn(user.getEppn());
		log.setIp(user.getIp());
		log.setInitialStatus(signRequest.getStatus().toString());
		log.setLogDate(new Date());
		log.setAction(action);
		log.setReturnCode(returnCode);
		log.setComment(comment);
		if(pageNumber != null) {
			log.setPageNumber(pageNumber);
			log.setPosX(posX);
			log.setPosY(posY);
		}
		if(signRequestStatus != null) {
			log.setFinalStatus(signRequestStatus.toString());		
			signRequest.setStatus(signRequestStatus);
		} else {
			log.setFinalStatus(signRequest.getStatus().toString());
		}
		logRepository.save(log);
	}

	public void refuse(SignRequest signRequest, User user) {
		updateStatus(signRequest, SignRequestStatus.refused, "Refusé", user, "SUCCESS", signRequest.getComment());
	}

	public boolean checkUserSignRights(User user, SignRequest signRequest) {
		if ((signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
				&& signRequest.getParentSignBook().getCurrentWorkflowStep().getRecipients().containsKey(user.getId())) {
			return true;
		} else {
			return false;
		}
	}

	public boolean checkUserViewRights(User user, SignRequest signRequest) {
		if(signRequest != null) {
			List<Log> log = logRepository.findByEppnAndSignRequestId(user.getEppn(), signRequest.getId());
			if (signRequest.getCreateBy().equals(user.getEppn()) || log.size() > 0 || signBookService.isUserInWorkflow(signRequest.getParentSignBook(), user)) {
				return true;
			}
		}
		return false;
	}
	
	public SignRequestParams getEmptySignRequestParams() {
		SignRequestParams signRequestParams = new SignRequestParams();
		signRequestParams.setSignPageNumber(1);
		signRequestParams.setXPos(0);
		signRequestParams.setYPos(0);
		signRequestParamsRepository.save(signRequestParams);
		return signRequestParams;
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

	public void delete(SignRequest signRequest) {
		List<Log> logs = logRepository.findBySignRequestId(signRequest.getId());
		for (Log log : logs) {
			logRepository.delete(log);
		}
		for(Document document : signRequest.getOriginalDocuments()) {
			documentService.deleteDocument(document);
		}
		signRequest.getOriginalDocuments().clear();
		signRequestRepository.delete(signRequest);
	}

	public String getStep() {
		return step;
	}
	
}