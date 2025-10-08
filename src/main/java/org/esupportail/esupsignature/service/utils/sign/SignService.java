package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.asic.cades.ASiCWithCAdESSignatureParameters;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
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
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.DssUtilsService;
import org.esupportail.esupsignature.dss.config.DSSProperties;
import org.esupportail.esupsignature.dss.model.*;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@EnableConfigurationProperties(SignProperties.class)
public class SignService {

	private static final Logger logger = LoggerFactory.getLogger(SignService.class);

	private final SignProperties signProperties;
	private final PAdESService padesService;
	private final ASiCWithCAdESService asicWithCAdESService;
	private final ASiCWithXAdESService asicWithXAdESService;
	private final DocumentService documentService;
	private final FileService fileService;
	private final PdfService pdfService;
	private final UserService userService;
	private final UserKeystoreService userKeystoreService;
	private final CertificatService certificatService;
	private final ValidationService validationService;
	private final DSSProperties dssProperties;
	private final GlobalProperties globalProperties;
	private final OpenXPKICertificatGenerationService openXPKICertificatGenerationService;
	private final DssUtilsService dssUtilsService;
    private final SignRequestRepository signRequestRepository;

    public SignService(@Autowired(required = false) OpenXPKICertificatGenerationService openXPKICertificatGenerationService, SignProperties signProperties, PAdESService padesService, ASiCWithCAdESService asicWithCAdESService, ASiCWithXAdESService asicWithXAdESService, DocumentService documentService, FileService fileService, PdfService pdfService, UserService userService, UserKeystoreService userKeystoreService, CertificatService certificatService, ValidationService validationService, DSSProperties dssProperties, GlobalProperties globalProperties, DssUtilsService dssUtilsService, SignRequestRepository signRequestRepository) {
		this.openXPKICertificatGenerationService = openXPKICertificatGenerationService;
		this.signProperties = signProperties;
        this.padesService = padesService;
        this.asicWithCAdESService = asicWithCAdESService;
        this.asicWithXAdESService = asicWithXAdESService;
        this.documentService = documentService;
        this.fileService = fileService;
        this.pdfService = pdfService;
        this.userService = userService;
        this.userKeystoreService = userKeystoreService;
        this.certificatService = certificatService;
        this.validationService = validationService;
        this.dssProperties = dssProperties;
        this.globalProperties = globalProperties;
        this.dssUtilsService = dssUtilsService;
        this.signRequestRepository = signRequestRepository;
    }

    @Transactional
    public Document sign(List<Document> toSignDocuments, SignRequest signRequest, User signerUser, String password, String signWith, SignRequestParams lastSignRequestParams) throws IOException {
        SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(toSignDocuments, signRequest, true);
        return certSign(signatureDocumentForm, signRequest, signerUser.getEppn(), password, SignWith.valueOf(signWith), lastSignRequestParams);
    }

