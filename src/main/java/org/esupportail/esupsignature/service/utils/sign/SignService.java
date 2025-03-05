package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.AbstractSignatureParameters;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.x509.tsp.TimestampToken;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.signature.XAdESService;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.UserKeystoreService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.interfaces.certificat.impl.OpenXPKICertificatGenerationService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfParameters;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Service
@EnableConfigurationProperties(SignProperties.class)
public class SignService {

	private static final Logger logger = LoggerFactory.getLogger(SignService.class);

	final private SignProperties signProperties;

	@Resource
	private CAdESService cadesService;

	@Resource
	private PAdESService padesService;

	@Resource
	private XAdESService xadesService;

	@Resource
	private ASiCWithCAdESService asicWithCAdESService;

	@Resource
	private ASiCWithXAdESService asicWithXAdESService;

	@Resource
	private DocumentService documentService;
	
	@Resource
	private FileService fileService;

	@Resource
	private PdfService pdfService;

	@Resource
	private UserService userService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private Environment environment;

	@Resource
	private UserKeystoreService userKeystoreService;

	@Resource
	private CertificatService certificatService;

	@Resource
	private ValidationService validationService;

	@Resource
	private DSSProperties dssProperties;

	@Resource
	private GlobalProperties globalProperties;


	private final OpenXPKICertificatGenerationService openXPKICertificatGenerationService;

	@Resource
	private DssUtilsService dssUtilsService;

	public SignService(@Autowired(required = false) OpenXPKICertificatGenerationService openXPKICertificatGenerationService, SignProperties signProperties) {
		this.openXPKICertificatGenerationService = openXPKICertificatGenerationService;
		this.signProperties = signProperties;
	}

