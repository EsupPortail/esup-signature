package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import jakarta.annotation.Resource;
import org.bouncycastle.asn1.x509.Certificate;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.NexuSignature;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.NexuSignatureRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class NexuService {

	private static final Logger logger = LoggerFactory.getLogger(NexuService.class);

	@Resource
	private SignService signService;

	@Resource
	private DocumentService documentService;

	@Resource
	private FileService fileService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private NexuSignatureRepository nexuSignatureRepository;

	@Resource
	private DssUtilsService dssUtilsService;

	@Resource
	private AuditTrailService auditTrailService;

	public AbstractSignatureParameters<?> getParameters(SignatureMultipleDocumentsForm signatureMultipleDocumentsForm, List<Document> documentsToSign) throws IOException {
		AbstractSignatureParameters<?> parameters = signService.getASiCSignatureParameters(signatureMultipleDocumentsForm.getContainerType(), signatureMultipleDocumentsForm.getSignatureForm());
		List<DssMultipartFile> dssMultipartFiles = new ArrayList<>();
		for(Document document : documentsToSign) {
			dssMultipartFiles.add(new DssMultipartFile(document.getFileName(), document.getFileName(), document.getContentType(), document.getInputStream()));
		}
		parameters.getDetachedContents().addAll(dssUtilsService.toDSSDocuments(dssMultipartFiles));
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
	public ToBeSigned getDataToSign(Long id, String userEppn, SignatureDocumentForm signatureDocumentForm, List<Document> documentsToSign) throws DSSException, IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = signService.getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		DSSDocument toSignDocument = dssUtilsService.toDSSDocument(new DssMultipartFile(documentsToSign.get(0).getFileName(), documentsToSign.get(0).getFileName(), documentsToSign.get(0).getContentType(), documentsToSign.get(0).getInputStream()));
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, signatureDocumentForm, documentsToSign);
		ToBeSigned  toBeSigned = service.getDataToSign(toSignDocument, parameters);
		logger.info("End getDataToSign with one document");
		return toBeSigned;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ToBeSigned getDataToSign(SignatureMultipleDocumentsForm form, List<Document> documentsToSign) throws DSSException, IOException {
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
	public DSSDocument signDocument(Long id, String userEppn, SignatureDocumentForm signatureDocumentForm, List<Document> documentsToSign) throws IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("Start signDocument with one document");
		DocumentSignatureService service = signService.getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		DSSDocument toSignDssDocument = dssUtilsService.toDSSDocument(signatureDocumentForm.getDocumentToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(signatureDocumentForm.getEncryptionAlgorithm(), signatureDocumentForm.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, signatureDocumentForm.getSignatureValue());
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, signatureDocumentForm, documentsToSign);
		DSSDocument signedDocument = service.signDocument(toSignDssDocument, parameters, signatureValue);
		logger.info("End signDocument with one document");
		return signedDocument;
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument signMultipleDocuments(SignatureMultipleDocumentsForm signatureMultipleDocumentsForm, List<Document> documentsToSign) throws IOException {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = signService.getASiCSignatureService(signatureMultipleDocumentsForm.getSignatureForm());
		List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(signatureMultipleDocumentsForm.getDocumentsToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(signatureMultipleDocumentsForm.getEncryptionAlgorithm(), signatureMultipleDocumentsForm.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, signatureMultipleDocumentsForm.getSignatureValue());
		AbstractSignatureParameters parameters = getParameters(signatureMultipleDocumentsForm, documentsToSign);
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
	public NexuSignature saveNexuSignature(Long id, AbstractSignatureForm abstractSignatureForm) {
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
		nexuSignature.setSignWithExpiredCertificate(abstractSignatureForm.isSignWithExpiredCertificate());
		nexuSignature.getDocumentToSign().addAll(signService.getToSignDocuments(id));
		nexuSignatureRepository.save(nexuSignature);
		return nexuSignature;
	}

	@Transactional
	public NexuSignature getNexuSignature(Long id) {
		return nexuSignatureRepository.findBySignRequestId(id);
	}

	@Transactional
	public void deleteNexuSignature(Long id) {
		SignRequest signRequest = signRequestRepository.findById(id).orElseThrow();
		NexuSignature nexuSignature = nexuSignatureRepository.findBySignRequestId(id);
		if(nexuSignature != null) {
			signRequest.getSignedDocuments().removeAll(nexuSignature.getDocumentToSign());
			nexuSignatureRepository.delete(nexuSignature);
		}
	}

	@Transactional
	public AbstractSignatureForm getAbstractSignatureFormFromNexuSignature(NexuSignature nexuSignature) throws IOException {
		AbstractSignatureForm abstractSignatureForm = null;
		if(nexuSignature.getDocumentToSign().size() > 1) {
			abstractSignatureForm = new SignatureMultipleDocumentsForm();
			for(Document document : nexuSignature.getDocumentToSign()) {
				((SignatureMultipleDocumentsForm) abstractSignatureForm).getDocumentsToSign().add(new DssMultipartFile(document.getFileName(), document.getFileName(), document.getContentType(), document.getInputStream()));
			}
		} else {
			abstractSignatureForm = new SignatureDocumentForm();
			((SignatureDocumentForm) abstractSignatureForm).setDocumentToSign(new DssMultipartFile(nexuSignature.getDocumentToSign().get(0).getFileName(), nexuSignature.getDocumentToSign().get(0).getFileName(), nexuSignature.getDocumentToSign().get(0).getContentType(), nexuSignature.getDocumentToSign().get(0).getInputStream()));
		}
		abstractSignatureForm.setSigningDate(nexuSignature.getSigningDate());
		abstractSignatureForm.setSignatureValue(nexuSignature.getSignatureValue());
		abstractSignatureForm.setCertificate(nexuSignature.getCertificate());
		abstractSignatureForm.setCertificateChain(nexuSignature.getCertificateChain());
		abstractSignatureForm.setSignatureForm(nexuSignature.getSignatureForm());
		abstractSignatureForm.setDigestAlgorithm(nexuSignature.getDigestAlgorithm());
		abstractSignatureForm.setEncryptionAlgorithm(nexuSignature.getEncryptionAlgorithm());
		abstractSignatureForm.setSignatureLevel(nexuSignature.getSignatureLevel());
		abstractSignatureForm.setSignWithExpiredCertificate(nexuSignature.getSignWithExpiredCertificate());
		return abstractSignatureForm;
	}

	@Transactional
	public AbstractSignatureForm getSignatureForm(Long signRequestId) throws IOException, EsupSignatureRuntimeException {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
		return signService.getAbstractSignatureForm(signService.getToSignDocuments(signRequest.getId()), signRequest, false);
	}

	@Transactional
	public AbstractSignatureParameters<?> getSignatureParameters(SignRequest signRequest, String userEppn, AbstractSignatureForm abstractSignatureForm, List<Document> documentsToSign) throws IOException {
		User user = userService.getByEppn(userEppn);
		AbstractSignatureParameters<?> parameters = null;
		if(abstractSignatureForm instanceof SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) {
			parameters = getParameters(signatureMultipleDocumentsForm, documentsToSign);
		} else if(abstractSignatureForm instanceof SignatureDocumentForm signatureDocumentForm) {
			if(abstractSignatureForm.getSignatureForm().equals(SignatureForm.PAdES)) {
				if(!signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty()) {
					parameters = signService.fillVisibleParameters(signatureDocumentForm, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0), documentsToSign.get(0).getInputStream(), user, abstractSignatureForm.getSigningDate());
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
	public SignDocumentResponse getSignDocumentResponse(Long signRequestId, SignResponse signatureValue, AbstractSignatureForm abstractSignatureForm, String userEppn, List<Document> documentsToSign) throws EsupSignatureRuntimeException {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		SignDocumentResponse signedDocumentResponse;
		abstractSignatureForm.setSignatureValue(signatureValue.getSignatureValue());
		try {
			Document signedFile = nexuSign(signRequest, userEppn, abstractSignatureForm, documentsToSign);
			if(signedFile != null) {
				Certificate certificate = Certificate.getInstance(abstractSignatureForm.getCertificate());
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, certificate.getSubject().toString(), "", new Date(), signRequest.getViewedBy().contains(user), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0).getSignPageNumber(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0).getxPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0).getyPos());
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
	public Document nexuSign(SignRequest signRequest, String userEppn, AbstractSignatureForm signatureDocumentForm, List<Document> documentsToSign) throws IOException {
		logger.info(userEppn + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;
		if(signatureDocumentForm instanceof SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) {
			dssDocument = signMultipleDocuments(signatureMultipleDocumentsForm, documentsToSign);
		} else {
			dssDocument = signDocument(signRequest.getId(), userEppn, (SignatureDocumentForm) signatureDocumentForm, documentsToSign);
		}
		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());
		return documentService.addSignedFile(signRequest, signedDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(signedDocument.getName()), Files.probeContentType(Path.of(dssDocument.getName())));
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
		if (Utils.isCollectionNotEmpty(originalDocuments)) {
			parameters.setDetachedContents(originalDocuments);
		}
		DSSDocument extendedDoc = service.extendDocument(signedDocument, parameters);
		logger.info("End extend with one document");
		return extendedDoc;
	}

}
