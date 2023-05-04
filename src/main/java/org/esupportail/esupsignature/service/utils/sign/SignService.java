package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.CAdESSignatureParameters;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.signature.DocumentSignatureService;
import eu.europa.esig.dss.signature.MultipleDocumentsSignatureService;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.Pkcs11SignatureToken;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.apache.commons.codec.binary.Base64;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureKeystoreException;
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
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
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
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(SignProperties.class)
public class SignService {

	Float fixFactor = .75f;

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

	private final OpenXPKICertificatGenerationService openXPKICertificatGenerationService;

	public SignService(@Autowired(required = false) OpenXPKICertificatGenerationService openXPKICertificatGenerationService, SignProperties signProperties) {
		this.openXPKICertificatGenerationService = openXPKICertificatGenerationService;
		this.signProperties = signProperties;
	}

	@Transactional
	public List<Document> getToSignDocuments(Long signRequestId) {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && signRequest.getSignedDocuments().size() > 0 ) {
			documents.add(signRequest.getLastSignedDocument());
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	public Document certSign(SignRequest signRequest, String userEppn, String password, SignWith signWith) throws EsupSignatureRuntimeException {
		User user = userService.getByEppn(userEppn);
		logger.info("start certSign for signRequest : " + signRequest.getId());
		SignatureForm signatureForm;
		List<Document> toSignDocuments = new ArrayList<>(getToSignDocuments(signRequest.getId()));
		AbstractKeyStoreTokenConnection abstractKeyStoreTokenConnection = null;
		try {
			if(signWith.equals(SignWith.userCert)) {
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(user.getKeystore().getInputStream(), password);
			} else if(signWith.equals(SignWith.autoCert)){
				Certificat certificat = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getCertificat();
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(certificat.getKeystore().getInputStream(), certificatService.decryptPassword(certificat.getPassword()));
			} else if(signWith.equals(SignWith.groupCert)){
				Certificat certificat = certificatService.getCertificatByUser(userEppn).get(0);
				abstractKeyStoreTokenConnection = userKeystoreService.getPkcs12Token(certificat.getKeystore().getInputStream(), certificatService.decryptPassword(certificat.getPassword()));
			} else if (signWith.equals(SignWith.sealCert) && (user.getRoles().contains("ROLE_SEAL") || userEppn.equals("system"))) {
				try {
					abstractKeyStoreTokenConnection = certificatService.getSealToken();
				} catch (Exception e) {
					throw new EsupSignatureRuntimeException("unable to open seal token", e);
				}
			} else if (signWith.equals(SignWith.openPkiCert)) {
				abstractKeyStoreTokenConnection = openXPKICertificatGenerationService.generateTokenForUser(user);
			} else {
				throw new EsupSignatureRuntimeException("Aucun certificat disponible pour signer le document");
			}

			CertificateToken certificateToken = userKeystoreService.getCertificateToken(abstractKeyStoreTokenConnection);
			CertificateToken[] certificateTokenChain = userKeystoreService.getCertificateTokenChain(abstractKeyStoreTokenConnection);
			AbstractSignatureForm signatureDocumentForm = getSignatureDocumentForm(toSignDocuments, signRequest, user, new Date(), abstractKeyStoreTokenConnection instanceof Pkcs11SignatureToken);
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(EncryptionAlgorithm.RSA);
			signatureDocumentForm.setBase64Certificate(Base64.encodeBase64String(certificateToken.getEncoded()));
			List<String> base64CertificateChain = new ArrayList<>();
			for (CertificateToken token : certificateTokenChain) {
				base64CertificateChain.add(Base64.encodeBase64String(token.getEncoded()));
			}
			signatureDocumentForm.setBase64CertificateChain(base64CertificateChain);
			AbstractSignatureParameters parameters;
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
				List<SignRequestParams> signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
				List<SignRequestParams> signRequestParamsesForSign = signRequestParamses.stream().filter(srp -> srp.getSignImageNumber() >= 0 && srp.getTextPart() == null).collect(Collectors.toList());
				if(abstractKeyStoreTokenConnection instanceof Pkcs12SignatureToken && signRequestParamsesForSign.size() == 1) {
					parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequestParamsesForSign.get(0) , new ByteArrayInputStream(((SignatureDocumentForm) signatureDocumentForm).getDocumentToSign()), new Color(214, 0, 128), user, signatureDocumentForm.getSigningDate());
				} else {
					parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, user);
				}
			}
			parameters.setSigningCertificate(certificateToken);
			parameters.setCertificateChain(certificateTokenChain);
			parameters.setSignatureLevel(signatureDocumentForm.getSignatureLevel());
			parameters.bLevel().setSigningDate(signatureDocumentForm.getSigningDate());
			// not needed with baseline_t
//			signatureDocumentForm.setContentTimestamp(DssUtils.fromTimestampToken(getContentTimestamp((SignatureDocumentForm) signatureDocumentForm)));
//			signatureDocumentForm.setAddContentTimestamp(true);
			DSSDocument dssDocument;
			if(signatureDocumentForm instanceof SignatureMultipleDocumentsForm) {
				dssDocument = certSignDocument((SignatureMultipleDocumentsForm) signatureDocumentForm, parameters, abstractKeyStoreTokenConnection);
			} else {
				dssDocument = certSignDocument((SignatureDocumentForm) signatureDocumentForm, parameters, abstractKeyStoreTokenConnection);
			}
			abstractKeyStoreTokenConnection.close();
			Document signedDocument = documentService.addSignedFile(signRequest, dssDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(dssDocument.getName()), Files.probeContentType(Path.of(dssDocument.getName())));
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


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument extend(ExtensionForm extensionForm) {

		ASiCContainerType containerType = extensionForm.getContainerType();
		SignatureForm signatureForm = extensionForm.getSignatureForm();

		DSSDocument signedDocument = DssUtils.toDSSDocument(extensionForm.getSignedFile());
		List<DSSDocument> originalDocuments = DssUtils.toDSSDocuments(extensionForm.getOriginalFiles());

		DocumentSignatureService service = getSignatureService(containerType, signatureForm);

		AbstractSignatureParameters parameters = getSignatureParameters(containerType, signatureForm);
		parameters.setSignatureLevel(extensionForm.getSignatureLevel());

		if (Utils.isCollectionNotEmpty(originalDocuments)) {
			parameters.setDetachedContents(originalDocuments);
		}

		DSSDocument extendedDoc = service.extendDocument(signedDocument, parameters);
		logger.info("End extend with one document");
		return extendedDoc;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public ToBeSigned getDataToSign(Long id, String userEppn, SignatureDocumentForm form) throws DSSException, IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		DSSDocument toSignDocument = DssUtils.toDSSDocument(new ByteArrayInputStream(form.getDocumentToSign()));
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, form);
		ToBeSigned  toBeSigned = service.getDataToSign(toSignDocument, parameters);
		logger.info("End getDataToSign with one document");
		return toBeSigned;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ToBeSigned getDataToSign(SignatureMultipleDocumentsForm form) {
		logger.info("Start getDataToSign with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());

		AbstractSignatureParameters parameters = fillCommonsParameters(form);

		ToBeSigned toBeSigned = null;
		try {
			List<DSSDocument> toSignDocuments = DssUtils.toDSSDocuments(form.getDocumentsToSign());
			toBeSigned = service.getDataToSign(toSignDocuments, parameters);
		} catch (Exception e) {
			logger.error("Unable to execute getDataToSign : " + e.getMessage(), e);
		}
		logger.info("End getDataToSign with multiple documents");
		return toBeSigned;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimestampToken getContentTimestamp(SignatureDocumentForm form) {
		logger.info("Start getContentTimestamp with one document");

		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		AbstractSignatureParameters parameters = getParameters(form);
		DSSDocument toSignDocument = DssUtils.toDSSDocument(new ByteArrayInputStream(form.getDocumentToSign()));

		TimestampToken contentTimestamp = service.getContentTimestamp(toSignDocument, parameters);

		logger.info("End getContentTimestamp with one document");
		return contentTimestamp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimestampToken getContentTimestamp(SignatureMultipleDocumentsForm form) {
		logger.info("Start getContentTimestamp with multiple documents");

		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		AbstractSignatureParameters parameters = fillCommonsParameters(form);

		TimestampToken contentTimestamp = service.getContentTimestamp(DssUtils.toDSSDocuments(form.getDocumentsToSign()), parameters);

		logger.info("End getContentTimestamp with  multiple documents");
		return contentTimestamp;
	}

	public AbstractSignatureParameters<?> fillCommonsParameters(SignatureMultipleDocumentsForm form) {
		AbstractSignatureParameters<?> finalParameters = getASiCSignatureParameters(form.getContainerType(), form.getSignatureForm());
		fillCommonsParameters(finalParameters, form);
		return finalParameters;
	}

	public AbstractSignatureParameters getParameters(SignatureDocumentForm form) {
		AbstractSignatureParameters parameters = getSignatureParameters(form.getContainerType(), form.getSignatureForm());
		parameters.setSignaturePackaging(form.getSignaturePackaging());
		fillCommonsParameters(parameters, form);
		return parameters;
	}

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm form, User user) {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(form.getSignaturePackaging());
		fillCommonsParameters(pAdESSignatureParameters, form);
		pAdESSignatureParameters.setSignerName(user.getFirstname() + " " + user.getName());
		pAdESSignatureParameters.setContactInfo(user.getEmail());
		return pAdESSignatureParameters;
	}

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm form, SignRequestParams signRequestParams, InputStream toSignFile, Color color, User user, Date date) throws IOException {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		SignatureImageParameters imageParameters = new SignatureImageParameters();
		InMemoryDocument fileDocumentImage;
		if(signRequestParams.getSignImageNumber() >= 0 && (signRequestParams.getSignImageNumber() == null || user.getSignImages().size() >= signRequestParams.getSignImageNumber() || user.getEppn().equals("system"))) {
			InputStream inputStream;
			if(user.getSignImages().size() > signRequestParams.getSignImageNumber() && signRequestParams.getAddImage()) {
				inputStream = user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream();
			} else {
				inputStream = fileService.getDefaultImage(user.getName(), user.getFirstname());
			}
			InputStream signImage = fileService.addTextToImage(inputStream, signRequestParams, SignType.nexuSign, user, date, fixFactor);
			if(signRequestParams.getAddWatermark()) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				fileService.addImageWatermark(new ClassPathResource("/static/images/watermark.png").getInputStream(), signImage, outputStream, color, signRequestParams.getExtraOnTop());
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
			imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
			PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFile, signRequestParams.getSignPageNumber());

			int widthAdjusted = Math.round(signRequestParams.getSignWidth() * fixFactor);
			int heightAdjusted = Math.round(signRequestParams.getSignHeight() * fixFactor);

			if(pdfParameters.getRotation() == 0) {
				signatureFieldParameters.setWidth(widthAdjusted);
				signatureFieldParameters.setHeight(heightAdjusted);
				signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() * fixFactor));
			} else {
				signatureFieldParameters.setWidth(heightAdjusted);
				signatureFieldParameters.setHeight(widthAdjusted);
				signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() - 50 * fixFactor));
			}
			int yPos = Math.round(signRequestParams.getyPos() * fixFactor);
			signatureFieldParameters.setOriginY(yPos);
			imageParameters.setFieldParameters(signatureFieldParameters);
			imageParameters.setDpi(300);
			imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
			imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
			pAdESSignatureParameters.setImageParameters(imageParameters);
		}
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(form.getSignaturePackaging());

		fillCommonsParameters(pAdESSignatureParameters, form);
		pAdESSignatureParameters.setSignerName(user.getFirstname() + " " + user.getName());
		pAdESSignatureParameters.setContactInfo(user.getEmail());
		return pAdESSignatureParameters;
	}
	
	public static BufferedImage scaleWithPadding(BufferedImage img, int newWidth, int newHeight) {
        int currentWidth = img.getWidth();
        int currentHeight = img.getHeight();
 
        int scaledWidth;
        int scaledHeight;
        if (currentWidth == 0 || currentHeight == 0
            || (currentWidth == newWidth && currentHeight == newHeight)) {
                return img;
        } else if (currentWidth == currentHeight) {
                scaledWidth = newWidth;
                scaledHeight = newHeight;
        } else if (currentWidth >= currentHeight) {
                scaledWidth = newWidth;
                double scale = (double) newWidth / (double) currentWidth;
                scaledHeight = (int) Math.round(currentHeight * scale);
        } else {
                scaledHeight = newHeight;
                double scale = (double) newHeight / (double) currentHeight;
                scaledWidth = (int) Math.round(currentWidth * scale);
        }
 
        int x = (newWidth - scaledWidth) / 2;
        int y = (newHeight - scaledHeight) / 2;
 
        /*
         * This is _very_ painful. I've tried a large number of different permutations here trying to
         * get the white image background to be transparent without success. We've tried different
         * fills, composite types, image types, etc.. I'm moving on now.
         */
        BufferedImage newImg = new BufferedImage(newWidth, newHeight, img.getType());
        Graphics2D g = newImg.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, newWidth, newHeight);
        g.drawImage(img, x, y, x + scaledWidth, y + scaledHeight, 0, 0, currentWidth, currentHeight,
                    Color.WHITE, null);
        g.dispose();
 
        return newImg;
}
	
	private void fillCommonsParameters(AbstractSignatureParameters<?> parameters, AbstractSignatureForm form) {
		parameters.setSignatureLevel(form.getSignatureLevel());
		parameters.setDigestAlgorithm(form.getDigestAlgorithm());
		//parameters.setEncryptionAlgorithm(form.getEncryptionAlgorithm()); retrieved from certificate
		parameters.bLevel().setSigningDate(form.getSigningDate());
		parameters.setSignWithExpiredCertificate(form.isSignWithExpiredCertificate());

		if (form.getContentTimestamp() != null) {
			parameters.setContentTimestamps(List.of(DssUtils.toTimestampToken(form.getContentTimestamp())));
		}

		CertificateToken signingCertificate = DSSUtils.loadCertificateFromBase64EncodedString(form.getBase64Certificate());
		parameters.setSigningCertificate(signingCertificate);

		List<String> base64CertificateChain = form.getBase64CertificateChain();
		if (Utils.isCollectionNotEmpty(base64CertificateChain)) {
			List<CertificateToken> certificateChain = new LinkedList<>();
			for (String base64Certificate : base64CertificateChain) {
				certificateChain.add(DSSUtils.loadCertificateFromBase64EncodedString(base64Certificate));
			}
			parameters.setCertificateChain(certificateChain);
		}

	}

	@Transactional
	public boolean isNotSigned(SignRequest signRequest) throws IOException {
		List<Document> documents = getToSignDocuments(signRequest.getId());
		if(documents.size() > 0 && (signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.certSign) || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.nexuSign)))) {
			byte[] bytes = getToSignDocuments(signRequest.getId()).get(0).getInputStream().readAllBytes();
			Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
			return signRequest.getSignedDocuments().size() == 0 && reports != null && reports.getSimpleReport().getSignatureIdList().size() == 0;
		} else {
			return true;
		}
	}

	public AbstractSignatureForm getSignatureDocumentForm(List<Document> documents, SignRequest signRequest, User user, Date date, boolean sealSign) throws IOException, EsupSignatureRuntimeException {
		SignatureForm signatureForm;
		AbstractSignatureForm abstractSignatureForm;
		if(documents.size() > 1) {
			signatureForm = signProperties.getDefaultSignatureForm();
			SignatureMultipleDocumentsForm signatureMultipleDocumentsForm = new SignatureMultipleDocumentsForm();
			List<MultipartFile> multipartFiles = new ArrayList<>();
			for(Document toSignFile : documents) {
				multipartFiles.add(fileService.toMultipartFile(toSignFile.getInputStream(), toSignFile.getFileName(), toSignFile.getContentType()));
			}
			signatureMultipleDocumentsForm.setDocumentsToSign(multipartFiles);
			signatureMultipleDocumentsForm.setContainerType(signProperties.getContainerType());
			abstractSignatureForm = signatureMultipleDocumentsForm;
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
				if(isNotSigned(signRequest) && !pdfService.isPdfAComplient(bytes)) {
//					int i = 0;
//					if(sealSign) i = 1;
//					List<SignRequestParams> signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
//					for(SignRequestParams signRequestParams : signRequestParamses.stream().filter(srp -> srp.getSignImageNumber() < 0).collect(Collectors.toList())) {
//						bytes = pdfService.stampImage(bytes, signRequest, signRequestParams, i, user, date);
//						i++;
//					}
					bytes = pdfService.convertGS(pdfService.writeMetadatas(bytes, toSignFile.getFileName(), signRequest, new ArrayList<>()));
				}
			} else {
				signatureForm = signProperties.getDefaultSignatureForm();
				bytes = toSignFile.getInputStream().readAllBytes();
			}
			SignatureDocumentForm signatureDocumentForm = new SignatureDocumentForm();
			signatureDocumentForm.setDocumentToSign(bytes);
			if(!signatureForm.equals(SignatureForm.PAdES)) {
				signatureDocumentForm.setContainerType(signProperties.getContainerType());
			}
			abstractSignatureForm = signatureDocumentForm;
		}
		if(environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("dev")) {
			abstractSignatureForm.setSignWithExpiredCertificate(true);
		}
		abstractSignatureForm.setSignatureForm(signatureForm);
		if(signatureForm.equals(SignatureForm.PAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getPadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getPadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signProperties.getSignaturePackaging());
		} else if(signatureForm.equals(SignatureForm.CAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getCadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getCadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signProperties.getSignaturePackaging());
		} else if(signatureForm.equals(SignatureForm.XAdES)) {
			abstractSignatureForm.setSignatureLevel(signProperties.getXadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signProperties.getXadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signProperties.getSignaturePackaging());
		}
		abstractSignatureForm.setSigningDate(date);
		return abstractSignatureForm;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureDocumentForm signatureDocumentForm, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) {
		DocumentSignatureService service = getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		fillCommonsParameters(parameters, signatureDocumentForm);
		DSSDocument toSignDocument = new InMemoryDocument(new ByteArrayInputStream(signatureDocumentForm.getDocumentToSign()));
		ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		return service.signDocument(toSignDocument, parameters, signatureValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureMultipleDocumentsForm form, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		List<DSSDocument> toSignDocuments = DssUtils.toDSSDocuments(form.getDocumentsToSign());
		ToBeSigned dataToSign = service.getDataToSign(toSignDocuments, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		DSSDocument signedDocument = service.signDocument(toSignDocuments, parameters, signatureValue);
		logger.info("End signDocument with multiple documents");
		return signedDocument;
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Transactional
	public DSSDocument nexuSignDocument(Long id, String userEppn, SignatureDocumentForm form) throws IOException {
		SignRequest signRequest = signRequestRepository.findById(id).get();
		AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, form);
		logger.info("Start signDocument with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		DSSDocument toSignDssDocument = DssUtils.toDSSDocument(new ByteArrayInputStream(form.getDocumentToSign()));
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, Utils.fromBase64(form.getBase64SignatureValue()));
		DSSDocument signedDocument = service.signDocument(toSignDssDocument, parameters, signatureValue);
		logger.info("End signDocument with one document");
		return signedDocument;
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument signDocument(SignatureMultipleDocumentsForm form) {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		AbstractSignatureParameters parameters = fillCommonsParameters(form);
		List<DSSDocument> toSignDocuments = DssUtils.toDSSDocuments(form.getDocumentsToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, Utils.fromBase64(form.getBase64SignatureValue()));
		DSSDocument signedDocument = service.signDocument(toSignDocuments, parameters, signatureValue);
		logger.info("End signDocument with multiple documents");
		return signedDocument;
	}

	@SuppressWarnings("rawtypes")
	private DocumentSignatureService getSignatureService(ASiCContainerType containerType, SignatureForm signatureForm) {
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

	private AbstractSignatureParameters getSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters parameters = null;
		if (containerType != null) {
			parameters = getASiCSignatureParameters(containerType, signatureForm);
		} else {
			switch (signatureForm) {
			case CAdES:
				parameters = new CAdESSignatureParameters();
				break;
			case PAdES:
				PAdESSignatureParameters padesParams = new PAdESSignatureParameters();
				padesParams.setContentSize(9472 * 2); // double reserved space for signature
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

	@SuppressWarnings("rawtypes")
	private MultipleDocumentsSignatureService getASiCSignatureService(SignatureForm signatureForm) {
		MultipleDocumentsSignatureService service = null;
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

	private AbstractSignatureParameters<?> getASiCSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
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

	@Transactional
	public AbstractSignatureForm getAbstractSignatureForm(Long signRequestId, String userEppn) throws IOException, EsupSignatureRuntimeException {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		List<SignRequestParams> liveWfSignRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
		return getSignatureDocumentForm(getToSignDocuments(signRequest.getId()), signRequest, user, new Date(), false);
	}

	@Transactional
	public AbstractSignatureParameters<?> getSignatureParameters(SignRequest signRequest, String userEppn, AbstractSignatureForm abstractSignatureForm) throws IOException {
		User user = userService.getByEppn(userEppn);
		AbstractSignatureParameters<?> parameters;
		if(abstractSignatureForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
			parameters = fillCommonsParameters((SignatureMultipleDocumentsForm) abstractSignatureForm);
		} else {
			if(abstractSignatureForm.getSignatureForm().equals(SignatureForm.PAdES)) {
				SignatureDocumentForm documentForm = (SignatureDocumentForm) abstractSignatureForm;
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().size() > 0) {
					parameters = fillVisibleParameters((SignatureDocumentForm) abstractSignatureForm, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0), new ByteArrayInputStream(documentForm.getDocumentToSign()), new Color(61, 170, 231), user, abstractSignatureForm.getSigningDate());
				} else {
					parameters = fillVisibleParameters((SignatureDocumentForm) abstractSignatureForm, user);
				}
			} else {
				parameters = getParameters((SignatureDocumentForm) abstractSignatureForm);
			}
		}
		return parameters;
	}

	@Transactional
	public SignDocumentResponse getSignDocumentResponse(Long signRequestId, SignatureValueAsString signatureValue, AbstractSignatureForm abstractSignatureForm, String userEppn, String authUserEppn) throws EsupSignatureRuntimeException {
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		SignDocumentResponse signedDocumentResponse;
		abstractSignatureForm.setBase64SignatureValue(signatureValue.getSignatureValue());
		try {
			Document signedFile = nexuSign(signRequest, userEppn, abstractSignatureForm);
			if(signedFile != null) {
				signedDocumentResponse = new SignDocumentResponse();
				signedDocumentResponse.setUrlToDownload("download");
				return signedDocumentResponse;
			}
		} catch (IOException e) {
			throw new EsupSignatureRuntimeException("unable to sign" , e);
		}
		return null;
	}

	public Document nexuSign(SignRequest signRequest, String userEppn, AbstractSignatureForm signatureDocumentForm) throws IOException {
		logger.info(userEppn + " launch nexu signature for signRequest : " + signRequest.getId());
		DSSDocument dssDocument;

		if(signatureDocumentForm instanceof SignatureMultipleDocumentsForm) {
			dssDocument = signDocument((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			dssDocument = nexuSignDocument(signRequest.getId(), userEppn, (SignatureDocumentForm) signatureDocumentForm);
		}

		InMemoryDocument signedDocument = new InMemoryDocument(DSSUtils.toByteArray(dssDocument), dssDocument.getName(), dssDocument.getMimeType());

		return documentService.addSignedFile(signRequest, signedDocument.openStream(), fileService.getNameOnly(signRequest.getTitle()) + "." + fileService.getExtension(signedDocument.getName()), Files.probeContentType(Path.of(dssDocument.getName())));
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