	@Transactional
	public List<Document> getToSignDocuments(Long signRequestId) {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && !signRequest.getSignedDocuments().isEmpty()) {
			documents.add(signRequest.getLastSignedDocument());
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	@Transactional
	public Document certSign(SignRequest signRequest, String userEppn, String password, SignWith signWith) throws EsupSignatureRuntimeException {
		User user = userService.getByEppn(userEppn);
		logger.info("start certSign for signRequest : " + signRequest.getId());
		SignatureForm signatureForm;
		List<Document> toSignDocuments = new ArrayList<>(getToSignDocuments(signRequest.getId()));
		SignatureTokenConnection abstractKeyStoreTokenConnection = null;
		try {
			if(signWith.equals(SignWith.userCert)) {
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(user.getKeystore().getInputStream(), password);
			} else if(signWith.equals(SignWith.autoCert)){
				Certificat certificat = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getCertificat();
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(certificat.getKeystore().getInputStream(), certificatService.decryptPassword(certificat.getPassword()));
			} else if(signWith.equals(SignWith.groupCert)){
				Certificat certificat = certificatService.getCertificatByUser(userEppn).get(0);
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(certificat.getKeystore().getInputStream(), certificatService.decryptPassword(certificat.getPassword()));
			} else if ((signWith.equals(SignWith.sealCert) && (userService.getRoles(userEppn).contains("ROLE_SEAL")) || userEppn.equals("system"))) {
				try {
					if(!userEppn.equals("system") || certificatService.getOpenSCKey() != null || StringUtils.hasText(globalProperties.getSealCertificatFile())) {
						abstractKeyStoreTokenConnection = certificatService.getSealToken();
					}
				} catch (Exception e) {
					throw new EsupSignatureRuntimeException("unable to open seal token", e);
				}
			} else if (signWith.equals(SignWith.openPkiCert)) {
				abstractKeyStoreTokenConnection = openXPKICertificatGenerationService.generateTokenForUser(user);
			}
			if(abstractKeyStoreTokenConnection == null) {
				throw new EsupSignatureRuntimeException("Aucun certificat disponible pour signer ou sceller le document");
			}
			CertificateToken certificateToken = userKeystoreService.getCertificateToken(abstractKeyStoreTokenConnection);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(abstractKeyStoreTokenConnection);
			AbstractSignatureForm signatureDocumentForm = getAbstractSignatureForm(toSignDocuments, signRequest, true);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(certificateToken.getSignatureAlgorithm().getEncryptionAlgorithm());
			signatureDocumentForm.setCertificate(certificateToken.getEncoded());
			List<byte[]> base64CertificateChain = new ArrayList<>();
			for (CertificateToken token : certificateTokenChain) {
				base64CertificateChain.add(token.getEncoded());
			}
			signatureDocumentForm.setCertificateChain(base64CertificateChain);
			AbstractSignatureParameters<?> parameters;
			if(signatureForm.equals(SignatureForm.CAdES)) {
				ASiCWithCAdESSignatureParameters aSiCWithCAdESSignatureParameters = new ASiCWithCAdESSignatureParameters();
				aSiCWithCAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				aSiCWithCAdESSignatureParameters.aSiC().setMimeType("application/vnd.etsi.asic-e+zip");
				parameters = aSiCWithCAdESSignatureParameters;
			} else if(signatureForm.equals(SignatureForm.XAdES)) {
				ASiCWithXAdESSignatureParameters aSiCWithXAdESSignatureParameters = new ASiCWithXAdESSignatureParameters();
				aSiCWithXAdESSignatureParameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
				aSiCWithXAdESSignatureParameters.aSiC().setMimeType("application/vnd.etsi.asic-e+zip");
				parameters = aSiCWithXAdESSignatureParameters;
			} else {
				List<SignRequestParams> signRequestParamsesForSign = signRequest.getSignRequestParams().stream().filter(srp -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().contains(srp) && srp.getSignImageNumber() >= 0 && srp.getTextPart() == null).toList();
				if((abstractKeyStoreTokenConnection instanceof OpenSCSignatureToken
                        || abstractKeyStoreTokenConnection instanceof Pkcs11SignatureToken
                        || abstractKeyStoreTokenConnection instanceof Pkcs12SignatureToken)
                        && signRequestParamsesForSign.size() == 1 && !user.getEppn().equals("system")) {
					parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequestParamsesForSign.get(0) , new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign().getBytes()), user, signatureDocumentForm.getSigningDate());
				} else {
					parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, user);
				}
			}
			if(parameters instanceof PAdESSignatureParameters pAdESSignatureParameters) {
				pAdESSignatureParameters.setReason("Signé par " + user.getFirstname() + " " + user.getName() + ", " + user.getEmail());
			}
			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			validationService.checkRevocation(signatureDocumentForm, certificateToken, parameters);
			DSSDocument dssDocument;
			if (signatureDocumentForm instanceof SignatureMultipleDocumentsForm) {
				dssDocument = certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, abstractKeyStoreTokenConnection);
			} else {
				dssDocument = certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, abstractKeyStoreTokenConnection);
			}
			abstractKeyStoreTokenConnection.close();
			Document signedDocument = documentService.addSignedFile(signRequest, dssDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(dssDocument.getName()), Files.probeContentType(Path.of(dssDocument.getName())), user);
			logger.info("certSign ok for signRequest : " + signRequest.getId());
			return signedDocument;
		} catch (EsupSignatureKeystoreException e) {
			if(abstractKeyStoreTokenConnection != null) abstractKeyStoreTokenConnection.close();
			throw new EsupSignatureKeystoreException(e.getMessage(), e);
		} catch (Exception e) {
			if(abstractKeyStoreTokenConnection != null) abstractKeyStoreTokenConnection.close();
			throw new EsupSignatureRuntimeException(e.getMessage(), e);
		}
	}

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm signatureDocumentForm, User user) {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(signatureDocumentForm.getSignaturePackaging());
		pAdESSignatureParameters.setSignerName(user.getFirstname() + " " + user.getName());
		pAdESSignatureParameters.setContactInfo(user.getEmail());
		fillCommonsParameters(pAdESSignatureParameters, signatureDocumentForm);
		return pAdESSignatureParameters;
	}

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm signatureDocumentForm, SignRequestParams signRequestParams, InputStream toSignFile, User user, Date date) throws IOException {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		SignatureImageParameters imageParameters = new SignatureImageParameters();
		InMemoryDocument fileDocumentImage;
		if(signRequestParams.getSignImageNumber() >= 0 && (signRequestParams.getSignImageNumber() == null || signRequestParams.getSignImageNumber() == 999998 || signRequestParams.getSignImageNumber() == 999999 || user.getSignImages().size() >= signRequestParams.getSignImageNumber() || user.getEppn().equals("system"))) {
			InputStream inputStream;
			if(user.getSignImages().size() > signRequestParams.getSignImageNumber() && signRequestParams.getAddImage()) {
				inputStream = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
			} else {
				inputStream = fileService.getDefaultImage(user.getName(), user.getFirstname(), user.getEmail(), true);
			}
			InputStream signImage = fileService.addTextToImage(inputStream, signRequestParams, SignType.nexuSign, user, date, userService.getRoles(user.getEppn()).contains("ROLE_OTP"));
			if(signRequestParams.getAddWatermark()) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				fileService.addImageWatermark(new ClassPathResource("/static/images/watermark.png").getInputStream(), signImage, outputStream, signRequestParams.getExtraOnTop());
				signImage = new ByteArrayInputStream(outputStream.toByteArray());
			}
			BufferedImage bufferedSignImage = ImageIO.read(signImage);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(bufferedSignImage, "png", os);
			fileDocumentImage = new InMemoryDocument(new ByteArrayInputStream(os.toByteArray()), "sign.png");
			fileDocumentImage.setMimeType(MimeTypeEnum.PNG);
			imageParameters.setImage(fileDocumentImage);
			SignatureFieldParameters signatureFieldParameters = imageParameters.getFieldParameters();
			signatureFieldParameters.setPage(signRequestParams.getSignPageNumber());
			imageParameters.getFieldParameters().setRotation(VisualSignatureRotation.AUTOMATIC);
			PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFile, signRequestParams.getSignPageNumber());

			int widthAdjusted = Math.round(signRequestParams.getSignWidth() * globalProperties.getFixFactor());
			int heightAdjusted = Math.round(signRequestParams.getSignHeight() * globalProperties.getFixFactor());

			if(pdfParameters.getRotation() == 0) {
				signatureFieldParameters.setWidth(widthAdjusted);
				signatureFieldParameters.setHeight(heightAdjusted);
				signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() * globalProperties.getFixFactor()));
			} else {
				signatureFieldParameters.setWidth(heightAdjusted);
				signatureFieldParameters.setHeight(widthAdjusted);
				signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() - 50 * globalProperties.getFixFactor()));
			}
			int yPos = Math.round(signRequestParams.getyPos() * globalProperties.getFixFactor());
			if(yPos < 0) yPos = 0;
			signatureFieldParameters.setOriginY(yPos);
			imageParameters.setFieldParameters(signatureFieldParameters);
			imageParameters.setDpi(globalProperties.getSignatureImageDpi());
			imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
			imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
			pAdESSignatureParameters.setImageParameters(imageParameters);
		}
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(signatureDocumentForm.getSignaturePackaging());
		pAdESSignatureParameters.setSignerName(user.getFirstname() + " " + user.getName());
		pAdESSignatureParameters.setContactInfo(user.getEmail());
		fillCommonsParameters(pAdESSignatureParameters, signatureDocumentForm);
		return pAdESSignatureParameters;
	}

	public void fillCommonsParameters(AbstractSignatureParameters<?> parameters, AbstractSignatureForm form) {
		parameters.setSignatureLevel(form.getSignatureLevel());
		parameters.setDigestAlgorithm(form.getDigestAlgorithm());
		parameters.bLevel().setSigningDate(form.getSigningDate());
		parameters.bLevel().setClaimedSignerRoles(List.of("Manager"));
		if(StringUtils.hasText(dssProperties.getCountry())) {
			eu.europa.esig.dss.model.SignerLocation signerLocation = new eu.europa.esig.dss.model.SignerLocation();
			signerLocation.setCountry(dssProperties.getCountry());
			signerLocation.setStateOrProvince(dssProperties.getStateOrProvince());
			signerLocation.setPostalCode(dssProperties.getPostalCode());
			signerLocation.setLocality(dssProperties.getLocality());
			parameters.bLevel().setSignerLocation(signerLocation);
		}
        CertificateToken signingCertificate = DSSUtils.loadCertificate(form.getCertificate());
		parameters.setSigningCertificate(signingCertificate);
		List<CertificateToken> certificateChain = new LinkedList<>();
		for (byte[] base64Certificate : form.getCertificateChain()) {
			certificateChain.add(DSSUtils.loadCertificate(base64Certificate));
		}
		parameters.setCertificateChain(certificateChain);
	}

	@Transactional
	public Reports validate(long signRequestId) throws IOException {
		List<Document> documents = getToSignDocuments(signRequestId);
		if(!documents.isEmpty()) {
			byte[] bytes = documents.get(0).getInputStream().readAllBytes();
			return validationService.validate(new ByteArrayInputStream(bytes), null);
		} else {
			return null;
		}
	}

	@Transactional
	public boolean isSigned(SignRequest signRequest, Reports reports) {
		try {
			if(reports == null) {
				reports = validate(signRequest.getId());
			}
			List<Document> documents = getToSignDocuments(signRequest.getId());
			if (!documents.isEmpty() && (signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null)) {
				return (reports != null && !reports.getSimpleReport().getSignatureIdList().isEmpty());
			}
		} catch (Exception e) {
			logger.error("error while checking if signRequest is signed", e);
		}
		return false;
	}

	@Transactional
	public boolean isSigned(SignBook signBook, Reports reports) {
		for (SignRequest signRequest : signBook.getSignRequests()) {
			if (isSigned(signRequest, reports)) {
				return true;
			}
		}
		return false;
	}

	@Transactional
	public AbstractSignatureForm getAbstractSignatureForm(List<Document> documents, SignRequest signRequest, boolean includeDocuments) throws IOException, EsupSignatureRuntimeException {
		SignatureForm signatureForm;
		AbstractSignatureForm abstractSignatureForm;
		if(documents.size() > 1) {
			signatureForm = signProperties.getDefaultSignatureForm();
			abstractSignatureForm = new SignatureMultipleDocumentsForm();
			if(includeDocuments) {
				List<DssMultipartFile> multipartFiles = new ArrayList<>();
				for (Document toSignFile : documents) {
					multipartFiles.add(new DssMultipartFile(toSignFile.getFileName(), toSignFile.getFileName(), toSignFile.getContentType(), toSignFile.getInputStream()));
				}
				((SignatureMultipleDocumentsForm) abstractSignatureForm).setDocumentsToSign(multipartFiles);
			}
			((SignatureMultipleDocumentsForm) abstractSignatureForm).setContainerType(signProperties.getContainerType());
		} else {
			InputStream inputStream;
			byte[] bytes;
			Document toSignFile = documents.get(0);
			if(toSignFile.getContentType().equals("application/pdf")) {
				signatureForm = SignatureForm.PAdES;
				if(toSignFile.getTransientInputStream() != null) {
					inputStream = toSignFile.getTransientInputStream();
				} else {
					inputStream = toSignFile.getInputStream();
				}
				bytes = inputStream.readAllBytes();
				if(!isSigned(signRequest, null) && !pdfService.isPdfAComplient(bytes)) {
					bytes = pdfService.convertToPDFA(pdfService.writeMetadatas(bytes, toSignFile.getFileName(), signRequest, new ArrayList<>()));
				}
			} else {
				signatureForm = signProperties.getDefaultSignatureForm();
				bytes = toSignFile.getInputStream().readAllBytes();
			}
			abstractSignatureForm = new SignatureDocumentForm();
			if(includeDocuments) {
				((SignatureDocumentForm) abstractSignatureForm).setDocumentToSign(new DssMultipartFile(toSignFile.getFileName(), toSignFile.getFileName(), toSignFile.getContentType(), bytes));
			} else {
				documentService.addSignedFile(signRequest, new ByteArrayInputStream(bytes), toSignFile.getFileName(), toSignFile.getContentType(), userService.getSystemUser());
			}
			if(!signatureForm.equals(SignatureForm.PAdES)) {
				((SignatureDocumentForm) abstractSignatureForm).setContainerType(signProperties.getContainerType());
			}
		}
		abstractSignatureForm.setSignatureForm(signatureForm);
		if(signatureForm.equals(SignatureForm.PAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getPadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getPadesDigestAlgorithm());
		} else if(signatureForm.equals(SignatureForm.CAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getCadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getCadesDigestAlgorithm());
		} else if(signatureForm.equals(SignatureForm.XAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getXadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getXadesDigestAlgorithm());
		}
		abstractSignatureForm.setSigningDate(new Date());
		return abstractSignatureForm;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureDocumentForm signatureDocumentForm, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) throws IOException {
		DocumentSignatureService service = getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		DSSDocument toSignDocument = new InMemoryDocument(new ByteArrayInputStream(signatureDocumentForm.getDocumentToSign().getBytes()));
		ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		return service.signDocument(toSignDocument, parameters, signatureValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureMultipleDocumentsForm form, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(form.getDocumentsToSign());
		ToBeSigned dataToSign = service.getDataToSign(toSignDocuments, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		DSSDocument signedDocument = service.signDocument(toSignDocuments, parameters, signatureValue);
		logger.info("End signDocument with multiple documents");
		return signedDocument;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimestampToken getContentTimestamp(SignatureMultipleDocumentsForm form, AbstractSignatureParameters<?> parameters) {
		logger.info("Start getContentTimestamp with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		TimestampToken contentTimestamp = service.getContentTimestamp(dssUtilsService.toDSSDocuments(form.getDocumentsToSign()), parameters);
		logger.info("End getContentTimestamp with  multiple documents");
		return contentTimestamp;
	}

	@SuppressWarnings("rawtypes")
	public DocumentSignatureService getSignatureService(ASiCContainerType containerType, SignatureForm signatureForm) {
		DocumentSignatureService service = null;
		if (containerType != null) {
			service = (DocumentSignatureService) getASiCSignatureService(signatureForm);
		} else {
			switch (signatureForm) {
				case CAdES:
					service = cadesService;
					break;
				case PAdES:
					service = padesService;
					break;
				case XAdES:
					service = xadesService;
					break;
				default:
					logger.error("Unknow signature form : " + signatureForm);
				}
		}
		return service;
	}

	public MultipleDocumentsSignatureService<?, ?> getASiCSignatureService(SignatureForm signatureForm) {
		MultipleDocumentsSignatureService<?, ?> service = null;
		switch (signatureForm) {
		case CAdES:
			service = asicWithCAdESService;
			break;
		case XAdES:
			service = asicWithXAdESService;
			break;
		default:
			logger.error("Unknow signature form : " + signatureForm);
		}
		return service;
	}

	public AbstractSignatureParameters<?> getASiCSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters<?> parameters = null;
		switch (signatureForm) {
			case CAdES:
				ASiCWithCAdESSignatureParameters asicCadesParams = new ASiCWithCAdESSignatureParameters();
				asicCadesParams.aSiC().setContainerType(containerType);
				parameters = asicCadesParams;
				break;
			case XAdES:
				ASiCWithXAdESSignatureParameters asicXadesParams = new ASiCWithXAdESSignatureParameters();
				asicXadesParams.aSiC().setContainerType(containerType);
				parameters = asicXadesParams;
				break;
			default:
				logger.error("Unknow signature form for ASiC container: " + signatureForm);
		}
		return parameters;
	}

	public SignatureForm getDefaultSignatureForm() {
		return signProperties.getDefaultSignatureForm();
	}

	public Long getPasswordTimeout() {
		return signProperties.getPasswordTimeout();
	}

	public boolean checkSignTypeDocType(SignType signType, MultipartFile multipartFile) {
		boolean check = true;
		if(!multipartFile.getContentType().toLowerCase().contains("pdf") && !multipartFile.getContentType().toLowerCase().contains("image")) {
			if(signType.equals(SignType.pdfImageStamp) || signType.equals(SignType.visa)) {
				check = false;
			}
		}
		return check;
	}

}
