package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.enumerations.ValidationDataEncapsulationStrategy;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.signature.AbstractSignatureParameters;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.NexuSignatureRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

@Service
public class NexuService {

	private static final Logger logger = LoggerFactory.getLogger(NexuService.class);

	private final SignService signService;
	private final SignRequestService signRequestService;
	private final DocumentService documentService;
	private final FileService fileService;
	private final UserService userService;
	private final SignRequestRepository signRequestRepository;
	private final NexuSignatureRepository nexuSignatureRepository;
	private final DssUtilsService dssUtilsService;
	private final AuditTrailService auditTrailService;
	private final ValidationService validationService;

	public NexuService(SignService signService, SignRequestService signRequestService, DocumentService documentService, FileService fileService, UserService userService, SignRequestRepository signRequestRepository, NexuSignatureRepository nexuSignatureRepository, DssUtilsService dssUtilsService, AuditTrailService auditTrailService, ValidationService validationService) {
        this.signService = signService;
        this.signRequestService = signRequestService;
        this.documentService = documentService;
        this.fileService = fileService;
        this.userService = userService;
        this.signRequestRepository = signRequestRepository;
        this.nexuSignatureRepository = nexuSignatureRepository;
        this.dssUtilsService = dssUtilsService;
        this.auditTrailService = auditTrailService;
        this.validationService = validationService;
	}

    public AbstractSignatureParameters<?> getParameters(SignatureMultipleDocumentsForm signatureMultipleDocumentsForm, List<DSSDocument> documentsToSign) throws IOException {
		AbstractSignatureParameters<?> parameters = signService.getASiCSignatureParameters(signatureMultipleDocumentsForm.getContainerType(), signatureMultipleDocumentsForm.getSignatureForm());
		parameters.getDetachedContents().addAll(documentsToSign);
		signService.fillCommonsParameters(parameters, signatureMultipleDocumentsForm);
		return parameters;
	}

