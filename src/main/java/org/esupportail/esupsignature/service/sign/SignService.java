package org.esupportail.esupsignature.service.sign;

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
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.pdf.PdfParameters;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@Service
public class SignService {

	private static final Logger logger = LoggerFactory.getLogger(SignService.class);

	@Resource
	private SignConfig signConfig;

	@Autowired
	private ObjectProvider<CAdESService> cadesService;

	@Autowired
	private ObjectProvider<PAdESService> padesService;

	@Autowired
	private ObjectProvider<XAdESService> xadesService;

	@Autowired
	private ObjectProvider<ASiCWithCAdESService> asicWithCAdESService;

	@Autowired
	private ObjectProvider<ASiCWithXAdESService> asicWithXAdESService;
	
	@Resource
	private FileService fileService;

	@Resource
	private PdfService pdfService;

	@Resource
	private SignRequestService signRequestService;

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

		DSSDocument extendedDoc = (DSSDocument) service.extendDocument(signedDocument, parameters);
		logger.info("End extend with one document");
		return extendedDoc;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ToBeSigned getDataToSign(SignatureDocumentForm form, AbstractSignatureParameters parameters) {
		logger.info("Start getDataToSign with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		ToBeSigned toBeSigned = null;
		try {
			DSSDocument toSignDocument = DssUtils.toDSSDocument(form.getDocumentToSign());
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

		AbstractSignatureParameters parameters = fillParameters(form);

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
		AbstractSignatureParameters parameters = fillParameters(form);
		DSSDocument toSignDocument = DssUtils.toDSSDocument(form.getDocumentToSign());

		TimestampToken contentTimestamp = service.getContentTimestamp(toSignDocument, parameters);

		logger.info("End getContentTimestamp with one document");
		return contentTimestamp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimestampToken getContentTimestamp(SignatureMultipleDocumentsForm form) {
		logger.info("Start getContentTimestamp with multiple documents");

		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		AbstractSignatureParameters parameters = fillParameters(form);

		TimestampToken contentTimestamp = service.getContentTimestamp(DssUtils.toDSSDocuments(form.getDocumentsToSign()), parameters);

		logger.info("End getContentTimestamp with  multiple documents");
		return contentTimestamp;
	}

	public AbstractSignatureParameters fillParameters(SignatureMultipleDocumentsForm form) {
		AbstractSignatureParameters finalParameters = getASiCSignatureParameters(form.getContainerType(), form.getSignatureForm());

		fillParameters(finalParameters, form);

		return finalParameters;
	}

	public AbstractSignatureParameters fillParameters(SignatureDocumentForm form) {
		AbstractSignatureParameters parameters = getSignatureParameters(form.getContainerType(), form.getSignatureForm());
		parameters.setSignaturePackaging(form.getSignaturePackaging());

		fillParameters(parameters, form);

		return parameters;
	}

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm form, SignRequestParams signRequestParams, MultipartFile toSignFile, User user) throws IOException {
		PAdESSignatureParameters pAdESSignatureParameters = new PAdESSignatureParameters();
		SignatureImageParameters imageParameters = new SignatureImageParameters();
		InMemoryDocument fileDocumentImage;
		InputStream signImage;
		DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss ", Locale.FRENCH);
		List<String> addText = pdfService.getSignatureStrings(user, SignType.certSign, new Date(), dateFormat);
		signImage = fileService.addTextToImage(user.getSignImages().get(signRequestParams.getSignImageNumber()).getInputStream(), addText, false);
		BufferedImage bufferedSignImage = ImageIO.read(signImage);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		ImageIO.write(bufferedSignImage, "png", os);
		fileDocumentImage = new InMemoryDocument(new ByteArrayInputStream(os.toByteArray()), "sign.png");
		fileDocumentImage.setMimeType(MimeType.PNG);
		imageParameters.setImage(fileDocumentImage);
		imageParameters.setPage(signRequestParams.getSignPageNumber());
		imageParameters.setRotation(VisualSignatureRotation.AUTOMATIC);
		PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFile.getInputStream());
		if(signRequestParams.isAddExtra()) {
			signRequestParams.setSignWidth(signRequestParams.getSignWidth() + 200);
		}
		int widthAdjusted = Math.round((float) (bufferedSignImage.getWidth() / 3 * 0.75));
		int heightAdjusted = Math.round((float) (bufferedSignImage.getHeight() / 3 * 0.75));

		if(pdfParameters.getRotation() == 0) {
			imageParameters.setWidth(widthAdjusted);
			imageParameters.setHeight(heightAdjusted);
			imageParameters.setxAxis(signRequestParams.getxPos());
		} else {
			imageParameters.setWidth(heightAdjusted);
			imageParameters.setHeight(widthAdjusted);
			imageParameters.setxAxis(signRequestParams.getxPos() - 50);
		}
		int yPos = Math.round(signRequestParams.getyPos() - ((heightAdjusted - signRequestParams.getSignHeight())) / 0.75f);
		imageParameters.setyAxis(yPos);
		imageParameters.setDpi(300);
		imageParameters.setAlignmentHorizontal(VisualSignatureAlignmentHorizontal.LEFT);
		imageParameters.setAlignmentVertical(VisualSignatureAlignmentVertical.TOP);
		pAdESSignatureParameters.setImageParameters(imageParameters);
		//signature size 32767 is max for PDF/A-2B
		pAdESSignatureParameters.setContentSize(32767);
		pAdESSignatureParameters.setSignaturePackaging(form.getSignaturePackaging());

		fillParameters(pAdESSignatureParameters, form);
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
	
	private void fillParameters(AbstractSignatureParameters parameters, AbstractSignatureForm form) {
		parameters.setSignatureLevel(form.getSignatureLevel());
		parameters.setDigestAlgorithm(form.getDigestAlgorithm());
		//parameters.setEncryptionAlgorithm(form.getEncryptionAlgorithm()); retrieved from certificate
		//parameters.bLevel().setSigningDate(form.getSigningDate());
		parameters.setSignWithExpiredCertificate(form.isSignWithExpiredCertificate());

		if (form.getContentTimestamp() != null) {
			parameters.setContentTimestamps(Arrays.asList(DssUtils.toTimestampToken(form.getContentTimestamp())));
		}
		CertificateToken signingCertificate = DSSUtils.loadCertificateFromBase64EncodedString(form.getBase64Certificate());
		parameters.setSigningCertificate(signingCertificate);

		List<String> base64CertificateChain = form.getBase64CertificateChain();
		if (Utils.isCollectionNotEmpty(base64CertificateChain)) {
			List<CertificateToken> certificateChain = new LinkedList<CertificateToken>();
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
				//multipartFiles.add(new MultipartInputStreamFileResource(toSignFile.getInputStream(), toSignFile.getFileName() + ".pdf").);
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
					inputStream = pdfService.convertGS(pdfService.writeMetadatas(inputStream, toSignFile.getFileName(), signRequest));
				}
			} else {
				signatureForm = signConfig.getSignProperties().getDefaultSignatureForm();
				inputStream = toSignFile.getInputStream();
			}
			SignatureDocumentForm signatureDocumentForm = new SignatureDocumentForm();
			signatureDocumentForm.setDocumentToSign(fileService.toMultipartFile(inputStream, documents.get(0).getFileName(), documents.get(0).getContentType()));
			if(!signatureForm.equals(SignatureForm.PAdES)) {	
				signatureDocumentForm.setContainerType(signConfig.getSignProperties().getContainerType());
			}
			abstractSignatureForm = signatureDocumentForm;
		}
		abstractSignatureForm.setSignWithExpiredCertificate(false);
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
		fillParameters(parameters, signatureDocumentForm);
		DSSDocument toSignDocument = DssUtils.toDSSDocument(signatureDocumentForm.getDocumentToSign());
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
	public DSSDocument nexuSignDocument(SignatureDocumentForm form, AbstractSignatureParameters parameters) {
		logger.info("Start signDocument with one document");
		DocumentSignatureService service = getSignatureService(form.getContainerType(), form.getSignatureForm());
		DSSDocument toSignDocument = DssUtils.toDSSDocument(form.getDocumentToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, Utils.fromBase64(form.getBase64SignatureValue()));
		DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
		logger.info("End signDocument with one document");
		return signedDocument;
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument signDocument(SignatureMultipleDocumentsForm form) {
		logger.info("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = getASiCSignatureService(form.getSignatureForm());
		AbstractSignatureParameters parameters = fillParameters(form);
		List<DSSDocument> toSignDocuments = DssUtils.toDSSDocuments(form.getDocumentsToSign());
		SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(form.getEncryptionAlgorithm(), form.getDigestAlgorithm());
		SignatureValue signatureValue = new SignatureValue(sigAlgorithm, Utils.fromBase64(form.getBase64SignatureValue()));
		DSSDocument signedDocument = (DSSDocument) service.signDocument(toSignDocuments, parameters, signatureValue);
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
				service = cadesService.getIfAvailable();
				break;
			case PAdES:
				service = padesService.getIfAvailable();
				break;
			case XAdES:
				service = xadesService.getIfAvailable();
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
			service = asicWithCAdESService.getIfAvailable();
			break;
		case XAdES:
			service = asicWithXAdESService.getIfAvailable();
			break;
		default:
			logger.error("Unknow signature form : " + signatureForm);
		}
		return service;
	}

	private AbstractSignatureParameters getASiCSignatureParameters(ASiCContainerType containerType, SignatureForm signatureForm) {
		AbstractSignatureParameters parameters = null;
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

	public AbstractSignatureForm getAbstractSignatureForm(SignRequest signRequest) throws IOException, EsupSignatureException {
		return getSignatureDocumentForm(signRequest.getToSignDocuments(), signRequest, true);
	}

	public ToBeSigned getToBeSigned(SignRequest signRequest, User user, AbstractSignatureForm signatureDocumentForm, AbstractSignatureParameters parameters) throws IOException {
		ToBeSigned dataToSign;
		if(signatureDocumentForm.getClass().equals(SignatureMultipleDocumentsForm.class)) {
			parameters = fillParameters((SignatureMultipleDocumentsForm) signatureDocumentForm);
		} else {
			if(signatureDocumentForm.getSignatureForm().equals(SignatureForm.PAdES)) {
				SignatureDocumentForm documentForm = (SignatureDocumentForm) signatureDocumentForm;
				parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequest.getSignRequestParams().get(signRequest.getSignedDocuments().size()), documentForm.getDocumentToSign(), user);
			} else {
				parameters = fillParameters((SignatureDocumentForm) signatureDocumentForm);
			}
		}
		dataToSign = getDataToSign((SignatureDocumentForm) signatureDocumentForm, parameters);
		return dataToSign;
	}

	public SignDocumentResponse getSignDocumentResponse(User user, SignatureValueAsString signatureValue, AbstractSignatureForm signatureDocumentForm, SignRequest signRequest, AbstractSignatureParameters parameters) throws EsupSignatureException {
		SignDocumentResponse signedDocumentResponse;
		signatureDocumentForm.setBase64SignatureValue(signatureValue.getSignatureValue());
		try {
			Document signedFile = signRequestService.nexuSign(signRequest, user, signatureDocumentForm, parameters);
			if(signedFile != null) {
				signedDocumentResponse = new SignDocumentResponse();
				signedDocumentResponse.setUrlToDownload("download");
				signRequestService.updateStatus(signRequest, SignRequestStatus.signed, "Signature", "SUCCESS");
				signRequestService.applyEndOfSignRules(signRequest, user);
			}
		} catch (IOException e) {
			throw new EsupSignatureException("unable to sign" , e);
		}
		return null;
	}

}