    /**
     * Scelle une demande de signature en utilisant un certificat de sceau.
     * Cette méthode récupère les documents à signer, génère le formulaire de signature,
     * utilise le service de signature pour ajouter une signature certifiée, puis
     * met à jour la liste des documents signés en supprimant les dernières entrées si nécessaire
     * et en ajoutant le document nouvellement signé.
     *
     * @param signRequestId L'identifiant unique de la demande de signature à sceller.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors du processus de scellage.
     */
    @Transactional
    public void seal(Long signRequestId) throws IOException {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
        SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(getToSignDocuments(signRequestId), signRequest, true);
        Document document = certSign(signatureDocumentForm, signRequest, "system", "", SignWith.sealCert, null);
        if(signRequest.getSignedDocuments().size() > 1) {
            signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 1);
        }
        if(signRequest.getSignedDocuments().size() > 1) {
            signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 2);
        }
        signRequest.getSignedDocuments().add(document);
    }

    /**
     * Récupère la liste des documents à signer ou signés associés à une demande de signature.
     * Si des documents signés existent, retourne le dernier document signé.
     * Sinon, retourne tous les documents originaux de la demande.
     *
     * @param signRequestId l'identifiant de la demande de signature
     * @return une liste des documents à signer ou déjà signés
     */
    @Transactional
    public List<Document> getToSignDocuments(Long signRequestId) {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
        List<Document> documents = new ArrayList<>();
        if(signRequest.getSignedDocuments() != null && !signRequest.getSignedDocuments().isEmpty()) {
            documents.add(signRequest.getLastSignedDocument());
        } else {
            documents.addAll(signRequest.getOriginalDocuments());
        }
        return documents;
    }

	@Transactional
	public Document certSign(AbstractSignatureForm signatureDocumentForm, SignRequest signRequest, String userEppn, String password, SignWith signWith, SignRequestParams signRequestParams) throws EsupSignatureRuntimeException {
		User user = userService.getByEppn(userEppn);
		logger.info("start certSign for signRequest : " + signRequest.getId());
		SignatureForm signatureForm;
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
			} else if (signWith.equals(SignWith.sealCert) &&
                    (userService.getRoles(userEppn).contains("ROLE_SEAL")) || userEppn.equals("system") || (user.getUserType().equals(UserType.external) && globalProperties.getSealForExternals()) || (!user.getUserType().equals(UserType.external) && globalProperties.getSealAuthorizedForSignedFiles())) {
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
			signatureForm = signatureDocumentForm.getSignatureForm();
			signatureDocumentForm.setEncryptionAlgorithm(certificateToken.getSignatureAlgorithm().getEncryptionAlgorithm());
			signatureDocumentForm.setCertificate(certificateToken.getEncoded());
			List<byte[]> base64CertificateChain = new ArrayList<>();
			for (CertificateToken token : certificateTokenChain) {
				base64CertificateChain.add(token.getEncoded());
			}
			signatureDocumentForm.setCertificateChain(base64CertificateChain);
			AbstractSignatureParameters<?> parameters;
			if(signatureForm.equals(SignatureForm.CAdES) || signatureForm.equals(SignatureForm.XAdES)) {
                parameters = getSignatureParameters(signatureForm);
				fillCommonsParameters(parameters, signatureDocumentForm);
            } else {
				if((abstractKeyStoreTokenConnection instanceof OpenSCSignatureToken
                        || abstractKeyStoreTokenConnection instanceof Pkcs11SignatureToken
                        || abstractKeyStoreTokenConnection instanceof Pkcs12SignatureToken)
                        && signRequestParams != null && !user.getEppn().equals("system")) {
					parameters = fillVisibleParameters((SignatureDocumentForm) signatureDocumentForm, signRequestParams , user, signatureDocumentForm.getSigningDate());
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

	public PAdESSignatureParameters fillVisibleParameters(SignatureDocumentForm signatureDocumentForm, SignRequestParams signRequestParams, User user, Date date) throws IOException {
		InputStream toSignFile = new ByteArrayInputStream(signatureDocumentForm.getDocumentToSign().getBytes());
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
			InputStream signImage = fileService.addTextToImage(inputStream, signRequestParams, SignType.signature, user, date, userService.getRoles(user.getEppn()).contains("ROLE_OTP"));
			if(BooleanUtils.isTrue(signRequestParams.getAddWatermark())) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				fileService.addImageWatermark(new ClassPathResource("/static/images/watermark.png").getInputStream(), signImage, outputStream, signRequestParams.getExtraOnTop());
				signImage = new ByteArrayInputStream(outputStream.toByteArray());
			}
			SignatureFieldParameters signatureFieldParameters = imageParameters.getFieldParameters();
			imageParameters.getFieldParameters().setRotation(VisualSignatureRotation.AUTOMATIC);
			PdfParameters pdfParameters = pdfService.getPdfParameters(toSignFile, signRequestParams.getSignPageNumber());
			int widthAdjusted = Math.round(signRequestParams.getSignWidth() * globalProperties.getFixFactor());
			int heightAdjusted = Math.round(signRequestParams.getSignHeight() * globalProperties.getFixFactor());
			PDSignatureField pdSignatureField = pdfService.getSignatureField(signatureDocumentForm.getDocumentToSign(), signRequestParams);
			if(pdSignatureField != null && StringUtils.hasText(signRequestParams.getPdSignatureFieldName())) {
				signImage = fileService.resizeImage(signImage, pdSignatureField.getWidgets().get(0).getRectangle().getWidth() * 3, pdSignatureField.getWidgets().get(0).getRectangle().getHeight() * 3);
				signatureFieldParameters.setFieldId(signRequestParams.getPdSignatureFieldName());
			} else {
				signatureFieldParameters.setPage(signRequestParams.getSignPageNumber());
				if (pdfParameters.getRotation() == 0) {
					signatureFieldParameters.setWidth(widthAdjusted);
					signatureFieldParameters.setHeight(heightAdjusted);
					signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() * globalProperties.getFixFactor()));
				} else {
					signatureFieldParameters.setWidth(heightAdjusted);
					signatureFieldParameters.setHeight(widthAdjusted);
					signatureFieldParameters.setOriginX(Math.round(signRequestParams.getxPos() - 50 * globalProperties.getFixFactor()));
				}
			}
			int yPos = Math.round(signRequestParams.getyPos() * globalProperties.getFixFactor());
			if (yPos < 0) yPos = 0;
			signatureFieldParameters.setOriginY(yPos);

			BufferedImage bufferedSignImage = ImageIO.read(signImage);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(bufferedSignImage, "png", os);
			fileDocumentImage = new InMemoryDocument(new ByteArrayInputStream(os.toByteArray()), "sign.png");
			fileDocumentImage.setMimeType(MimeTypeEnum.PNG);
			imageParameters.setImage(fileDocumentImage);
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
        parameters.setSignaturePackaging(signProperties.getSignaturePackaging());
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureDocumentForm signatureDocumentForm, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) throws IOException {
        logger.debug("Start signDocument with single documents");
        DocumentSignatureService service = getDocumentSignatureService(signatureDocumentForm.getSignatureForm());
        DSSDocument toSignDocument = new InMemoryDocument(new ByteArrayInputStream(signatureDocumentForm.getDocumentToSign().getBytes()), "detached-file");
		ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
        logger.debug("End signDocument with single documents");
		return service.signDocument(toSignDocument, parameters, signatureValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public DSSDocument certSignDocument(SignatureMultipleDocumentsForm form, AbstractSignatureParameters parameters, SignatureTokenConnection signingToken) {
		logger.debug("Start signDocument with multiple documents");
		MultipleDocumentsSignatureService service = getMultipleDocumentSignatureService(form.getSignatureForm());
		List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(form.getDocumentsToSign());
		ToBeSigned dataToSign = service.getDataToSign(toSignDocuments, parameters);
		SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), signingToken.getKeys().get(0));
		DSSDocument signedDocument = service.signDocument(toSignDocuments, parameters, signatureValue);
		logger.debug("End signDocument with multiple documents");
		return signedDocument;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TimestampToken getContentTimestamp(SignatureMultipleDocumentsForm form, AbstractSignatureParameters<?> parameters) {
		logger.info("Start getContentTimestamp with multiple documents");
		MultipleDocumentsSignatureService service = getMultipleDocumentSignatureService(form.getSignatureForm());
		TimestampToken contentTimestamp = service.getContentTimestamp(dssUtilsService.toDSSDocuments(form.getDocumentsToSign()), parameters);
		logger.info("End getContentTimestamp with  multiple documents");
		return contentTimestamp;
	}

    public DocumentSignatureService<?, ?> getDocumentSignatureService(SignatureForm signatureForm) {
        DocumentSignatureService<?, ?> service = null;
        switch (signatureForm) {
            case CAdES:
                service = asicWithCAdESService;
                break;
            case XAdES:
                service = asicWithXAdESService;
                break;
            case PAdES:
                service = padesService;
                break;
            default:
                logger.error("Unknow signature form : " + signatureForm);
        }
        return service;
    }

    public AbstractSignatureParameters<?> getSignatureParameters(SignatureForm signatureForm) {
		AbstractSignatureParameters<?> parameters = null;
        switch (signatureForm) {
            case CAdES:
                ASiCWithCAdESSignatureParameters asicCadesParams = new ASiCWithCAdESSignatureParameters();
                asicCadesParams.aSiC().setContainerType(signProperties.getContainerType());
                asicCadesParams.setSignatureLevel(signProperties.getCadesSignatureLevel());
                parameters = asicCadesParams;
                break;
            case XAdES:
                ASiCWithXAdESSignatureParameters asicXadesParams = new ASiCWithXAdESSignatureParameters();
                asicXadesParams.aSiC().setContainerType(signProperties.getContainerType());
                asicXadesParams.setSignatureLevel(signProperties.getXadesSignatureLevel());
                parameters = asicXadesParams;
                break;
            default:
                logger.error("Unknow signature form for ASiC container: " + signatureForm);
        }
        parameters.setSignaturePackaging(signProperties.getSignaturePackaging());
        return parameters;
	}

    public MultipleDocumentsSignatureService<?, ?> getMultipleDocumentSignatureService(SignatureForm signatureForm) {
        return (MultipleDocumentsSignatureService<?, ?>) getDocumentSignatureService(signatureForm);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Transactional
    public DSSDocument signDocument(Long signRequestId, String userEppn, SignatureDocumentForm signatureDocumentForm) throws IOException, EsupSignatureException {
        SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
        logger.info("Start signDocument with one document");
        DocumentSignatureService service = getDocumentSignatureService(signatureDocumentForm.getSignatureForm());
        DSSDocument toSignDssDocument = dssUtilsService.toDSSDocument(signatureDocumentForm.getDocumentToSign());
        SignatureAlgorithm sigAlgorithm = SignatureAlgorithm.getAlgorithm(signatureDocumentForm.getEncryptionAlgorithm(), signatureDocumentForm.getDigestAlgorithm());
        SignatureValue signatureValue = new SignatureValue(sigAlgorithm, signatureDocumentForm.getSignatureValue());
        AbstractSignatureParameters parameters = getSignatureParameters(signRequest, userEppn, signatureDocumentForm);
        validationService.checkRevocation(signatureDocumentForm, DSSUtils.loadCertificate(signatureDocumentForm.getCertificate()), parameters);
        try {
            logger.info("End signDocument with one document");
            DSSDocument signedFile = service.signDocument(toSignDssDocument, parameters, signatureValue);
            return signedFile;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            throw new EsupSignatureException(e.getMessage());
        }
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public DSSDocument extend(ExtensionForm extensionForm) {
        SignatureForm signatureForm = extensionForm.getSignatureForm();
        DSSDocument signedDocument = extensionForm.getSignedFile();
        List<DSSDocument> originalDocuments = extensionForm.getOriginalFiles();
        DocumentSignatureService service = getDocumentSignatureService(signatureForm);
        AbstractSignatureParameters parameters = getSignatureParameters(signatureForm);
        parameters.setSignatureLevel(extensionForm.getSignatureLevel());
        if (Utils.isCollectionNotEmpty(originalDocuments)) {
            parameters.setDetachedContents(originalDocuments);
        }
        DSSDocument extendedDoc = service.extendDocument(signedDocument, parameters);
        logger.info("End extend with one document");
        return extendedDoc;
    }


    @Transactional
    public AbstractSignatureParameters<?> getSignatureParameters(SignRequest signRequest, String userEppn, AbstractSignatureForm abstractSignatureForm) throws IOException {
        User user = userService.getByEppn(userEppn);
        AbstractSignatureParameters<?> parameters = null;
        if(abstractSignatureForm instanceof SignatureMultipleDocumentsForm signatureMultipleDocumentsForm) {
            List<DSSDocument> toSignDocuments = dssUtilsService.toDSSDocuments(signatureMultipleDocumentsForm.getDocumentsToSign());
            parameters = getParameters(signatureMultipleDocumentsForm, toSignDocuments);
        } else if(abstractSignatureForm instanceof SignatureDocumentForm signatureDocumentForm) {
            SignRequestParams lastSignRequestParams = findLastSignRequestParams(signRequest);
            if(abstractSignatureForm.getSignatureForm().equals(SignatureForm.PAdES)) {
                if(lastSignRequestParams != null) {
                    parameters = fillVisibleParameters(signatureDocumentForm, lastSignRequestParams, user, abstractSignatureForm.getSigningDate());
                } else {
                    parameters = fillVisibleParameters(signatureDocumentForm, user);
                }
            } else {
                parameters = getParameters(signatureDocumentForm);
                parameters.setDetachedContents(Collections.singletonList(dssUtilsService.toDSSDocument(signatureDocumentForm.getDocumentToSign())));
            }
        }
        return parameters;
    }

    /**
     * Recherche et retourne le dernier objet SignRequestParams dans la liste fournie
     * qui satisfait les conditions : le numéro d'image de signature est supérieur ou
     * égal à zéro et la partie textuelle est vide ou non définie.
     *
     * @param signRequest signRequest à analyser
     * @return le dernier objet SignRequestParams répondant aux critères ou null si aucun n'est trouvé
     */
    public SignRequestParams findLastSignRequestParams(SignRequest signRequest) {
        SignRequestParams lastSignRequestParams = null;
        for (SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
            if (signRequestParams.getSignImageNumber() >= 0 && !StringUtils.hasText(signRequestParams.getTextPart())) {
                if(lastSignRequestParams == null || signRequestParams.getSignPageNumber() >= lastSignRequestParams.getSignPageNumber()) {
                    lastSignRequestParams = signRequestParams;
                }
            }
        }
        return lastSignRequestParams;
    }

    public AbstractSignatureParameters<?> getParameters(SignatureMultipleDocumentsForm signatureMultipleDocumentsForm, List<DSSDocument> documentsToSign) {
        AbstractSignatureParameters<?> parameters = getSignatureParameters(signatureMultipleDocumentsForm.getSignatureForm());
        parameters.getDetachedContents().addAll(documentsToSign);
        fillCommonsParameters(parameters, signatureMultipleDocumentsForm);
        return parameters;
    }

    public AbstractSignatureParameters<?> getParameters(SignatureDocumentForm signatureDocumentForm) {
        AbstractSignatureParameters<?> parameters = getSignatureParameters(signatureDocumentForm.getSignatureForm());
        parameters.setSignaturePackaging(signatureDocumentForm.getSignaturePackaging());
        fillCommonsParameters(parameters, signatureDocumentForm);
        return parameters;
    }

    /**
     * Génère et retourne un formulaire de signature DSS pour des documents donnés, en prenant en compte
     * les paramètres de signature et d'inclusion des documents.
     *
     * @param documents une liste d'objets Document représentant les fichiers à signer.
     * @param signRequest un objet SignRequest contenant les informations de la demande de signature.
     * @param includeDocuments un booléen indiquant si les documents doivent être inclus dans le processus de signature.
     * @return une instance de SignatureDocumentForm correspondant au formulaire de signature généré.
     * @throws IOException si une erreur d'entrée/sortie se produit lors du traitement des fichiers.
     * @throws EsupSignatureRuntimeException si une erreur spécifique à EsupSignature survient.
     */
    @Transactional
    public SignatureDocumentForm getAbstractSignatureForm(List<Document> documents, SignRequest signRequest, boolean includeDocuments) throws IOException, EsupSignatureRuntimeException {
        SignatureForm signatureForm;
        AbstractSignatureForm abstractSignatureForm;
        if(documents.size() > 1) {
            signatureForm = signProperties.getDefaultSignatureForm();
            abstractSignatureForm = new SignatureMultipleDocumentsForm();
            if(includeDocuments) {
                List<DssMultipartFile> multipartFiles = new ArrayList<>();
                for (Document toSignFile : documents) {
                    multipartFiles.add(toSignFile.getMultipartFile());
                }
                ((SignatureMultipleDocumentsForm) abstractSignatureForm).setDocumentsToSign(multipartFiles);
            }
            abstractSignatureForm.setContainerType(signProperties.getContainerType());
        } else {
            InputStream inputStream;
            byte[] bytes;
            Document toSignFile = documents.get(0);
            if(toSignFile.getContentType().equals("application/pdf")) {
                signatureForm = SignatureForm.PAdES;
                inputStream = toSignFile.getTransientInputStream();
                bytes = inputStream.readAllBytes();
                if(!isSigned(signRequest, null) && !pdfService.isPdfAComplient(bytes)) {
                    bytes = pdfService.convertToPDFA(pdfService.writeMetadatas(bytes, toSignFile.getFileName(), signRequest, new ArrayList<>()));
                }
            } else {
                signatureForm = signProperties.getDefaultSignatureForm();
                bytes = toSignFile.getTransientInputStream().readAllBytes();
            }
            abstractSignatureForm = new SignatureDocumentForm();
            ((SignatureDocumentForm) abstractSignatureForm).setDocumentToSign(new DssMultipartFile(toSignFile.getFileName(), toSignFile.getFileName(), toSignFile.getContentType(), bytes));
            if(!signatureForm.equals(SignatureForm.PAdES)) {
                abstractSignatureForm.setContainerType(signProperties.getContainerType());
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
        assert abstractSignatureForm instanceof SignatureDocumentForm;
        ((SignatureDocumentForm) abstractSignatureForm).setSignaturePackaging(signProperties.getSignaturePackaging());
        return (SignatureDocumentForm) abstractSignatureForm;
    }

    /**
     * Vérifie si une demande de signature est signée en fonction des rapports et
     * des documents associés.
     *
     * @param signRequest l'objet contenant les informations sur la demande de signature
     * @param reports l'objet contenant les rapports connexes, peut être null
     * @return true si la demande de signature est signée, false sinon
     */
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

    /**
     * Validation DSS d'une requête de signature en fonction de son identifiant.
     *
     * @param signRequestId L'identifiant de la requête de signature à valider.
     * @return Un objet Reports contenant les informations de validation si des documents à signer existent,
     *         sinon retourne null.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la lecture des documents.
     */
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
}