	public AbstractSignatureParameters<?> getParameters(SignatureDocumentForm signatureDocumentForm) {
		AbstractSignatureParameters<?> parameters = getSignatureParameters(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		parameters.setSignaturePackaging(signatureDocumentForm.getSignaturePackaging());
		signService.fillCommonsParameters(parameters, signatureDocumentForm);
		return parameters;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public ToBeSigned getDataToSign(Long id, String userEppn, SignatureDocumentForm signatureDocumentForm) throws DSSException, IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = signService.getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		DSSDocument toSignDocument = dssUtilsService.toDSSDocument(signatureDocumentForm.getDocumentToSign());
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, signatureDocumentForm);
		ToBeSigned toBeSigned = service.getDataToSign(toSignDocument, parameters);
		logger.info("End getDataToSign with one document");
		return toBeSigned;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ToBeSigned getDataToSign(SignatureMultipleDocumentsForm form, List<DSSDocument> documentsToSign) throws DSSException, IOException {
		logger.info("Start getDataToSign with multiple documents");
		MultipleDocumentsSignatureService service = signService.getASiCSignatureService(form.getSignatureForm());
		AbstractSignatureParameters parameters = getParameters(form, documentsToSign);
		ToBeSigned toBeSigned = null;
		try {
			List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(form.getDocumentsToSign());
			toBeSigned = service.getDataToSign(toSignDocuments, parameters);
		} catch (Exception e) {
			logger.error("Unable to execute getDataToSign : " + e.getMessage(), e);
		}
		logger.info("End getDataToSign with multiple documents");
		return toBeSigned;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public DSSDocument signDocument(Long id, String userEppn, SignatureDocumentForm signatureDocumentForm) throws IOException, EsupSignatureException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("Start signDocument with one document");
		DocumentSignatureService service = signService.getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		DSSDocument toSignDssDocument = dssUtilsService.toDSSDocument(signatureDocumentForm.getDocumentToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(signatureDocumentForm.getEncryptionAlgorithm(), signatureDocumentForm.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, signatureDocumentForm.getSignatureValue());
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, signatureDocumentForm);
		validationService.checkRevocation(signatureDocumentForm, DSSUtils.loadCertificate(signatureDocumentForm.getCertificate()), parameters);
		try {
			logger.info("End signDocument with one document");
			return service.signDocument(toSignDssDocument, parameters, signatureValue);
		} catch (Exception e) {
			logger.warn(e.getMessage());
			throw new EsupSignatureException(e.getMessage());
		}
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument signMultipleDocuments(SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) throws IOException {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = signService.getASiCSignatureService(signatureMultipleDocumentsForm.getSignatureForm());
		List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(signatureMultipleDocumentsForm.getDocumentsToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(signatureMultipleDocumentsForm.getEncryptionAlgorithm(), signatureMultipleDocumentsForm.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, signatureMultipleDocumentsForm.getSignatureValue());
		AbstractSignatureParameters parameters = getParameters(signatureMultipleDocumentsForm, toSignDocuments);
		DSSDocument signedDocument = service.signDocument(toSignDocuments, parameters, signatureValue);
		logger.info("End signDocument with multiple documents");
		return signedDocument;
	}

	private AbstractSignatureParameters<?> getSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters<?> parameters = null;
		if (containerType != null) {
			parameters = signService.getASiCSignatureParameters(containerType, signatureForm);
		} else {
			switch (signatureForm) {
				case CAdES:
					parameters = new CAdESSignatureParameters();
					break;
				case PAdES:
					PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
					//signature size 32767 is max for PDF/A-2B
					padesParams.setContentSize(32767);
					parameters = padesParams;
					break;
				case XAdES:
					parameters = new XAdESSignatureParameters();
					break;
				default:
					logger.error("Unknow signature form : " + signatureForm);
			}
		}
		return parameters;
	}

	@Transactional
	public NexuSignature saveNexuSignature(Long id, SignatureDocumentForm abstractSignatureForm, String userEppn) throws IOException {
		User user = userService.getByEppn(userEppn);
		NexuSignature nexuSignature = new NexuSignature();
		nexuSignature.setSignRequest(signRequestRepository.findById(id).orElseThrow());
		nexuSignature.setSignatureValue(abstractSignatureForm.getSignatureValue());
		nexuSignature.setSigningDate(abstractSignatureForm.getSigningDate());
		nexuSignature.setCertificate(abstractSignatureForm.getCertificate());
		nexuSignature.setCertificateChain(abstractSignatureForm.getCertificateChain());
		nexuSignature.setSignatureForm(abstractSignatureForm.getSignatureForm());
		nexuSignature.setDigestAlgorithm(abstractSignatureForm.getDigestAlgorithm());
		nexuSignature.setEncryptionAlgorithm(abstractSignatureForm.getEncryptionAlgorithm());
		nexuSignature.setSignatureLevel(abstractSignatureForm.getSignatureLevel());
		//nexuSignature.setSignWithExpiredCertificate(abstractSignatureForm.isSignWithExpiredCertificate());
		nexuSignature.getDocumentToSign().add(documentService.createDocument(abstractSignatureForm.getDocumentToSign().getInputStream(), user, abstractSignatureForm.getDocumentToSign().getOriginalFilename(), abstractSignatureForm.getDocumentToSign().getContentType()));
		nexuSignatureRepository.save(nexuSignature);
		return nexuSignature;
	}

	@Transactional
	public NexuSignature getNexuSignature(Long id) {
		return nexuSignatureRepository.findBySignRequestId(id).get(0);
	}

	@Transactional
	public AbstractSignatureForm getAbstractSignatureFormFromNexuSignature(NexuSignature nexuSignature) {
		AbstractSignatureForm abstractSignatureForm;
		if(nexuSignature.getDocumentToSign().size() > 1) {
			abstractSignatureForm = new SignatureMultipleDocumentsForm();
			for(Document document : nexuSignature.getDocumentToSign()) {
				((SignatureMultipleDocumentsForm) abstractSignatureForm).getDocumentsToSign().add(document.getMultipartFile());
			}
		} else {
			abstractSignatureForm = new SignatureDocumentForm();
			((SignatureDocumentForm) abstractSignatureForm).setDocumentToSign(nexuSignature.getDocumentToSign().get(0).getMultipartFile());
		}
		abstractSignatureForm.setSigningDate(nexuSignature.getSigningDate());
		abstractSignatureForm.setSignatureValue(nexuSignature.getSignatureValue());
		abstractSignatureForm.setCertificate(nexuSignature.getCertificate());
		abstractSignatureForm.setCertificateChain(nexuSignature.getCertificateChain());
		abstractSignatureForm.setSignatureForm(nexuSignature.getSignatureForm());
		abstractSignatureForm.setDigestAlgorithm(nexuSignature.getDigestAlgorithm());
		abstractSignatureForm.setEncryptionAlgorithm(nexuSignature.getEncryptionAlgorithm());
		abstractSignatureForm.setSignatureLevel(nexuSignature.getSignatureLevel());
		return abstractSignatureForm;
	}

	@Transactional
	public SignatureDocumentForm getSignatureForm(Long signRequestId, String userEppn, Date date) throws IOException, EsupSignatureRuntimeException {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		List<Document> documentsToSign = signRequestService.getToSignDocuments(signRequest.getId());
		byte[] bytes = documentsToSign.get(0).getInputStream().readAllBytes();
		Reports reports = signRequestService.validate(signRequestId);
		if (reports == null || reports.getDiagnosticData().getAllSignatures().isEmpty()) {
			bytes = signRequestService.stampImagesOnFirstSign(signRequest, signRequest.getSignRequestParams(), userEppn, userEppn, documentsToSign.get(0).getInputStream().readAllBytes(), date, null);
		} else {
			logger.warn("skip add visuals because document already signed");
		}
		documentsToSign.get(0).setTransientInputStream(new ByteArrayInputStream(bytes));
		return signRequestService.getAbstractSignatureForm(documentsToSign, signRequest, false);
	}

	@Transactional
	public AbstractSignatureParameters<?> getSignatureParameters(SignRequest signRequest, String userEppn, AbstractSignatureForm abstractSignatureForm) throws IOException {
		User user = userService.getByEppn(userEppn);
		AbstractSignatureParameters<?> parameters = null;
		if(abstractSignatureForm instanceof SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) {
			List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(signatureMultipleDocumentsForm.getDocumentsToSign());
			parameters = getParameters(signatureMultipleDocumentsForm, toSignDocuments);
		} else if(abstractSignatureForm instanceof SignatureDocumentForm signatureDocumentForm) {
			List<SignRequestParams> signRequestParamsesForSign = signRequest.getSignRequestParams().stream().filter(srp -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().contains(srp) && srp.getSignImageNumber() >= 0 && srp.getTextPart() == null).toList();
			if(abstractSignatureForm.getSignatureForm().equals(SignatureForm.PAdES)) {
				if(!signRequestParamsesForSign.isEmpty()) {
					parameters = signService.fillVisibleParameters(signatureDocumentForm, signRequestParamsesForSign.get(0), user, abstractSignatureForm.getSigningDate());
				} else {
					parameters = signService.fillVisibleParameters(signatureDocumentForm, user);
				}
			} else {
				parameters = getParameters(signatureDocumentForm);
			}
		}
		return parameters;
	}

	@Transactional
	public SignDocumentResponse getSignDocumentResponse(Long signRequestId, SignResponse signResponse, AbstractSignatureForm abstractSignatureForm, String userEppn) throws EsupSignatureRuntimeException, EsupSignatureException {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		if(signRequest.getAuditTrail() == null) {
			signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
		}
		SignDocumentResponse signedDocumentResponse;
		abstractSignatureForm.setSignatureValue(signResponse.getSignatureValue());
		try {
			Document signedFile = nexuSign(signRequest, userEppn, abstractSignatureForm);
			if(signedFile != null) {
				auditTrailService.createSignAuditStep(signRequest, userEppn, signedFile, signRequest.getViewedBy().contains(user));
				auditTrailService.closeAuditTrail(signRequest.getToken(), signedFile, signedFile.getInputStream());
				signedDocumentResponse = new SignDocumentResponse();
				signedDocumentResponse.setUrlToDownload("download");
				return signedDocumentResponse;
			}
		} catch (IOException e) {
			throw new EsupSignatureRuntimeException("unable to sign" , e);
		}
		return null;
	}

	@Transactional
	public Document nexuSign(SignRequest signRequest, String userEppn, AbstractSignatureForm signatureDocumentForm) throws IOException, EsupSignatureException {
		logger.info(userEppn + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;
		if(signatureDocumentForm instanceof SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) {
			dssDocument = signMultipleDocuments(signatureMultipleDocumentsForm);
		} else {
			dssDocument = signDocument(signRequest.getId(), userEppn, (SignatureDocumentForm) signatureDocumentForm);
		}
		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
		return documentService.addSignedFile(signRequest, signedDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(signedDocument.getName()), Files.probeContentType(Path.of(dssDocument.getName())), userService.getByEppn(userEppn));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument extend(ExtensionForm extensionForm) {
		ASiCContainerType containerType = extensionForm.getContainerType();
		SignatureForm signatureForm = extensionForm.getSignatureForm();
		DSSDocument signedDocument = dssUtilsService.toDSSDocument(extensionForm.getSignedFile());
		List<DSSDocument> originalDocuments = dssUtilsService.toDSSDocuments(extensionForm.getOriginalFiles());
		DocumentSignatureService service = signService.getSignatureService(containerType, signatureForm);
		AbstractSignatureParameters parameters = getSignatureParameters(containerType, signatureForm);
		parameters.setSignatureLevel(extensionForm.getSignatureLevel());
		parameters.setValidationDataEncapsulationStrategy(ValidationDataEncapsulationStrategy.CERTIFICATE_REVOCATION_VALUES_AND_TIMESTAMP_VALIDATION_DATA);
		if (Utils.isCollectionNotEmpty(originalDocuments)) {
			parameters.setDetachedContents(originalDocuments);
		}
		DSSDocument extendedDoc = service.extendDocument(signedDocument, parameters);
		logger.info("End extend with one document");
		return extendedDoc;
	}

}
