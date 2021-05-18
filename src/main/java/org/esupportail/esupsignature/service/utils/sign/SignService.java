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
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.esupportail.esupsignature.config.sign.SignConfig;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfParameters;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

@Service
public class SignService {

	private static final Logger logger = LoggerFactory.getLogger(SignService.class);

	@Resource
	private SignConfig signConfig;

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
	private SignRequestService signRequestService;

	@Resource
	private Environment environment;

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
	public ToBeSigned getDataToSign(Long id, String userEppn, SignatureDocumentForm form) {
		SignRequest signRequest = signRequestService.getById(id);
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		ToBeSigned toBeSigned = null;
		try {
			DSSDocument toSignDocument = DssUtils.toDSSDocument(new ByteArrayInputStream(form.getDocumentToSign()));
			AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, form);
			toBeSigned = service.getDataToSign(toSignDocument, parameters);
		} catch (Exception e) {
			logger.error("Unable to execute getDataToSign : " + e.getMessage(), e);
		}
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

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm form, SignRequestParams signRequestParams, InputStream toSignFile, Color color, User user) throws IOException {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		SignatureImageParameters imageParameters = new SignatureImageParameters();
		InMemoryDocument fileDocumentImage;
		InputStream signImage;
		signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), signRequestParams, SignType.nexuSign);
		if(signRequestParams.getAddWatermark()) {
			File fileWithWatermark = fileService.getTempFile("sign_with_mark.png");
			fileService.addImageWatermark(PdfService.class.getResourceAsStream("/static/images/watermark.png"), signImage, fileWithWatermark, color);
			signImage = new FileInputStream(fileWithWatermark);
		}
		BufferedImage bufferedSignImage = ImageIO.read(signImage);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(bufferedSignImage, "png", os);
		fileDocumentImage = new InMemoryDocument(new ByteArrayInputStream(os.toByteArray()), "sign.png");
		fileDocumentImage.setMimeType(MimeType.PNG);
		imageParameters.setImage(fileDocumentImage);
		SignatureFieldParameters signatureFieldParameters = imageParameters.getFieldParameters();
		signatureFieldParameters.setPage(signRequestParams.getSignPageNumber());
		imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFile);
		if(signRequestParams.getAddExtra()) {
			signRequestParams.setSignWidth(signRequestParams.getSignWidth() + 200);
		}
		int widthAdjusted = Math.round((float) (bufferedSignImage.getWidth() / 3 * 0.75));
		int heightAdjusted = Math.round((float) (bufferedSignImage.getHeight() / 3 * 0.75));

		if(pdfParameters.getRotation() == 0) {
			signatureFieldParameters.setWidth(widthAdjusted);
			signatureFieldParameters.setHeight(heightAdjusted);
			signatureFieldParameters.setOriginX(signRequestParams.getxPos());
		} else {
			signatureFieldParameters.setWidth(heightAdjusted);
			signatureFieldParameters.setHeight(widthAdjusted);
			signatureFieldParameters.setOriginX(signRequestParams.getxPos() - 50);
		}
		int yPos = Math.round(signRequestParams.getyPos() - ((heightAdjusted - signRequestParams.getSignHeight())) / 0.75f);
		signatureFieldParameters.setOriginY(yPos);
		imageParameters.setFieldParameters(signatureFieldParameters);
		imageParameters.setDpi(300);
		imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
		imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
		pAdESSignatureParameters.setImageParameters(imageParameters);
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(form.getSignaturePackaging());

		fillCommonsParameters(pAdESSignatureParameters, form);
		pAdESSignatureParameters.setSignerName(user.getFirstname() + " " + user.getName());
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
	
	private void fillCommonsParameters(AbstractSignatureParameters parameters, AbstractSignatureForm form) {
		parameters.setSignatureLevel(form.getSignatureLevel());
		parameters.setDigestAlgorithm(form.getDigestAlgorithm());
		//parameters.setEncryptionAlgorithm(form.getEncryptionAlgorithm()); retrieved from certificate
		parameters.bLevel().setSigningDate(form.getSigningDate());
		parameters.setSignWithExpiredCertificate(form.isSignWithExpiredCertificate());

		if (form.getContentTimestamp() != null) {
			parameters.setContentTimestamps(Arrays.asList(DssUtils.toTimestampToken(form.getContentTimestamp())));
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

	public AbstractSignatureForm getSignatureDocumentForm(List<Document> documents, SignRequest signRequest, boolean visual) throws IOException, EsupSignatureException {
		SignatureForm signatureForm;
		AbstractSignatureForm abstractSignatureForm;
		if(documents.size() > 1) {
			signatureForm = signConfig.getSignProperties().getDefaultSignatureForm();
			SignatureMultipleDocumentsForm signatureMultipleDocumentsForm = new SignatureMultipleDocumentsForm();
			List<MultipartFile> multipartFiles = new ArrayList<>();
			for(Document toSignFile : documents) {
				multipartFiles.add(fileService.toMultipartFile(toSignFile.getInputStream(), toSignFile.getFileName(), toSignFile.getContentType()));
			}
			signatureMultipleDocumentsForm.setDocumentsToSign(multipartFiles);
			signatureMultipleDocumentsForm.setContainerType(signConfig.getSignProperties().getContainerType());
			abstractSignatureForm = signatureMultipleDocumentsForm;
		} else {
			InputStream inputStream;
			Document toSignFile = documents.get(0);
			if(toSignFile.getContentType().equals("application/pdf") && visual) {
				signatureForm = SignatureForm.PAdES;
				if(toSignFile.getTransientInputStream() != null) {
					inputStream = toSignFile.getTransientInputStream();
				} else {
					inputStream = toSignFile.getInputStream();
				}
				if(signRequest.getSignedDocuments().size() == 0) {
					inputStream = pdfService.convertGS(pdfService.writeMetadatas(inputStream, toSignFile.getFileName(), signRequest, new ArrayList<>()), signRequest.getToken());
				}
			} else {
				signatureForm = signConfig.getSignProperties().getDefaultSignatureForm();
				inputStream = toSignFile.getInputStream();
			}
			SignatureDocumentForm signatureDocumentForm = new SignatureDocumentForm();
			signatureDocumentForm.setDocumentToSign(inputStream.readAllBytes());
			if(!signatureForm.equals(SignatureForm.PAdES)) {
				signatureDocumentForm.setContainerType(signConfig.getSignProperties().getContainerType());
			}
			abstractSignatureForm = signatureDocumentForm;
		}
		if(environment.getActiveProfiles().length > 0 && environment.getActiveProfiles()[0].equals("dev")) {
			abstractSignatureForm.setSignWithExpiredCertificate(true);
		}
		abstractSignatureForm.setSignatureForm(signatureForm);
		if(signatureForm.equals(SignatureForm.PAdES)) {
			abstractSignatureForm.setSignatureLevel(signConfig.getSignProperties().getPadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signConfig.getSignProperties().getPadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signConfig.getSignProperties().getSignaturePackaging());
		} else if(signatureForm.equals(SignatureForm.CAdES)) {
			abstractSignatureForm.setSignatureLevel(signConfig.getSignProperties().getCadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signConfig.getSignProperties().getCadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signConfig.getSignProperties().getSignaturePackaging());
		} else if(signatureForm.equals(SignatureForm.XAdES)) {
			abstractSignatureForm.setSignatureLevel(signConfig.getSignProperties().getXadesSignatureLevel());
			abstractSignatureForm.setDigestAlgorithm(signConfig.getSignProperties().getXadesDigestAlgorithm());
			abstractSignatureForm.setSignaturePackaging(signConfig.getSignProperties().getSignaturePackaging());
		}
		abstractSignatureForm.setSigningDate(new Date());
		return abstractSignatureForm;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureDocumentForm signatureDocumentForm, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) {
		logger.info("Start certSignDocument with database keystore");
		DocumentSignatureService service = getSignatureService(signatureDocumentForm.getContainerType(), signatureDocumentForm.getSignatureForm());
		fillCommonsParameters(parameters, signatureDocumentForm);
		DSSDocument toSignDocument = new InMemoryDocument(new ByteArrayInputStream(signatureDocumentForm.getDocumentToSign()));
		ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
		logger.info("End certSignDocument with database keystore");
		return signedDocument;
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
		SignRequest signRequest = signRequestService.getById(id);
		Document toSignDocument = signRequestService.getToSignDocuments(id).get(0);
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
		return signConfig.getSignProperties().getDefaultSignatureForm();
	}

	public Long getPasswordTimeout() {
		return signConfig.getSignProperties().getPasswordTimeout();
	}

	@Transactional
	public AbstractSignatureForm getAbstractSignatureForm(Long signRequestId) throws IOException, EsupSignatureException {
		SignRequest signRequest = signRequestService.getById(signRequestId);
		List<SignRequestParams> liveWfSignRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
		return getSignatureDocumentForm(signRequestService.getToSignDocuments(signRequest.getId()), signRequest, liveWfSignRequestParams.get(0).getVisual());
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
				parameters = fillVisibleParameters((SignatureDocumentForm) abstractSignatureForm, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0), new ByteArrayInputStream(documentForm.getDocumentToSign()),new Color(61, 170, 231), user);
			} else {
				parameters = getParameters((SignatureDocumentForm) abstractSignatureForm);
			}
		}
		return parameters;
	}

	@Transactional
	public SignDocumentResponse getSignDocumentResponse(Long signRequestId, SignatureValueAsString signatureValue, AbstractSignatureForm abstractSignatureForm, String userEppn, String authUserEppn) throws EsupSignatureException {
		SignRequest signRequest = signRequestService.getById(signRequestId);
		SignDocumentResponse signedDocumentResponse;
		abstractSignatureForm.setBase64SignatureValue(signatureValue.getSignatureValue());
		try {
			Document signedFile = nexuSign(signRequest, userEppn, abstractSignatureForm);
			if(signedFile != null) {
				signedDocumentResponse = new SignDocumentResponse();
				signedDocumentResponse.setUrlToDownload("download");
				signRequestService.updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
				signRequestService.applyEndOfSignRules(signRequest, userEppn, authUserEppn, SignType.nexuSign, "");
				return signedDocumentResponse;
			}
		} catch (IOException e) {
			throw new EsupSignatureException("unable to sign" , e);
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
			if(signType.equals(SignType.pdfImageStamp)) {
				check = false;
			}
		}
		return check;
	}

}
