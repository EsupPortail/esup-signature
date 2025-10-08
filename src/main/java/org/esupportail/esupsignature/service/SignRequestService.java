package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import eu.europa.esig.dss.enumerations.SignatureForm;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sign.SignProperties;
import org.esupportail.esupsignature.dss.model.AbstractSignatureForm;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dss.model.SignatureDocumentForm;
import org.esupportail.esupsignature.dss.model.SignatureMultipleDocumentsForm;
import org.esupportail.esupsignature.dss.service.FOPService;
import org.esupportail.esupsignature.dto.js.JsMessage;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.RecipientsActionsDto;
import org.esupportail.esupsignature.dto.json.SignRequestStepsDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service dédié à la gestion des demandes de signature (SignRequest).
 * Fournit des méthodes permettant de récupérer, analyser, et modifier les informations
 * liées aux demandes de signature, ainsi que de vérifier les droits des utilisateurs sur celles-ci.
 *
 * @author David Lemaignent
 */
@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignRequestService {

	private static final Logger logger = LoggerFactory.getLogger(SignRequestService.class);

	private final GlobalProperties globalProperties;
	private final SignProperties signProperties;
	private final TargetService targetService;
	private final WebUtilsService webUtilsService;
	private final SignRequestRepository signRequestRepository;
	private final ActionService actionService;
	private final PdfService pdfService;
	private final DocumentService documentService;
	private final CustomMetricsService customMetricsService;
	private final SignService signService;
	private final UserService userService;
	private final DataService dataService;
	private final CommentService commentService;
	private final MailService mailService;
	private final AuditTrailService auditTrailService;
	private final UserShareService userShareService;
	private final RecipientService recipientService;
	private final FsAccessFactoryService fsAccessFactoryService;
	private final WsAccessTokenRepository wsAccessTokenRepository;
	private final FileService fileService;
	private final PreFillService preFillService;
	private final LogService logService;
	private final SignRequestParamsService signRequestParamsService;
	private final ValidationService validationService;
	private final FOPService fopService;
	private final ObjectMapper objectMapper;
	private final SignBookRepository signBookRepository;
	private final NexuSignatureRepository nexuSignatureRepository;
    private final OtpService otpService;

    public SignRequestService(GlobalProperties globalProperties, SignProperties signProperties, TargetService targetService, WebUtilsService webUtilsService, SignRequestRepository signRequestRepository, ActionService actionService, PdfService pdfService, DocumentService documentService, CustomMetricsService customMetricsService, SignService signService, SignTypeService signTypeService, UserService userService, DataService dataService, CommentService commentService, MailService mailService, AuditTrailService auditTrailService, UserShareService userShareService, RecipientService recipientService, FsAccessFactoryService fsAccessFactoryService, WsAccessTokenRepository wsAccessTokenRepository, FileService fileService, PreFillService preFillService, LogService logService, SignRequestParamsService signRequestParamsService, ValidationService validationService, FOPService fopService, ObjectMapper objectMapper, SignBookRepository signBookRepository, NexuSignatureRepository nexuSignatureRepository, OtpService otpService) {
        this.globalProperties = globalProperties;
        this.signProperties = signProperties;
        this.targetService = targetService;
        this.webUtilsService = webUtilsService;
        this.signRequestRepository = signRequestRepository;
        this.actionService = actionService;
        this.pdfService = pdfService;
        this.documentService = documentService;
        this.customMetricsService = customMetricsService;
        this.signService = signService;
        this.userService = userService;
        this.dataService = dataService;
        this.commentService = commentService;
        this.mailService = mailService;
        this.auditTrailService = auditTrailService;
        this.userShareService = userShareService;
        this.recipientService = recipientService;
        this.fsAccessFactoryService = fsAccessFactoryService;
        this.wsAccessTokenRepository = wsAccessTokenRepository;
        this.fileService = fileService;
        this.preFillService = preFillService;
        this.logService = logService;
        this.signRequestParamsService = signRequestParamsService;
        this.validationService = validationService;
        this.fopService = fopService;
        this.objectMapper = objectMapper;
        this.signBookRepository = signBookRepository;
		this.nexuSignatureRepository = nexuSignatureRepository;
        this.otpService = otpService;
    }

    @PostConstruct
	public void initSignrequestMetrics() {
		customMetricsService.registerValue("esup-signature.signrequests", "new");
		customMetricsService.registerValue("esup-signature.signrequests", "signed");
	}

	/**
     * Récupère une instance de SignRequest en fonction de son identifiant unique.
     *
     * @param id l'identifiant unique de la demande de signature à récupérer
     * @return l'objet SignRequest correspondant à l'identifiant spécifié,
     *         ou null si aucun objet n'est trouvé
     */
    public SignRequest getById(long id) {
		Optional<SignRequest> signRequest = signRequestRepository.findById(id);
		if(signRequest.isPresent()) {
			Data data = dataService.getBySignBook(signRequest.get().getParentSignBook());
			if (data != null) {
				signRequest.get().setData(data);
			}
			return signRequest.get();
		}
		return null;
	}

	/**
     * Récupère le statut associé à l'identifiant donné.
     *
     * @param id l'identifiant du SignRequest ou des Logs associés
     * @return une chaîne de caractères représentant le statut du SignRequest si trouvé,
     *         "fully-deleted" si des Logs existent pour cet identifiant mais aucun SignRequest correspondant,
     *         ou null si rien n'est trouvé
     */
    public String getStatus(long id) {
		SignRequest signRequest = getById(id);
		if (signRequest != null) {
			return signRequest.getStatus().name();
		} else {
			List<Log> logs = logService.getBySignRequest(id);
			if (!logs.isEmpty()) {
				return "fully-deleted";
			}
		}
		return null;
	}

	/**
     * Récupère une demande de signature en fonction de son token.
     *
     * @param token le token utilisé pour rechercher une demande de signature
     * @return un objet Optional contenant la demande de signature si elle existe, sinon un Optional vide
     */
    public Optional<SignRequest> getSignRequestByToken(String token) {
		return signRequestRepository.findByToken(token);
	}

	/**
     * Détermine si une requête de signature est éditable par l'utilisateur spécifié.
     *
     * Une requête de signature est considérée comme éditable si elle respecte les critères suivants :
     * 1. Elle n'est pas supprimée, a un statut "pending" (en attente), et :
     *    - L'utilisateur n'est pas de type "externe" ou,
     *    - Le workflow associé permet l'édition pour les utilisateurs externes ou,
     *    - Si aucun workflow n'est défini, les propriétés globales permettent l'édition pour les utilisateurs externes.
     * 2. L'utilisateur est impliqué dans les destinataires, ou a créé la requête, ou est un viewer du dossier de signatures.
     * 3. Alternativement, une requête en statut "draft" (brouillon) est éditable par son créateur.
     *
     * @param id Identifiant unique de la requête de signature à vérifier.
     * @param userEppn Identifiant eppn de l'utilisateur pour lequel vérifier les droits d'édition.
     * @return true si la requête est éditable par l'utilisateur, false sinon.
     */
    @Transactional
	public boolean isEditable(long id, String userEppn) {
		SignRequest signRequest = getById(id);
		User user = userService.getByEppn(userEppn);
		SignBook signBook = signRequest.getParentSignBook();
		if (
			(
				!signRequest.getDeleted() && signRequest.getStatus().equals(SignRequestStatus.pending)
				&&
				(!user.getUserType().equals(UserType.external) || (signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().getExternalCanEdit()) || (signBook.getLiveWorkflow().getWorkflow() == null && globalProperties.getExternalCanEdit()))
				&&
				(
					isUserInRecipients(signRequest, userEppn)
					|| signRequest.getCreateBy().getEppn().equals(userEppn)
					|| signBook.getViewers().contains(user)
				)
			)
			||
			(signRequest.getStatus().equals(SignRequestStatus.draft) && signRequest.getCreateBy().getEppn().equals(user.getEppn()))
		) {
			return true;
		}
		return false;
	}

	/**
     * Vérifie si un utilisateur est présent dans la liste des destinataires d'une demande de signature.
     *
     * @param signRequest l'objet SignRequest représentant la demande de signature
     * @param userEppn l'identifiant EPPN de l'utilisateur à vérifier
     * @return true si l'utilisateur est présent dans la liste des destinataires, sinon false
     */
    public boolean isUserInRecipients(SignRequest signRequest, String userEppn) {
		boolean isInRecipients = false;
		Set<Recipient> recipients = signRequest.getRecipientHasSigned().keySet();
		for(Recipient recipient : recipients) {
			if (recipient.getUser().getEppn().equals(userEppn)) {
				isInRecipients = true;
				break;
			}
		}
		return isInRecipients;
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
		SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
		List<Document> documents = new ArrayList<>();
		if(signRequest.getSignedDocuments() != null && !signRequest.getSignedDocuments().isEmpty()) {
			documents.add(signRequest.getLastSignedDocument());
		} else {
			documents.addAll(signRequest.getOriginalDocuments());
		}
		return documents;
	}

	/**
     * Signe une requête de signature en tenant compte de divers paramètres tels que l'utilisateur,
     * les documents à signer, et les informations de formulaire.
     *
     * @param signRequest L'objet représentant la requête de signature à traiter.
     * @param password Le mot de passe de l'utilisateur nécessaire pour effectuer la signature.
     * @param signWith Le type d'élément utilisé pour effectuer la signature (image, certificat, etc.).
     * @param data Les données associées à la signature, incluant des informations de formulaire.
     * @param formDataMap Une carte représentant les données des champs de formulaire à remplir ou à valider.
     * @param userEppn L'identifiant de l'utilisateur effectuant la signature.
     * @param authUserEppn L'identifiant de l'utilisateur authentifié utilisé pour des cas spécifiques de délégation ou de partage.
     * @param userShareId L'identifiant d'un partage utilisateur si applicable, pour déterminer si une délégation est utilisée.
     * @param comment Un commentaire libre que l'utilisateur peut ajouter à l'action de signature.
     * @return Le statut de l'étape courante après la signature, incluant des informations sur l'état de la progression dans le processus.
     * @throws EsupSignatureRuntimeException En cas d'erreur critique lors de la signature ou de la génération de métadonnées.
     * @throws IOException En cas de problème lors de la lecture ou de l'écriture de contenu lié aux documents.
     */
    @Transactional
	public StepStatus sign(SignRequest signRequest, String password, String signWith, Data data, Map<String, String> formDataMap, String userEppn, String authUserEppn, Long userShareId, String comment) throws EsupSignatureRuntimeException, IOException {
		User user = userService.getByEppn(userEppn);
		if(signRequest.getAuditTrail() == null) {
			signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
		}
		boolean isViewed = signRequest.getViewedBy().contains(user);
		StepStatus stepStatus;
		Date date = new Date();
		List<Log> lastSignLogs = new ArrayList<>();
		User signerUser = user;
		if(userShareId != null) {
			UserShare userShare = userShareService.getById(userShareId);
			if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
				signerUser = userService.getByEppn(authUserEppn);
			}
		}
		List<Document> toSignDocuments = getToSignDocuments(signRequest.getId());
		SignType signType = signRequest.getCurrentSignType();
		byte[] filledInputStream;
		boolean isForm = false;
		if(!isNextWorkFlowStep(signRequest.getParentSignBook())) {
			if(data != null && data.getForm() != null) {
				Form form = data.getForm();
				for (Field field : form.getFields()) {
					if ("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
						if (field.getExtValueReturn().equals("id")) {
							data.getDatas().put(field.getName(), signRequest.getToken());
							formDataMap.put(field.getName(), signRequest.getToken());
						}
					}
				}
				isForm = true;
			}
		}
		byte[] bytes = toSignDocuments.get(0).getInputStream().readAllBytes();
		Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
		if(formDataMap != null && !formDataMap.isEmpty() && toSignDocuments.get(0).getContentType().equals("application/pdf")
				&& (reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty())) {
			filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap, isStepAllSignDone(signRequest.getParentSignBook()), isForm);
		} else {
			filledInputStream = toSignDocuments.get(0).getInputStream().readAllBytes();
		}
		boolean visual = true;
		if(signWith == null || SignWith.valueOf(signWith).equals(SignWith.imageStamp)) {
			byte[] signedInputStream = filledInputStream;
			String fileName = toSignDocuments.get(0).getFileName();
			if(signType.equals(SignType.hiddenVisa)) visual = false;
			if(signRequest.getSignRequestParams().isEmpty() && visual) {
				throw new EsupSignatureRuntimeException("Il faut apposer au moins un élément visuel");
			}
			int nbSign = 0;
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
				for(SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
					if((signRequestParams.getSignImageNumber() < 0 || StringUtils.hasText(signRequestParams.getTextPart())) && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign() || signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSingleSignWithAnnotation())) {
						signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), false);
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
					} else if(signRequestParams.getSignImageNumber() >= 0 && !StringUtils.hasText(signRequestParams.getTextPart()) && (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getMultiSign() || nbSign == 0)) {
						signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), false);
						lastSignLogs.add(updateStatus(signRequest.getId(), signRequest.getStatus(), "Apposition de la signature", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
						auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
						nbSign++;
					}
				}
			} else {
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Visa", "Pas de timestamp", "", "", date, isViewed, null, null, null);
			}
			if (isStepAllSignDone(signRequest.getParentSignBook()) && (reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty()) && BooleanUtils.isNotFalse(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getConvertToPDFA())) {
				signedInputStream = pdfService.convertToPDFA(pdfService.writeMetadatas(signedInputStream, fileName, signRequest, lastSignLogs));
			}
			byte[] signedBytes = signedInputStream;

			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, signType, comment);
			documentService.addSignedFile(signRequest, new ByteArrayInputStream(signedBytes), signRequest.getTitle() + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType(), user);
		} else {
			SignRequestParams lastSignRequestParams = findLastSignRequestParams(signRequest);
			reports = validationService.validate(getToValidateFile(signRequest.getId()), null);
			if (reports == null || reports.getDiagnosticData().getAllSignatures().isEmpty()) {
				filledInputStream = stampImagesOnFirstSign(signRequest, signRequest.getSignRequestParams(), userEppn, authUserEppn, filledInputStream, date, lastSignLogs, lastSignRequestParams);
			} else {
				logger.warn("skip add visuals because document already signed");
			}
			if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && lastSignRequestParams != null) {
				signRequestParamsService.copySignRequestParams(signRequest.getId(), Collections.singletonList(lastSignRequestParams));
				toSignDocuments.get(0).setTransientInputStream(new ByteArrayInputStream(filledInputStream));
			}
			SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(toSignDocuments, signRequest, true);
			Document signedDocument = signService.certSign(signatureDocumentForm, signRequest, signerUser.getEppn(), password, SignWith.valueOf(signWith), lastSignRequestParams);
			auditTrailService.createSignAuditStep(signRequest, userEppn, signedDocument, isViewed);
			stepStatus = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, SignType.signature, comment);

		}
		customMetricsService.incValue("esup-signature.signrequests", "signed");
		return stepStatus;
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

	/**
     * Ajoute des images aux emplacements spécifiés lors de la première signature d'une demande.
     *
     * @param signRequest La demande de signature contenant les informations nécessaires au processus.
     * @param signRequestParamses Liste des paramètres de signature définissant les emplacements et options des signatures.
     * @param userEppn L'identifiant eppn de l'utilisateur effectuant la demande de signature.
     * @param authUserEppn L'identifiant eppn de l'utilisateur authentifié actuellement.
     * @param filledInputStream Le tableau d'octets représentant le flux PDF déjà rempli et modifiable.
     * @param date La date actuelle pour l'horodatage du processus de signature.
     * @param lastSignLogs Liste des journaux des opérations de signature précédentes, si disponible.
     * @param lastSignRequestParams Le dernier paramètre de signature utilisé dans le processus, permettant d'éviter des doublons.
     * @return Le flux PDF représenté comme un tableau d'octets, contenant les images supplémentaires insérées.
     */
    public byte[] stampImagesOnFirstSign(SignRequest signRequest, List<SignRequestParams> signRequestParamses, String userEppn, String authUserEppn, byte[] filledInputStream, Date date, List<Log> lastSignLogs, SignRequestParams lastSignRequestParams) {
		User signerUser = userService.getByEppn(userEppn);
		boolean isViewed = signRequest.getViewedBy().contains(signerUser);
		if (signRequestParamses.size() > 1) {
			for (SignRequestParams signRequestParams : signRequestParamses) {
				if(signRequestParams.equals(lastSignRequestParams)) continue;
				filledInputStream = pdfService.stampImage(filledInputStream, signRequest, signRequestParams, 1, signerUser, date, userService.getRoles(userEppn).contains("ROLE_OTP"), true);
				Log log = updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", null, "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
				if(lastSignLogs != null) {
					lastSignLogs.add(log);
				}
				auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", "", "", date, isViewed, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
			}
		}
		return filledInputStream;
	}

	/**
     * Applique les règles de fin de signature pour une demande de signature donnée. La méthode gère les mises à jour
     * des statuts de la requête, les commentaires, la validation des destinataires, ainsi que les étapes du flux de travail.
     *
     * @param signRequestId Identifiant de la requête de signature à traiter.
     * @param userEppn EPPN (eduPersonPrincipalName) de l'utilisateur effectuant l'action.
     * @param authUserEppn EPPN de l'utilisateur authentifié.
     * @param signType Type de signature (Visa, Signature, etc.).
     * @param comment Commentaire à ajouter à la requête de signature.
     * @return Le statut final de l'étape (StepStatus) après application des règles : completed, last_end ou not_completed.
     * @throws EsupSignatureRuntimeException Exception levée en cas d'erreur pendant le traitement.
     */
    @Transactional
	public StepStatus applyEndOfSignRules(Long signRequestId, String userEppn, String authUserEppn, SignType signType, String comment) throws EsupSignatureRuntimeException {
		SignRequest signRequest = getById(signRequestId);
		if (signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa)) {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, null, userEppn);
			}
			updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa",  comment, "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
		} else {
			if(comment != null && !comment.isEmpty()) {
				commentService.create(signRequest.getId(), comment, 0, 0, 0, null,true, null, userEppn);
			}
			updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", comment, "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
		}
		recipientService.validateRecipient(signRequest, userEppn);
		if (isSignRequestStepCompleted(signRequest)) {
			completeSignRequests(Collections.singletonList(signRequest), authUserEppn);
			if (isCurrentStepCompleted(signRequest)) {
				for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
					recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
				}
				if (nextWorkFlowStep(signRequest.getParentSignBook())) {
					return StepStatus.completed;
				} else {
					return StepStatus.last_end;
				}
			}
		} else {
			updateStatus(signRequest.getId(), SignRequestStatus.pending, "Demande incomplète", null, "SUCCESS", null, null, null, null,  userEppn, authUserEppn);
		}
		return StepStatus.not_completed;
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
		SignRequest signRequest = getById(signRequestId);
		SignatureDocumentForm signatureDocumentForm = getAbstractSignatureForm(getToSignDocuments(signRequestId), signRequest, true);
		Document document = signService.certSign(signatureDocumentForm, signRequest, "system", "", SignWith.sealCert, null);
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 1);
		}
		if(signRequest.getSignedDocuments().size() > 1) {
			signRequest.getSignedDocuments().remove(signRequest.getSignedDocuments().size() - 2);
		}
		signRequest.getSignedDocuments().add(document);
	}

	/**
     * Retourne le nombre de SignBooks suivis par un utilisateur spécifique.
     *
     * @param userEppn l'identifiant unique de l'utilisateur (EPPN) pour lequel le compte des SignBooks suivis est calculé.
     * @return le nombre total de SignBooks où l'utilisateur identifié par userEppn est un spectateur.
     */
    public Long nbFollowedByMe(String userEppn) {
		return signBookRepository.countByViewersContaining(userEppn);
	}

	/**
     * Récupère la liste des demandes de signature à signer pour un utilisateur donné.
     *
     * @param userEppn l'identifiant unique (EPPN) de l'utilisateur pour lequel les demandes de signature doivent être récupérées
     * @return une liste de SignRequest contenant toutes les demandes de signature à signer, triées par date de création dans un ordre décroissant
     */
    @Transactional
	public List<SignRequest> getToSignRequests(String userEppn) {
		User user = userService.getByEppn(userEppn);
		List<SignRequest> signRequestsToSign = signBookRepository.findToSign(user, null, null, null, new Date(0), new Date(), Pageable.unpaged()).getContent()
				.stream().map(SignBook::getSignRequests).flatMap(Collection::stream).collect(Collectors.toList());
		signRequestsToSign = signRequestsToSign.stream().sorted(Comparator.comparing(SignRequest::getCreateDate).reversed()).collect(Collectors.toList());
		return  signRequestsToSign;
	}

	/**
     * Crée une nouvelle requête de signature et l'associe à un SignBook.
     *
     * @param name le nom de la requête de signature. Peut être nul ou vide, auquel cas un nom généré automatiquement sera utilisé.
     * @param signBook l'objet SignBook auquel la requête de signature doit être associée.
     * @param userEppn l'identifiant unique EPPN de l'utilisateur créant la requête.
     * @param authUserEppn l'identifiant unique EPPN de l'utilisateur authentifié.
     * @return Une instance de SignRequest représentant la nouvelle requête de signature créée.
     */
    @Transactional
	public SignRequest createSignRequest(String name, SignBook signBook, String userEppn, String authUserEppn) {
		String token = UUID.randomUUID().toString();
		while (signRequestRepository.findByToken(token).isPresent()) {
			token = UUID.randomUUID().toString();
		}
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = new SignRequest();
		if(name == null || name.isEmpty()) {
			if (signBook.getSignRequests().isEmpty()) {
				signRequest.setTitle(signBook.getSubject());
			} else {
				signRequest.setTitle(signBook.getSubject() + "_" + signBook.getSignRequests().size());
			}
		} else {
			signRequest.setTitle(name);
		}
		signRequest.setToken(token);
		signRequest.setCreateBy(user);
		signRequest.setCreateDate(new Date());
		signRequest.setParentSignBook(signBook);
		signRequest.setStatus(SignRequestStatus.draft);
		signRequestRepository.save(signRequest);
		signBook.getSignRequests().add(signRequest);
		updateStatus(signRequest.getId(), SignRequestStatus.draft, "Création de la demande " + signBook.getId(), null, "SUCCESS", null, null, null, null, userEppn, authUserEppn);
		return signRequest;
	}

	/**
     * Ajoute des documents à une demande de signature.
     *
     * @param signRequest La demande de signature à laquelle les documents doivent être ajoutés.
     * @param scanSignatureFields Indique si les champs de signature doivent être scannés dans le document.
     * @param docNumber Numéro de document pour l'indexation.
     * @param signRequestParamses Liste des paramètres de champ de signature.
     * @param multipartFiles Liste des fichiers à ajouter sous forme de fichiers multipart.
     * @throws EsupSignatureIOException Si une erreur survient lors de l'ajout ou de la conversion des fichiers.
     */
    @Transactional
	public void addDocsToSignRequest(SignRequest signRequest, boolean scanSignatureFields, int docNumber, List<SignRequestParams> signRequestParamses, MultipartFile... multipartFiles) throws EsupSignatureIOException {
		for(MultipartFile multipartFile : multipartFiles) {
			try {
				byte[] bytes = multipartFile.getInputStream().readAllBytes();
				String pdfaCheck = smallCheckPDFA(bytes);
				String contentType = multipartFile.getContentType();
				InputStream inputStream = new ByteArrayInputStream(bytes);
				if (multipartFiles.length == 1 && bytes.length > 0) {
					if("application/pdf".equals(multipartFiles[0].getContentType()) && (scanSignatureFields || (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && StringUtils.hasText(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getSignRequestParamsDetectionPattern())))) {
						bytes = pdfService.normalizePDF(bytes);
						List<SignRequestParams> toAddSignRequestParams = new ArrayList<>();
						if(signRequestParamses.isEmpty()) {
							toAddSignRequestParams = signRequestParamsService.scanSignatureFields(new ByteArrayInputStream(bytes), docNumber, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), true);
						} else {
							for (SignRequestParams signRequestParams : signRequestParamses) {
								toAddSignRequestParams.add(signRequestParamsService.createSignRequestParams(signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos()));
							}
						}
						addAllSignRequestParamsToSignRequest(signRequest, toAddSignRequestParams);
						Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
						if(reports == null || reports.getSimpleReport().getSignatureIdList().isEmpty()) {
							inputStream = pdfService.removeSignField(new ByteArrayInputStream(bytes), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow());
						}
					} else if(contentType != null && contentType.contains("image")){
						bytes = pdfService.jpegToPdf(multipartFile.getInputStream(), multipartFile.getName()).readAllBytes();
						contentType = "application/pdf";
						inputStream = new ByteArrayInputStream(bytes);
					}
					Document document = documentService.createDocument(inputStream, signRequest.getCreateBy(), multipartFile.getOriginalFilename(), contentType);
					document.setPdfaCheck(pdfaCheck);
					signRequest.getOriginalDocuments().add(document);
					document.setParentId(signRequest.getId());
				} else {
					logger.warn("file size is 0");
					throw new EsupSignatureIOException("Erreur lors de l'ajout des fichiers");
				}
			} catch (IOException e) {
				logger.warn("error on adding files", e);
				throw new EsupSignatureIOException("Erreur lors de l'ajout des fichiers", e);
			} catch (EsupSignatureRuntimeException e) {
				logger.warn("error on converting files", e);
				throw new EsupSignatureIOException("Erreur lors de la conversion du document", e);
			}
		}
	}

	/**
     * Ajoute tous les paramètres de demande de signature à une demande de signature existante
     * et crée des commentaires liés à chaque paramètre ajouté.
     *
     * @param signRequest L'objet SignRequest auquel les paramètres doivent être ajoutés.
     * @param toAddSignRequestParams La liste des objets SignRequestParams qui seront ajoutés au SignRequest.
     */
    public void addAllSignRequestParamsToSignRequest(SignRequest signRequest, List<SignRequestParams> toAddSignRequestParams) {
		signRequest.getSignRequestParams().addAll(toAddSignRequestParams);
		int step = 1;
		for(SignRequestParams signRequestParams : toAddSignRequestParams) {
			commentService.create(signRequest.getId(), "", signRequestParams.getxPos(), signRequestParams.getyPos(), signRequestParams.getSignPageNumber(), step, false, null, "system");
			step++;
		}
	}

	/**
     * Met à jour une demande de signature en la définissant comme en attente de signature.
     * Affecte une action vide à chaque destinataire de l'étape courante.
     * Vérifie le type de signature et ajuste si nécessaire pour inclure la signature visible.
     * Met également à jour le statut de la demande et enregistre une métrique personnalisée.
     * Envoie les informations d'attente pour les cibles REST, si applicable.
     *
     * @param signRequest Demande de signature à mettre à jour en tant qu'attente de signature.
     * @param authUserEppn Identifiant unique de l'utilisateur authentifié exécutant l'action.
     */
    @Transactional
	public void pendingSignRequest(SignRequest signRequest, String authUserEppn) {
		for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
			signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
			if (isSigned(signRequest, null) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().getValue() < 3) {
					signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().setSignType(SignType.signature);
				}
			}
		}
		updateStatus(signRequest.getId(), SignRequestStatus.pending, "Envoyé pour signature", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
		customMetricsService.incValue("esup-signature.signrequests", "new");
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
			targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "pending", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), authUserEppn, "");
		}
	}

	private boolean isNextWorkFlowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 2;
	}

	/**
     * Vérifie si le SignBook possède davantage d'étapes dans le workflow actuel.
     *
     * @param signBook l'objet SignBook contenant le workflow et ses étapes.
     * @return vrai si le workflow du SignBook actuel contient des étapes supplémentaires à celle en cours, faux sinon.
     */
    public boolean isMoreWorkflowStep(SignBook signBook) {
		return signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >= signBook.getLiveWorkflow().getCurrentStepNumber() + 1 && signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
	}

	private boolean isStepAllSignDone(SignBook signBook) {
		LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
		return (!liveWorkflowStep.getAllSignToComplete() || isWorkflowStepFullSigned(liveWorkflowStep)) && !isMoreWorkflowStep(signBook);
	}

	private boolean isWorkflowStepFullSigned(LiveWorkflowStep liveWorkflowStep) {
		for (Recipient recipient : liveWorkflowStep.getRecipients()) {
			if (!recipient.getSigned()) {
				return false;
			}
		}
		return true;
	}

	/**
     * Avance à l'étape suivante du flux de travail pour un SignBook donné, si des étapes supplémentaires existent.
     *
     * @param signBook l'objet SignBook contenant le flux de travail en cours (LiveWorkflow) et ses étapes.
     * @return true si le flux de travail a avancé vers une nouvelle étape, false si aucune étape supplémentaire n'existe.
     */
    public boolean nextWorkFlowStep(SignBook signBook) {
		if (isMoreWorkflowStep(signBook)) {
			signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(signBook.getLiveWorkflow().getCurrentStepNumber()));
			return signBook.getLiveWorkflow().getCurrentStepNumber() > -1;
		}
		return false;
	}

	/**
     * Vérifie si l'étape actuelle de la demande de signature est terminée.
     *
     * L'étape est considérée comme terminée si toutes les demandes de signature associées
     * sont dans le statut "terminée" ou "refusée".
     *
     * @param signRequest une instance de SignRequest représentant la demande de signature à évaluer
     * @return true si l'étape actuelle est terminée, false sinon
     */
    public boolean isCurrentStepCompleted(SignRequest signRequest) {
		return signRequest.getParentSignBook().getSignRequests().stream().allMatch(sr -> sr.getStatus().equals(SignRequestStatus.completed) || sr.getStatus().equals(SignRequestStatus.refused));
	}

	private boolean isSignRequestStepCompleted(SignRequest signRequest) {
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).noneMatch(recipient -> signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		} else {
			return signRequest.getRecipientHasSigned().keySet().stream().filter(r -> signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().contains(r)).anyMatch(recipient -> !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
		}
	}

	/**
     * Complète les demandes de signature en attribuant le statut "Terminé" si elles ne sont pas refusées.
     *
     * @param signRequests liste des demandes de signature à compléter
     * @param authUserEppn 'EPPN' de l'utilisateur authentifié effectuant cette action
     */
    @Transactional
	public void completeSignRequests(List<SignRequest> signRequests, String authUserEppn) {
		for(SignRequest signRequest : signRequests) {
			if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
				updateStatus(signRequest.getId(), SignRequestStatus.completed, "Terminé", null,"SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
			}
		}
	}

    /**
     * Ajoute un post-it à un SignBook identifié par son identifiant.
     *
     * @param signBookId L'identifiant du SignBook auquel ajouter un post-it.
     * @param comment Le commentaire à ajouter sous forme de post-it.
     * @param userEppn L'identifiant EPPN de l'utilisateur effectuant l'action.
     * @param authUserEppn L'identifiant EPPN de l'utilisateur authentifié.
     */
    @Transactional
	public void addPostit(Long signBookId, String comment, String userEppn, String authUserEppn) {
		SignBook signBook = signBookRepository.findById(signBookId).orElseThrow();
		for(SignRequest signRequest : signBook.getSignRequests()) {
			if (comment != null && !comment.isEmpty()) {
				updateStatus(signRequest.getId(), signRequest.getStatus(), "comment", comment, "SUCCES", null, null, null, 0, userEppn, authUserEppn);
			}
		}
	}

	@Transactional
	protected void cleanDocuments(SignRequest signRequest, String authUserEppn) {
		Date cleanDate = getEndDate(signRequest);
		if(cleanDate != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(cleanDate);
			cal.add(Calendar.DATE, globalProperties.getDelayBeforeCleaning());
			Date test = cal.getTime();
			Date now = new Date();
			if(signRequest.getExportedDocumentURI() != null
					&& test.getTime() < now.getTime()
					&& !signRequest.getSignedDocuments().isEmpty()) {
				clearAllDocuments(signRequest);
				updateStatus(signRequest.getId(), SignRequestStatus.exported, "Fichiers nettoyés", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
			} else {
				logger.debug("cleanning documents was skipped because date");
			}
		} else {
			logger.error("no end date for signrequest " + signRequest.getId());
		}
	}

	@Transactional
	public void clearAllDocuments(SignRequest signRequest) {
		if(signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().isEmpty()) {
			logger.info("clear all documents from signRequest : " + signRequest.getId());
			List<Document> documents = new ArrayList<>();
			documents.addAll(signRequest.getOriginalDocuments());
			documents.addAll(signRequest.getSignedDocuments());
			signRequest.getOriginalDocuments().clear();
			signRequest.getSignedDocuments().clear();
			deleteNexu(signRequest.getId());
			for(Document document : documents) {
				documentService.delete(document);
			}
		}
	}

	/**
     * Supprime les paramètres Nexu et ses signatures associées d'une demande signature.
     *
     * @param id L'identifiant de SignRequest à supprimer.
     */
    @Transactional
	public void deleteNexu(Long id) {
		SignRequest signRequest = signRequestRepository.findById(id).orElseThrow();
		List<NexuSignature> nexuSignatures = nexuSignatureRepository.findBySignRequestId(id);
		for(NexuSignature nexuSignature : nexuSignatures) {
			signRequest.getSignedDocuments().removeAll(nexuSignature.getDocumentToSign());
			nexuSignatureRepository.delete(nexuSignature);
		}
	}

	/**
     * Récupère le dernier fichier signé dans le cadre d'une demande de signature.
     *
     * @param signRequest la demande de signature contenant les informations nécessaires pour identifier le fichier
     *                    à récupérer. Elle doit inclure des informations sur le statut de la demande et l'URI
     *                    du document exporté, le cas échéant.
     * @return un objet FsFile représentant le dernier fichier signé. Dans le cas où la demande de signature a été
     *         exportée et que l'URI du document exporté est valide, le fichier correspondant est retourné.
     *         Si l'URI n'est pas valide, ou si la demande n'est pas marquée comme "exportée", le dernier document
     *         "à signer" est utilisé pour générer un FsFile.
     * @throws EsupSignatureFsException si une erreur survient lors de l'accès au service de fichiers ou du
     *                                  traitement de l'URI du document exporté.
     */
    public FsFile getLastSignedFsFile(SignRequest signRequest) throws EsupSignatureFsException {
		if(signRequest.getStatus().equals(SignRequestStatus.exported)) {
			if (signRequest.getExportedDocumentURI() != null && !signRequest.getExportedDocumentURI().startsWith("mail")) {
				FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(signRequest.getExportedDocumentURI());
				return fsAccessService.getFileFromURI(signRequest.getExportedDocumentURI());
			}
		}
		Document lastSignedDocument = getToSignDocuments(signRequest.getId()).get(0);
		return new FsFile(lastSignedDocument.getInputStream(), lastSignedDocument.getFileName(), lastSignedDocument.getContentType());
	}

    /**
     * Met à jour le statut d'une demande de signature et crée un log associé.
     *
     * @param signRequestId L'identifiant de la demande de signature.
     * @param signRequestStatus Le nouveau statut de la demande de signature.
     * @param action L'action effectuée sur la demande.
     * @param comment Un commentaire associé à l'action.
     * @param returnCode Le code de retour associé à l'action.
     * @param pageNumber Le numéro de la page concernée.
     * @param posX La position horizontale associée à l'action.
     * @param posY La position verticale associée à l'action.
     * @param stepNumber Le numéro de l'étape du processus.
     * @param userEppn L'identifiant unique de l'utilisateur principal.
     * @param authUserEppn L'identifiant unique de l'utilisateur authentifié.
     * @return Un objet Log représentant le log créé associé à la mise à jour du statut.
     */
    @Transactional
	public Log updateStatus(Long signRequestId, SignRequestStatus signRequestStatus, String action, String comment, String returnCode, Integer pageNumber, Integer posX, Integer posY, Integer stepNumber, String userEppn, String authUserEppn) {
		SignBook signBook = getById(signRequestId).getParentSignBook();
		return logService.create(signRequestId, signBook.getSubject(), signBook.getWorkflowName(), signRequestStatus, action, comment, returnCode, pageNumber, posX, posY, stepNumber, userEppn, authUserEppn);
	}

	/**
     * Supprime une demande de signature en fonction de son identifiant et de l'utilisateur spécifié.
     * La suppression peut être définitive ou non, en fonction de l'état actuel de la demande.
     *
     * @param signRequestId L'identifiant de la demande de signature à supprimer.
     * @param userEppn L'identifiant de l'utilisateur effectuant l'opération de suppression.
     * @return L'identifiant de la demande parent (SignBook) si la suppression s'est effectuée correctement.
     * @throws EsupSignatureRuntimeException Si la demande de signature ne peut pas être supprimée.
     */
    @Transactional
	public Long delete(Long signRequestId, String userEppn) {
		logger.info("start delete of signrequest " + signRequestId);
		SignRequest signRequest = getById(signRequestId);
		if(!isDeletetable(signRequest, userEppn)) {
			throw new EsupSignatureRuntimeException("Interdiction de supprimer les demandes de ce circuit");
		}
		if(signRequest.getDeleted() || signRequest.getStatus().equals(SignRequestStatus.draft)) {
			return deleteDefinitive(signRequestId, userEppn);
		} else {
			logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Mise à la corbeille du document par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getArchiveStatus().equals(ArchiveStatus.archived)) {
				logger.info("nettoyage des documents archivés ou exportés");
				signRequest.getOriginalDocuments().clear();
				signRequest.getSignedDocuments().clear();
				logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Nettoyage des documents déjà archivés", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			}
			if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(SignRequest::getDeleted)) {
				signRequest.getParentSignBook().setDeleted(true);
				signRequest.getParentSignBook().setUpdateDate(new Date());
				signRequest.getParentSignBook().setUpdateBy(userEppn);
				logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Mise à la corbeille de la demande par l'utilisateur", "", "SUCCESS", null, null, null, null, userEppn, userEppn);
			}
			signRequest.setDeleted(true);
			for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
				try {
					targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "deleted", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), userEppn, "");
				} catch (Exception e) {
					logger.error("error on sending deleted on rest " + target.getTargetUri());
				}

			}
			return signRequest.getParentSignBook().getId();
		}
	}

	/**
     * Supprime définitivement une demande de signature ainsi que ses données associées, ses commentaires et ses paramètres,
     * tout en effectuant le nettoyage approprié sur les objets liés et en notifiant éventuellement les cibles avec des
     * informations mises à jour sur l'état de la suppression.
     *
     * @param signRequestId L'identifiant de la demande de signature à supprimer définitivement.
     * @param userEppn Le nom principal de l'utilisateur (EPPN) effectuant l'opération de suppression.
     * @return L'identifiant du livre de signatures parent si celui-ci n'est pas supprimé ou 0 si le livre de signatures
     *         a été supprimé.
     */
    @Transactional
	public Long deleteDefinitive(Long signRequestId, String userEppn) {
		logger.info("start definitive delete of signrequest " + signRequestId);
		SignRequest signRequest = getById(signRequestId);
		SignBook signBook = signRequest.getParentSignBook();
//		boolean testAllNone = signRequest.getRecipientHasSigned().values().stream().allMatch(a -> a.getActionType().equals(ActionType.none));
//		int step = signBook.getLiveWorkflow().getLiveWorkflowSteps().indexOf(signBook.getLiveWorkflow().getCurrentStep());
//		if(testAllNone && !signRequest.getDeleted() && step > 0) {
//			return -1L;
//		}
		deleteNexu(signRequestId);
		logService.create(signRequestId, signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), SignRequestStatus.deleted, "Suppression définitive", null, "SUCCESS", null, null, null,null, userEppn, userEppn);
		signRequest.getRecipientHasSigned().clear();
		signRequestRepository.save(signRequest);
		if (signRequest.getData() != null) {
			Long dataId = signRequest.getData().getId();
			signRequest.setData(null);
			dataService.delete(dataId);
		}
		List<Long> commentsIds = signRequest.getComments().stream().map(Comment::getId).toList();
		for (Long commentId : commentsIds) {
			commentService.deleteComment(commentId, signRequest);
		}
		signBook.getSignRequests().remove(signRequest);
		for(SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
			for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
				liveWorkflowStep.getSignRequestParams().remove(signRequestParams);
			}
		}
		signRequestRepository.delete(signRequest);
		for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
			try {
				targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "cleaned", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), userEppn, "");
			} catch (Exception e) {
				logger.error("error on sending deleted on rest " + target.getTargetUri());
			}
		}
		long signBookId = 0;
		if(!signBook.getSignRequests().isEmpty()) {
			signBookId = signBook.getId();
		} else {
			signBookRepository.delete(signBook);
		}
		if(!signBook.getDeleted() && signBook.getStatus().equals(SignRequestStatus.pending) && signBook.getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.signed) || s.getStatus().equals(SignRequestStatus.completed) || s.getStatus().equals(SignRequestStatus.refused))) {
			boolean isNextWorkflow = nextWorkFlowStep(signBook);
			if(isNextWorkflow) {
				for(SignRequest signRequest1 : signRequest.getParentSignBook().getSignRequests()) {
					if(!signRequest1.equals(signRequest) && !signRequest1.getStatus().equals(SignRequestStatus.refused)) {
						pendingSignRequest(signRequest1, userEppn);
					}
				}
			} else {
				if(signBook.getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.refused))) {
					signBook.setStatus(SignRequestStatus.refused);
				} else {
					signBook.setStatus(SignRequestStatus.completed);
				}
			}
		}
		if(signRequest.getParentSignBook().getSignRequests().stream().allMatch(s -> s.getStatus().equals(SignRequestStatus.refused))) {
			signRequest.getParentSignBook().setStatus(SignRequestStatus.refused);
		}
		return signBookId;
	}

	private Date getEndDate(SignRequest signRequest) {
		List<Action> action = signRequest.getRecipientHasSigned().values().stream().filter(action1 -> !action1.getActionType().equals(ActionType.none)).sorted(Comparator.comparing(Action::getDate)).collect(Collectors.toList());
		if(!action.isEmpty()) {
			return action.get(0).getDate();
		}
		return null;
	}

	/**
     * Vérifie si une demande de signature peut être supprimée.
     *
     * @param signRequest la demande de signature à évaluer.
     * @param userEppn l'identifiant unique (eppn) de l'utilisateur effectuant l'action.
     * @return true si la demande de signature est supprimable, false sinon.
     */
    public boolean isDeletetable(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		return signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() == null
				||
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDisableDeleteByCreator() == null
				||
				!signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDisableDeleteByCreator()
				||
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())
				||
				user.getRoles().contains("ROLE_ADMIN");
	}

	/**
     * Vérifie les utilisateurs temporaires associés à un SignBook basé sur son identifiant et une liste de destinataires.
     * Si des utilisateurs temporaires sont trouvés et correspondent aux destinataires fournis, effectue des actions spécifiques
     * comme l'envoi d'email ou la mise à jour des informations d'utilisateur. Retourne true si la vérification échoue, sinon false.
     *
     * @param id l'identifiant du SignBook à vérifier
     * @param recipients la liste des destinataires (RecipientWsDto) à comparer avec les utilisateurs temporaires
     * @return true si des utilisateurs temporaires ne correspondent pas aux destinataires fournis ou en nombre insuffisant, false si tout est conforme
     * @throws EsupSignatureRuntimeException en cas d'erreur lors de l'opération
     */
    @Transactional
	public boolean checkTempUsers(Long id, List<RecipientWsDto> recipients) throws EsupSignatureRuntimeException {
		SignBook signBook = signBookRepository.findById(id).get();
		List<User> tempUsers = userService.getTempUsers(signBook, recipients);
		if(!tempUsers.isEmpty()) {
			if (recipients != null && tempUsers.size() <= recipients.size()) {
				for (User tempUser : tempUsers) {
					if (tempUser.getUserType().equals(UserType.shib)) {
						logger.warn("TODO Envoi Mail SHIBBOLETH ");
						//TODO envoi mail spécifique
					} else if (tempUser.getUserType().equals(UserType.external)) {
						RecipientWsDto recipientWsDto = recipients.stream().filter(recipientWsDto1 -> recipientWsDto1.getEmail().toLowerCase().equals(tempUser.getEmail().toLowerCase(Locale.ROOT))).findFirst().get();
                        if(StringUtils.hasText(recipientWsDto.getFirstName())) {
                            tempUser.setFirstname(recipientWsDto.getFirstName());
                        }
                        if(StringUtils.hasText(recipientWsDto.getName())) {
                            tempUser.setName(recipientWsDto.getName());
                        }
						if(StringUtils.hasText(recipientWsDto.getPhone())) {
							userService.updatePhone(tempUser.getEppn(), recipientWsDto.getPhone());
						}
					}
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
     * Pré-remplit les champs d'une demande de signature en fonction des données disponibles,
     * du formulaire associé et des étapes du workflow actives.
     * Les valeurs par défaut sont définies à partir des données existantes.
     * Les autorisations d'édition des champs sont déterminées en fonction de l'utilisateur
     * et de l'étape active actuelle dans le workflow.
     *
     * @param signRequestId l'identifiant de la demande de signature à traiter
     * @param userEppn le nom principal (EPPN) de l'utilisateur actuel
     * @return une liste de champs (`Field`) avec les valeurs pré-remplies et leurs statuts d'édition mis à jour
     */
    @Transactional
	public List<Field> prefillSignRequestFields(Long signRequestId, String userEppn) {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = getById(signRequestId);
		List<Field> prefilledFields = new ArrayList<>();
		Data data = dataService.getBySignBook(signRequest.getParentSignBook());
		if(data != null) {
			if(data.getForm() != null) {
				List<Field> fields = data.getForm().getFields();
				if (!"".equals(data.getForm().getPreFillType()) && signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getUsers().contains(user)) {
					prefilledFields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), fields, user, signRequest);
					for (Field field : prefilledFields) {
						if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() == null
								|| !field.getWorkflowSteps().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep())) {
							field.setDefaultValue("");
						}
					}
				} else {
					prefilledFields = data.getForm().getFields();
				}
			}
		}
		for (Field field : prefilledFields) {
			if (field.getName() != null
				&& !data.getDatas().isEmpty()
				&& data.getDatas().get(field.getName()) != null
				&& !data.getDatas().get(field.getName()).isEmpty()) {
				field.setDefaultValue(data.getDatas().get(field.getName()));
			}
			for(WorkflowStep workflowStep : field.getWorkflowSteps()) {
				Optional<LiveWorkflowStep> liveWorkflowStep = signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().stream().filter(l -> workflowStep.equals(l.getWorkflowStep())).findFirst();
				if(liveWorkflowStep.isPresent()) {
					if(liveWorkflowStep.get().getRecipients().stream().anyMatch(recipient -> recipient.getUser().getEppn().equals(userEppn))
						&& liveWorkflowStep.get().equals(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep())) {
						field.setEditable(true);
						break;
					} else{
						field.setEditable(false);
					}
				} else {
					field.setEditable(false);
				}
			}
		}
		return prefilledFields;
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
     * Vérifie si un document dans un classeur de signatures est signé.
     *
     * @param signBook le classeur de signatures contenant différentes demandes de signature
     * @param reports l'objet de rapports utilisé pour suivre les actions ou les états des signatures
     * @return true si au moins une des demandes de signature dans le classeur est signée, sinon false
     */
    @Transactional
	public boolean isSigned(SignBook signBook, Reports reports) {
		for (SignRequest signRequest : signBook.getSignRequests()) {
			if (isSigned(signRequest, reports)) {
				return true;
			}
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

	/**
     * Ajoute des pièces jointes à une demande de signature. Les fichiers fournis sont
     * ajoutés à la demande de signature ainsi qu'un éventuel lien fourni.
     * Les pièces jointes sont ajoutées uniquement si elles contiennent des données.
     *
     * @param multipartFiles un tableau de fichiers à ajouter en tant que pièces jointes
     *                       à la demande de signature, peut être null
     * @param link un lien à associer à la demande de signature, peut être null ou vide
     * @param signRequestId l'identifiant de la demande de signature à laquelle les fichiers
     *                      et/ou le lien seront ajoutés
     * @param authUserEppn l'identifiant de l'utilisateur authentifié effectuant l'ajout
     * @return true si au moins une pièce jointe ou un lien a été ajouté avec succès,
     *         false sinon
     * @throws EsupSignatureIOException si une erreur liée à l'ajout des pièces jointes se produit
     */
    @Transactional
	public boolean addAttachement(MultipartFile[] multipartFiles, String link, Long signRequestId, String authUserEppn) throws EsupSignatureIOException {
		SignRequest signRequest = getById(signRequestId);
		int nbAttachmentAdded = 0;
		if(multipartFiles != null) {
			for (MultipartFile multipartFile : multipartFiles) {
				if(multipartFile.getSize() > 0) {
					addAttachmentToSignRequest(signRequest, authUserEppn, multipartFile);
					nbAttachmentAdded++;
					logService.create(signRequestId, signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), signRequest.getStatus(), "Ajout d'une pièce jointe", multipartFile.getOriginalFilename(), "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
				}
			}
		}
		if(link != null && !link.isEmpty()) {
			signRequest.getLinks().add(link);
			nbAttachmentAdded++;
		}
		return nbAttachmentAdded > 0;
	}

    private void addAttachmentToSignRequest(SignRequest signRequest, String authUserEppn, MultipartFile... multipartFiles) throws EsupSignatureIOException {
        User user = userService.getByEppn(authUserEppn);
        for(MultipartFile multipartFile : multipartFiles) {
            try {
                Document document = documentService.createDocument(multipartFile.getInputStream(), user, "attachement_" + signRequest.getAttachments().size() + "_" + multipartFile.getOriginalFilename(), multipartFile.getContentType());
                signRequest.getAttachments().add(document);
                document.setParentId(signRequest.getId());
            } catch (IOException e) {
                throw new EsupSignatureIOException(e.getMessage(), e);
            }
        }
    }

    /**
     * Supprime une pièce jointe associée à une demande de signature.
     *
     * @param id l'identifiant de la demande de signature
     * @param attachementId l'identifiant de la pièce jointe à supprimer
     * @param redirectAttributes les attributs utilisés pour transmettre des informations supplémentaires
     *        après la redirection, comme les messages d'erreur ou de succès
     */
    @Transactional
	public void removeAttachement(Long id, Long attachementId, RedirectAttributes redirectAttributes) {
		SignRequest signRequest = getById(id);
		Document attachement = documentService.getById(attachementId);
		if (!attachement.getParentId().equals(signRequest.getId())) {
			redirectAttributes.addFlashAttribute("message", new JsMessage("error", "Pièce jointe non trouvée ..."));
		} else {
			signRequest.getAttachments().remove(attachement);
			documentService.delete(attachement);
		}
	}

	/**
     * Supprime un lien spécifique d'une requête de signature donnée.
     *
     * @param id L'identifiant unique de la requête de signature à partir de laquelle le lien sera supprimé.
     * @param linkId L'identifiant unique du lien à supprimer dans la liste des liens de la requête.
     */
    @Transactional
	public void removeLink(Long id, Integer linkId) {
		SignRequest signRequest = getById(id);
		String toRemove = new ArrayList<>(signRequest.getLinks()).get(linkId);
		signRequest.getLinks().remove(toRemove);
	}

    /**
     * Ajoute un commentaire ou un emplacement de signature à une demande de signature.
     *
     * @param id l'identifiant de la demande de signature
     * @param commentText le texte du commentaire
     * @param commentPageNumber le numéro de page où le commentaire doit être placé
     * @param commentPosX la position horizontale du commentaire (X)
     * @param commentPosY la position verticale du commentaire (Y)
     * @param commentWidth la largeur du commentaire (optionnel)
     * @param commentHeight la hauteur du commentaire (optionnel)
     * @param postit indique si le commentaire est un post-it ("on") ou non
     * @param spotStepNumber le numéro de l'étape du processus de signature à laquelle l'emplacement se réfère (optionnel)
     * @param authUserEppn le eppn de l'utilisateur authentifié qui effectue l'opération
     * @param userEppn le eppn de l'utilisateur pour lequel le post-it doit être envoyé
     * @param forceSend indique s'il faut forcer l'envoi du post-it par email, même si la configuration globale ne l'exige pas
     * @return l'identifiant du commentaire créé, ou null si l'opération n'est pas autorisée
     */
    @Transactional
	public Long addComment(Long id, String commentText, Integer commentPageNumber, Integer commentPosX, Integer commentPosY, Integer commentWidth, Integer commentHeight, String postit, Integer spotStepNumber, String authUserEppn, String userEppn, boolean forceSend) {
		SignRequest signRequest = getById(id);
		User user = userService.getByEppn(userEppn);
		if(spotStepNumber == null || signRequest.getCreateBy().equals(user) || signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())) {
			if (spotStepNumber != null && spotStepNumber > 0) {
				SignRequestParams signRequestParams = signRequestParamsService.createSignRequestParams(commentPageNumber, commentPosX, commentPosY);
				if(commentWidth != null && commentHeight != null) {
					signRequestParams.setSignWidth(commentWidth);
					signRequestParams.setSignHeight(commentHeight);
				}
				int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
				signRequestParams.setSignDocumentNumber(docNumber);
				signRequestParams.setComment(commentText);
				signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(spotStepNumber - 1).getSignRequestParams().add(signRequestParams);
			}
			Comment comment = commentService.create(id, commentText, commentPosX, commentPosY, commentPageNumber, spotStepNumber, "on".equals(postit), null, authUserEppn);
			if(commentWidth != null && commentHeight != null) {
				comment.setSignWidth(commentWidth);
				comment.setSignHeight(commentHeight);
			}
			if (!(spotStepNumber != null && spotStepNumber > 0)) {
				updateStatus(signRequest.getId(), null, "Ajout d'un commentaire", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
				if ((globalProperties.getSendPostitByEmail() || forceSend)) {
					try {
						mailService.sendPostit(signRequest.getParentSignBook(), comment, userEppn, forceSend);
					} catch (EsupSignatureMailException e) {
						logger.warn("postit not sended", e);
					}
				}
			} else {
				updateStatus(signRequest.getId(), null, "Ajout d'un emplacement de signature", commentText, "SUCCESS", commentPageNumber, commentPosX, commentPosY, null, authUserEppn, authUserEppn);
			}
			return comment.getId();
		} else {
			return null;
		}
	}

	/**
     * Retourne le nombre de demandes de signature en attente pour un utilisateur donné.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur
     * @return le nombre de demandes de signature en attente
     */
    public Long getNbPendingSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.pending);
	}

	/**
     * Renvoie le nombre de demandes de signature ayant le statut "brouillon" pour un utilisateur donné.
     *
     * @param userEppn le eppn de l'utilisateur dont les demandes de signature doivent être comptées
     * @return le nombre de demandes de signature avec le statut "brouillon" associées à l'utilisateur donné
     */
    public Long getNbDraftSignRequests(String userEppn) {
		return signRequestRepository.countByCreateByEppnAndStatus(userEppn, SignRequestStatus.draft);
	}

	/**
     * Fournit le fichier original associé à une requête de signature donnée.
     *
     * @param signRequestId l'identifiant de la requête de signature pour laquelle le fichier original est demandé.
     * @param httpServletResponse l'objet HttpServletResponse dans lequel le fichier sera écrit.
     * @throws IOException si une erreur d'entrée/sortie survient pendant l'écriture du fichier dans la réponse.
     */
    @Transactional
	public void getOriginalFileResponse(Long signRequestId, HttpServletResponse httpServletResponse) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		webUtilsService.copyFileStreamToHttpResponse(signRequest.getOriginalDocuments().get(0).getFileName(), signRequest.getOriginalDocuments().get(0).getContentType(), "attachment", signRequest.getOriginalDocuments().get(0).getInputStream(), httpServletResponse);
    }

	/**
     * Récupère une pièce jointe associée à une requête de signature et écrit son contenu dans la réponse HTTP.
     *
     * @param signRequestId l'identifiant de la requête de signature
     * @param attachementId l'identifiant de la pièce jointe à récupérer
     * @param httpServletResponse la réponse HTTP dans laquelle le contenu de la pièce jointe sera écrit
     * @return true si la pièce jointe est trouvée et correctement copiée dans la réponse, false sinon
     * @throws IOException si une erreur d'entrée/sortie survient lors de l'écriture du fichier dans la réponse HTTP
     */
    @Transactional
	public boolean getAttachmentResponse(Long signRequestId, Long attachementId, HttpServletResponse httpServletResponse) throws IOException {
		SignRequest signRequest = getById(signRequestId);
		Document attachement = documentService.getById(attachementId);
		if (attachement != null && attachement.getParentId().equals(signRequest.getId())) {
			webUtilsService.copyFileStreamToHttpResponse(attachement.getFileName(), attachement.getContentType(), "attachment", attachement.getInputStream(), httpServletResponse);
			return true;
		}
		return false;
	}

	/**
     * Fournit un fichier à signer, en fonction de l'état de la requête de signature et des paramètres fournis.
     *
     * @param signRequestId L'identifiant unique de la requête de signature.
     * @param disposition La disposition de contenu HTTP (par exemple, "inline" ou "attachment").
     * @param httpServletResponse La réponse HTTP dans laquelle le fichier sera injecté.
     * @param force Indique si l'action doit être forcée, même si certaines restrictions sont imposées (par exemple, le téléchargement avant la fin du circuit).
     * @throws IOException Si une erreur d'entrée/sortie survient lors de la copie du fichier dans la réponse.
     * @throws EsupSignatureRuntimeException Si une erreur non contrôlée spécifique à l'application survient.
     * @throws EsupSignatureException Si une exception liée à la logique métier survient, comme une interdiction de téléchargement.
     */
    @Transactional
	public void getToSignFileResponse(Long signRequestId, String disposition, HttpServletResponse httpServletResponse, boolean force) throws IOException, EsupSignatureRuntimeException, EsupSignatureException {
		SignRequest signRequest = getById(signRequestId);
		if(!force && !disposition.equals("form-data")
				&& signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null
				&&  BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd())
				&& !signRequest.getStatus().equals(SignRequestStatus.completed)
				&& !signRequest.getStatus().equals(SignRequestStatus.refused)
				&& !signRequest.getArchiveStatus().equals(ArchiveStatus.archived)
				&& !signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned)
				&& !signRequest.getStatus().equals(SignRequestStatus.exported)) {
			throw new EsupSignatureException("Téléchargement interdit avant la fin du circuit");
		}
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			Document document;
			if(!documents.isEmpty()) {
				document = documents.get(0);
			} else {
				document = signRequest.getOriginalDocuments().get(0);
			}
			webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), disposition, document.getInputStream(), httpServletResponse);
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			webUtilsService.copyFileStreamToHttpResponse(fsFile.getName(), fsFile.getContentType(), disposition, fsFile.getInputStream(), httpServletResponse);
		}
	}

	/**
     * Génère une réponse contenant un document prêt à être signé, ou un fichier signé avec un QR code,
     * et l'écrit dans la réponse HTTP fournie.
     *
     * @param signRequestId l'identifiant de la requête de signature
     * @param httpServletResponse l'objet HttpServletResponse dans lequel le fichier sera écrit
     * @throws IOException si une erreur d'entrée/sortie se produit
     * @throws EsupSignatureRuntimeException si une erreur spécifique à l'application survient pendant le traitement
     * @throws WriterException si une erreur survient lors de la génération du QR code
     */
    @Transactional
	public void getToSignFileResponseWithCode(Long signRequestId, HttpServletResponse httpServletResponse) throws IOException, EsupSignatureRuntimeException, WriterException {
		SignRequest signRequest = getById(signRequestId);
		if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			Document document;
			if(!documents.isEmpty()) {
				document = documents.get(0);
			} else {
				document = signRequest.getOriginalDocuments().get(0);
			}
			InputStream inputStream = pdfService.addQrCode(signRequest, document.getInputStream());
			webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", inputStream, httpServletResponse);
		} else {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			InputStream inputStream = pdfService.addQrCode(signRequest, fsFile.getInputStream());
			webUtilsService.copyFileStreamToHttpResponse(fsFile.getName(), fsFile.getContentType(), "attachment", inputStream, httpServletResponse);
		}
	}


	/**
     * Fournit un fichier signé accompagné de son rapport.
     *
     * @param signRequestId l'identifiant de la requête de signature.
     * @param httpServletRequest la requête HTTP d'origine.
     * @param httpServletResponse la réponse HTTP dans laquelle le fichier est transmis.
     * @param force un indicateur permettant de forcer le téléchargement même si certaines restrictions sont en place.
     * @throws Exception si une erreur survient durant le traitement, notamment si les restrictions de téléchargement ne sont pas respectées.
     */
    @Transactional
	public void getSignedFileAndReportResponse(Long signRequestId, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, boolean force) throws Exception {
		SignRequest signRequest = getById(signRequestId);
		if(!force
			&& signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null
			&& BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd())
			&& (!signRequest.getStatus().equals(SignRequestStatus.completed)
			&& !signRequest.getStatus().equals(SignRequestStatus.refused)
			&& !signRequest.getStatus().equals(SignRequestStatus.exported)
			&& !signRequest.getArchiveStatus().equals(ArchiveStatus.archived)
			&& !signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned))) {
			throw new EsupSignatureException("Téléchargement interdit avant la fin du circuit");
		}
		webUtilsService.copyFileStreamToHttpResponse(signRequest.getParentSignBook().getSubject() + "-avec_rapport.zip", "application/zip; charset=utf-8", "attachment", new ByteArrayInputStream(getZipWithDocAndReport(signRequest, httpServletRequest, httpServletResponse)), httpServletResponse);
	}

	/**
     * Génère et retourne un fichier ZIP contenant les documents et un rapport associés à une demande de signature.
     *
     * @param signRequest la demande de signature pour laquelle les documents et le rapport doivent être générés
     * @param httpServletRequest l'objet HttpServletRequest lié à la requête HTTP actuelle
     * @param httpServletResponse l'objet HttpServletResponse lié à la réponse HTTP actuelle
     * @return un tableau d'octets représentant le fichier ZIP généré
     * @throws Exception si une erreur survient lors de la génération du fichier ZIP
     */
    @Transactional
	public byte[] getZipWithDocAndReport(SignRequest signRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
		Map<InputStream, String> inputStreams = new HashMap<>();
		String name = "";
		InputStream inputStream = null;
		if (signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.refused)) {
			List<Document> documents = getToSignDocuments(signRequest.getId());
			if(documents.size() == 1) {
				name = documents.get(0).getFileName();
				inputStream = documents.get(0).getInputStream();
			}
		} else if (signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getArchiveStatus().equals(ArchiveStatus.archived) || signRequest.getArchiveStatus().equals(ArchiveStatus.cleaned)) {
			FsFile fsFile = getLastSignedFsFile(signRequest);
			name = fsFile.getName();
			inputStream = fsFile.getInputStream();
		} else {
			throw new EsupSignatureException("Impossible de générer le zip, la demande n'est pas signée");
		}
		if(inputStream != null) {
			byte[] fileByte = inputStream.readAllBytes();
			inputStreams.put(new ByteArrayInputStream(fileByte), name);
			int i = 0;
			for(Document document : signRequest.getAttachments()) {
				inputStreams.put(document.getInputStream(), i + "_" + document.getFileName());
				i++;
			}

			ByteArrayOutputStream auditTrail = auditTrailService.generateAuditTrailPdf(signRequest, httpServletRequest, httpServletResponse);
			inputStreams.put(new ByteArrayInputStream(auditTrail.toByteArray()), "dossier-de-preuve.pdf");

			Reports reports = validationService.validate(new ByteArrayInputStream(fileByte), null);
			if(reports != null) {
				ByteArrayOutputStream reportByteArrayOutputStream = new ByteArrayOutputStream();
				fopService.generateSimpleReport(reports.getXmlSimpleReport(), reportByteArrayOutputStream);
				inputStreams.put(new ByteArrayInputStream(reportByteArrayOutputStream.toByteArray()), "rapport-signature.pdf");

			}
			return fileService.zipDocuments(inputStreams);
		}
		return null;
	}

	/**
     * Fournit document donné via son id.
     *
     * @param documentId L'identifiant du document à télécharger.
     * @param httpServletResponse L'objet HttpServletResponse dans lequel le contenu du fichier doit être écrit.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'écriture dans la réponse HTTP.
     * @throws EsupSignatureRuntimeException Si le téléchargement du fichier est interdit en fonction des règles métier (par exemple : workflow interdit les téléchargements avant sa
     *  fin).
     */
    @Transactional
	public void getFileResponse(Long documentId, HttpServletResponse httpServletResponse) throws IOException {
		Document document = documentService.getById(documentId);
		SignRequest signRequest = getById(document.getParentId());
		if(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
				.stream()
				.noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"))
			&&
				signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd()) && !signRequest.getStatus().equals(SignRequestStatus.completed)) {
			throw new EsupSignatureRuntimeException("Téléchargement interdit avant la fin du circuit");
		}
		webUtilsService.copyFileStreamToHttpResponse(document.getFileName(), document.getContentType(), "attachment", document.getInputStream(), httpServletResponse);
	}

	/**
     * Récupère la liste des pièces jointes associées à une demande de signature donnée.
     *
     * @param signRequestId l'identifiant de la demande de signature pour laquelle récupérer les pièces jointes
     * @return une liste de documents représentant les pièces jointes associées à la demande de signature
     */
    @Transactional
	public List<Document> getAttachments(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return new ArrayList<>(signRequest.getAttachments());
	}

	/**
     * Relance une notification pour une demande de signature si les conditions sont remplies.
     *
     * @param id l'identifiant de la demande de signature
     * @param userEppn l'identifiant eppn de l'utilisateur demandant la relance
     * @return un booléen indiquant si la notification a été relancée avec succès
     * @throws EsupSignatureMailException si une erreur survient lors de l'envoi de l'email
     */
    @Transactional
	public boolean replayNotif(Long id, String userEppn) throws EsupSignatureMailException {
		SignRequest signRequest = this.getById(id);
		if (isDisplayNotif(signRequest, userEppn)) {
			List<String> recipientEmails = new ArrayList<>();
			List<Recipient> recipients = getCurrentRecipients(signRequest);
			for (Recipient recipient : recipients) {
				if (recipient.getUser() != null && recipient.getUser().getEmail() != null) {
					recipientEmails.add(recipient.getUser().getEmail());
				}
			}
			long notifTime = Long.MAX_VALUE;
			if (signRequest.getParentSignBook().getLastNotifDate() != null) {
				notifTime = Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours();
			}
			if (!recipientEmails.isEmpty() && notifTime >= globalProperties.getHoursBeforeRefreshNotif() && signRequest.getStatus().equals(SignRequestStatus.pending)) {
                for(Recipient recipient : recipients) {
                    if(recipient.getUser().getUserType().equals(UserType.external)) {
                        mailService.sendSignRequestReplayAlertOtp(otpService.generateOtpForSignRequest(signRequest.getParentSignBook().getId(), recipient.getUser().getId(), recipient.getUser().getPhone(), true), signRequest.getParentSignBook());
                    } else {
                        mailService.sendSignRequestReplayAlert(Collections.singletonList(recipient.getUser().getEmail()), signRequest.getParentSignBook());
                    }
                }
				return true;
			}
		}
		return false;
	}

	/**
     * Détermine si une notification doit être affichée pour une demande de signature donnée
     * et un utilisateur spécifique.
     *
     * @param signRequest un objet SignRequest représentant la demande de signature à évaluer
     * @param userEppn le eppn (entitlement principal name) de l'utilisateur concerné
     * @return true si une notification doit être affichée, false sinon
     */
    public boolean isDisplayNotif(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		boolean displayNotif = false;
		if (signRequest.getParentSignBook().getStatus().equals(SignRequestStatus.pending) &&
				(signRequest.getCreateBy().getEppn().equals(userEppn)
						|| (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null &&
						(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail())
						|| !Collections.disjoint(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getDashboardRoles(), user.getRoles())))
				)
				&&
				((signRequest.getParentSignBook().getLastNotifDate() == null && Duration.between(signRequest.getParentSignBook().getCreateDate().toInstant(), new Date().toInstant()).toHours() >= globalProperties.getHoursBeforeRefreshNotif()) ||
						(signRequest.getParentSignBook().getLastNotifDate() != null && Duration.between(signRequest.getParentSignBook().getLastNotifDate().toInstant(), new Date().toInstant()).toHours() >= globalProperties.getHoursBeforeRefreshNotif()))) {
			displayNotif = true;
		}
		return displayNotif;
	}

	private List<Recipient> getCurrentRecipients(SignRequest signRequest) {
		return signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> !r.getSigned()).collect(Collectors.toList());
	}

	/**
     * Récupère une liste de demandes de signature pour lesquelles le destinataire n'est pas présent
     * dans le système de gestion d'annuaire (LDAP) ou possède un type d'utilisateur externe ou Shibboleth.
     *
     * @param eppn L'identifiant unique de la personne (EPPN) ayant créé les demandes de signature.
     * @return Une liste de demandes de signature (SignRequest) correspondant aux critères de filtrage.
     */
    @Transactional
	public List<SignRequest> getRecipientNotPresentSignRequests(String eppn) {
		List<SignRequest> signRequests = signRequestRepository.findByCreateByEppnAndStatus(eppn, SignRequestStatus.pending);
		List<SignRequest> recipientNotPresentsignRequests = new ArrayList<>(signRequests);
		for(SignRequest signRequest : signRequests) {
			List<Recipient> recipients = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients();
			for(Recipient recipient : recipients) {
				User user = recipient.getUser();
				if(userService.findPersonLdapLightByUser(user) != null
						|| user.getUserType().equals(UserType.external)
						|| user.getUserType().equals(UserType.shib)) {
					recipientNotPresentsignRequests.remove(signRequest);
				}
			}
		}
		return recipientNotPresentsignRequests;
	}

	/**
     * Retourne une liste de paramètres de demande de signature pour l'utilisateur spécifié et l'identifiant donné.
     *
     * @param id L'identifiant de la demande de signature.
     * @param userEppn L'identifiant EPPN de l'utilisateur pour lequel les paramètres de signature sont à récupérer.
     * @return Une liste de paramètres de demande de signature (SignRequestParams) que l'utilisateur peut utiliser, en se basant sur l'état actuel du flux de travail et de la demande
     *  de signature.
     */
    @Transactional
	public List<SignRequestParams> getToUseSignRequestParams(Long id, String userEppn) {
		User user = userService.getByEppn(userEppn);
		List<SignRequestParams> toUserSignRequestParams = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int signOrderNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
		if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
			if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAllSignToComplete()) {
				for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
					if (!signRequest.getRecipientHasSigned().isEmpty() && signRequest.getRecipientHasSigned().get(recipient) != null && !signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none)) {
						return toUserSignRequestParams;
					}
				}
			}
			List<SignRequestParams> signRequestParamsForCurrentStep = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().stream().filter(signRequestParams -> signRequestParams.getSignDocumentNumber().equals(signOrderNumber)).toList();
			for(SignRequestParams signRequestParams : signRequestParamsForCurrentStep) {
				if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().anyMatch(recipient -> recipient.getUser().equals(user))) {
					toUserSignRequestParams.add(signRequestParams);
				}
			}
		}
		return toUserSignRequestParams;
	}

	/**
     * Récupère un fichier à valider associé à une demande de signature.
     *
     * @param id l'identifiant de la demande de signature (SignRequest) à traiter
     * @return un flux d'entrée (`InputStream`) du fichier à valider, ou `null` si aucun fichier n'est disponible
     * @throws IOException si une erreur d'entrée/sortie se produit lors de la manipulation du fichier
     */
    @Transactional
	public InputStream getToValidateFile(long id) throws IOException {
		SignRequest signRequest = getById(id);
		if(signRequest != null) {
			Document toValideDocument = signRequest.getLastSignedDocument();
			if (toValideDocument != null) {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				IOUtils.copy(toValideDocument.getInputStream(), outputStream);
				outputStream.close();
				return new ByteArrayInputStream(outputStream.toByteArray());
			}
		}
		return null;
	}

	/**
     * Récupère et transforme toutes les données disponibles en format JSON en fonction de l'access token fourni.
     *
     * @param xApikey Le token d'accès utilisé pour déterminer les workflows accessibles ou pour récupérer l'ensemble des données.
     * @return Une chaîne de caractères représentant les données récupérées en format JSON.
     * @throws JsonProcessingException Si une erreur se produit lors de la conversion des données en JSON.
     */
    @Transactional
	public String getAllToJSon(String xApikey) throws JsonProcessingException {
		WsAccessToken wsAccessToken = wsAccessTokenRepository.findByToken(xApikey);
		if(wsAccessToken == null || wsAccessToken.getWorkflows().isEmpty()) {
			return objectMapper.writeValueAsString(signRequestRepository.findAllForWs());
		}
		return objectMapper.writeValueAsString(signRequestRepository.findAllByToken(wsAccessToken));
	}

	/**
     * Vérifie si une alerte de pièce jointe doit être déclenchée pour une demande de signature donnée.
     *
     * Cette méthode évalue si la demande de signature possède des alertes de pièce jointe actives dans
     * l'étape actuelle de son workflow, tout en n'ayant aucune pièce jointe associée.
     *
     * @param signRequest l'objet SignRequest pour lequel l'alerte de pièce jointe doit être vérifiée
     * @return true si une alerte de pièce jointe doit être déclenchée, false sinon
     */
    public boolean isAttachmentAlert(SignRequest signRequest) {
		boolean attachmentAlert = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& (
			(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentAlert())
				||
				(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentAlert() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentAlert())
			)
		&& signRequest.getAttachments().isEmpty()) {
			attachmentAlert = true;
		}
		return attachmentAlert;
	}

	/**
     * Vérifie si une pièce jointe est requise pour une demande de signature donnée.
     *
     * Cette méthode détermine si une pièce jointe est requise en fonction des paramètres du flux de travail associé
     * et de l'étape actuelle du processus. Une pièce jointe est considérée comme requise si elle est nécessaire
     * soit au niveau de l'étape de workflow, soit au niveau de l'étape actuelle de traitement,
     * et si aucune pièce jointe n'est actuellement associée à cette demande de signature.
     *
     * @param signRequest La demande de signature pour laquelle la vérification est effectuée
     * @return true si une pièce jointe est requise ; false sinon
     */
    public boolean isAttachmentRequire(SignRequest signRequest) {
		boolean attachmentRequire = false;
		if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null
			&& (
			(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getAttachmentRequire())
				||
				(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentRequire() != null
				&& signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getAttachmentRequire())
			)
		&& signRequest.getAttachments().isEmpty()) {
			attachmentRequire = true;
		}
		return attachmentRequire;
	}

	/**
     * Marque les avertissements de requêtes de signature comme lus pour un utilisateur authentifié.
     *
     * @param authUserEppn l'identifiant EPPN de l'utilisateur authentifié
     */
    @Transactional
	public void warningReaded(String authUserEppn) {
		User authUser = userService.getByEppn(authUserEppn);
		List<SignRequest> oldSignRequests = signRequestRepository.findByCreateByEppnAndOlderPending(authUser.getId(), globalProperties.getNbDaysBeforeWarning());
		for (SignRequest signRequest : oldSignRequests) {
			signRequest.setWarningReaded(true);
		}
	}

	/**
     * Anonymise les demandes de signature créées par un utilisateur en remplaçant
     * l'utilisateur d'origine par un utilisateur anonyme.
     *
     * @param userEppn le eppn de l'utilisateur dont les données doivent être anonymisées
     * @param anonymous l'utilisateur anonyme utilisé pour remplacer l'utilisateur d'origine
     */
    @Transactional
	public void anonymize(String userEppn, User anonymous) {
		for(SignRequest signRequest : signRequestRepository.findByCreateByEppn(userEppn)) {
			signRequest.setCreateBy(anonymous);
		}
	}

	/**
     * Récupère une liste de commentaires de type Post-it pour un SignRequest donné.
     *
     * @param id l'identifiant du SignRequest à partir duquel les commentaires de type Post-it seront récupérés
     * @return une liste de commentaires de type Post-it associés au SignRequest spécifié
     */
    @Transactional
	public List<Comment> getPostits(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(Comment::getPostit).collect(Collectors.toList());
	}

	/**
     * Récupère la liste des commentaires associés à une demande de signature,
     * en excluant les commentaires type "post-it" et ceux ayant un numéro d'étape.
     *
     * @param id l'identifiant de la demande de signature associée
     * @return une liste de commentaires filtrée, excluant les commentaires "post-it"
     * et ceux ayant un numéro d'étape
     */
    @Transactional
	public List<Comment> getComments(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(comment -> !comment.getPostit() && comment.getStepNumber() == null).collect(Collectors.toList());
	}

	/**
     * Récupère une liste d'emplacements associés à l'identifiant spécifié,
     * en filtrant ceux qui possèdent un numéro d'étape défini.
     *
     * @param id l'identifiant de la demande de signature dont les commentaires doivent être récupérés
     * @return une liste de commentaires contenant uniquement ceux avec un numéro d'étape défini
     */
    @Transactional
	public List<Comment> getSpots(Long id) {
		SignRequest signRequest = getById(id);
		return signRequest.getComments().stream().filter(comment -> comment.getStepNumber() != null).collect(Collectors.toList());
	}

	/**
     * Récupère une représentation JSON d'une entité SignRequest à partir de son identifiant.
     *
     * @param id l'identifiant unique de l'entité SignRequest
     * @return une chaîne de caractères représentant l'objet SignRequest sous forme de JSON
     * @throws JsonProcessingException si une erreur survient lors de la conversion de l'objet en JSON
     */
    @Transactional
	public String getJson(Long id) throws JsonProcessingException {
		SignRequest signRequest = getById(id);
		return objectMapper.writeValueAsString(signRequest);
	}

	/**
     * Récupère l'identifiant du parent d'une requête de signature si elle est unique dans son SignBook parent.
     *
     * @param id l'identifiant de la requête de signature pour laquelle on veut récupérer l'identifiant du parent
     * @return l'identifiant du parent si la requête de signature est la seule dans son livre de signatures parent,
     *         sinon retourne null
     */
    @Transactional
	public Long getParentIdIfSignRequestUnique(Long id) {
		SignRequest signRequest = getById(id);
		if(signRequest.getParentSignBook().getSignRequests().size() == 1) {
			return signRequest.getParentSignBook().getId();
		} else {
			return null;
		}
	}

	/**
     * Récupère le dossier de preuve sous forme de chaîne JSON pour une demande de signature donnée.
     *
     * @param id l'identifiant de la demande de signature dont on souhaite récupérer l'audit trail
     * @return une chaîne JSON représentant l'audit trail de la demande de signature si elle existe,
     *         sinon une chaîne vide
     * @throws JsonProcessingException si une erreur survient lors de la conversion en JSON
     */
    @Transactional
	public String getAuditTrailJson(Long id) throws JsonProcessingException {
		SignRequest signRequest = getById(id);
		if(signRequest != null) {
			return objectMapper.writeValueAsString(signRequest.getAuditTrail());
		} else {
			logger.warn("audit trail not found for " + id);
			return "";
		}
	}

	/**
     * Récupère une liste de demandes de signature associées à un ID spécifique et un destinataire donné.
     *
     * @param id L'identifiant unique de la demande de signature à rechercher.
     * @param userEppn Le nom principal utilisateur (userEppn) du destinataire associé à la demande.
     * @return Une liste d'objets SignRequest correspondant aux critères de recherche, ou une liste vide si aucun résultat n'est trouvé.
     */
    public List<SignRequest> getByIdAndRecipient(Long id, String userEppn) {
		return signRequestRepository.findByIdAndRecipient(id, userEppn);
	}

	/**
     * Marque une demande de signature comme "vue" par un utilisateur spécifique.
     *
     * @param signRequestId l'identifiant unique de la demande de signature à marquer comme vue
     * @param userEppn l'identifiant ePPN (eduPersonPrincipalName) de l'utilisateur qui a vu la demande de signature
     */
    @Transactional
	public void viewedBy(Long signRequestId, String userEppn) {
		User user = userService.getByEppn(userEppn);
		SignRequest signRequest = getById(signRequestId);
		signRequest.getViewedBy().add(user);
	}

	private String smallCheckPDFA(byte[] bytes) {
		List<String> results = pdfService.checkPDFA(bytes, true);
		StringBuilder pdfaCheck = new StringBuilder();
		for(String result : results) {
			if(result.startsWith("6") && !pdfaCheck.toString().contains(result.substring(0, 5))) {
				pdfaCheck.append(result, 0, 5).append(",");
			}
		}
		return pdfaCheck.toString();
	}

    /**
     * Récupère les destinataires externes associés à une requête de signature.
     *
     * @param signRequestId L'identifiant de la requête de signature pour laquelle
     *                      les destinataires externes doivent être récupérés.
     * @return Une liste d'objets RecipientWsDto contenant des informations sur les
     *         destinataires externes, notamment leur identifiant et leur email.
     */
    @Transactional
    public List<RecipientWsDto> getExternalRecipients(Long signRequestId) {
		SignRequest signRequest = getById(signRequestId);
		return signRequest.getParentSignBook().getTeam().stream().filter(user -> user.getUserType().equals(UserType.external)).map(user -> new RecipientWsDto(user.getId(), user.getEmail())).collect(Collectors.toList());
    }

	/**
     * Récupère une liste des étapes d'une demande de signature sous forme d'objets SignRequestStepsDto.
     *
     * @param id L'identifiant unique de la demande de signature (SignRequest).
     * @return Une liste de {@code SignRequestStepsDto} qui représentent les étapes de la demande de signature,
     *         avec les informations sur les actions des destinataires et les métadonnées associées.
     */
    @Transactional
	public List<SignRequestStepsDto> getStepsDto(Long id) {
		List<SignRequestStepsDto> signRequestStepsDtos = new ArrayList<>();
		SignRequest signRequest = getById(id);
		int i = 1;
		for(LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
			SignRequestStepsDto signRequestStepsDto = new SignRequestStepsDto();
			signRequestStepsDto.setStepNumber(i);
			signRequestStepsDto.setSignType(liveWorkflowStep.getSignType().name());
			signRequestStepsDto.setAllSignToComplete(liveWorkflowStep.getAllSignToComplete());
			for(Recipient recipient : liveWorkflowStep.getRecipients()) {
				RecipientsActionsDto recipientsActionsDto = new RecipientsActionsDto();
				recipientsActionsDto.setStepNumber(i);
				Action action = signRequest.getRecipientHasSigned().get(recipient);
				if(action != null) {
					recipientsActionsDto.setActionDate(action.getDate());
					recipientsActionsDto.setActionType(action.getActionType().name());
					if (action.getActionType().equals(ActionType.refused)) {
						Optional<Log> refuseLog = logService.getRefuseLogs(signRequest.getParentSignBook().getId()).stream().filter(l -> l.getComment() != null).findAny();
						refuseLog.ifPresent(l -> recipientsActionsDto.setRefuseComment(l.getComment()));
					}
				}
				User user = recipient.getUser();
				recipientsActionsDto.setUserEppn(user.getEppn());
				recipientsActionsDto.setUserName(user.getName());
				recipientsActionsDto.setUserFirstname(user.getFirstname());
				recipientsActionsDto.setUserEmail(user.getEmail());
				AuditTrail auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
				if(auditTrail != null && auditTrail.getAuditSteps().size() >= i) {
					AuditStep auditStep = auditTrailService.getAuditTrailByToken(signRequest.getToken()).getAuditSteps().get(i - 1);
					recipientsActionsDto.setSignPageNumber(auditStep.getPage());
					recipientsActionsDto.setSignPosX(auditStep.getPosX());
					recipientsActionsDto.setSignPosY(auditStep.getPosY());
				}
				signRequestStepsDto.getRecipientsActions().add(recipientsActionsDto);
			}
			signRequestStepsDtos.add(signRequestStepsDto);
			i++;
		}
		return signRequestStepsDtos;
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
     * Nettoie les paramètres de requêtes de signature associés à un identifiant donné.
     *
     * Cette méthode supprime les paramètres de requêtes de signature si les conditions
     * définies sont remplies. Elle vérifie si la liste des identifiants de signature
     * dans le rapport simple est vide et si le workflow associé est nul,
     * avant de vider les listes correspondantes de paramètres.
     *
     * @param id l'identifiant unique de la requête de signature à traiter
     * @throws IOException si une erreur d'entrée/sortie survient durant le traitement
     */
    @Transactional
	public void cleanSignRequestParams(Long id) throws IOException {
		SignRequest signRequest = getById(id);
		Reports reports = validate(id);
		if(reports.getSimpleReport().getSignatureIdList().isEmpty() && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() == null) {
			signRequest.getSignRequestParams().clear();
			signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().clear();
		}
	}

	/**
     * Vérifie si l'utilisateur actuel a signé la demande de signature.
     *
     * @param signRequest la demande de signature à vérifier
     * @param userEppn l'identifiant EPPN de l'utilisateur pour vérifier l'état de signature
     * @return true si l'utilisateur identifié par le EPPN a signé la demande, false sinon
     */
    public boolean isCurrentUserAsSigned(SignRequest signRequest, String userEppn) {
		User user = userService.getByEppn(userEppn);
		return signRequest.getRecipientHasSigned().entrySet().stream().anyMatch(rhs -> rhs.getValue().getActionType().equals(ActionType.signed) && rhs.getKey().getUser().getEppn().equals(user.getEppn()));
	}
}
