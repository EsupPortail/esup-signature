package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.SignRequestParamsWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.dto.view.UserDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.exception.*;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.OidcOtpSecurityService;
import org.esupportail.esupsignature.service.security.oauth.franceconnect.FranceConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.oauth.proconnect.ProConnectSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service permettant de gérer les SignBooks.
 *
 * @author David Lemaignent
 */
@Service
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    private final GlobalProperties globalProperties;
    private final MessageSource messageSource;
    private final AuditTrailService auditTrailService;
    private final SignBookRepository signBookRepository;
    private final SignRequestService signRequestService;
    private final UserService userService;
    private final FsAccessFactoryService fsAccessFactoryService;
    private final WebUtilsService webUtilsService;
    private final FileService fileService;
    private final PdfService pdfService;
    private final WorkflowService workflowService;
    private final MailService mailService;
    private final WorkflowStepService workflowStepService;
    private final LiveWorkflowService liveWorkflowService;
    private final LiveWorkflowStepService liveWorkflowStepService;
    private final DataService dataService;
    private final LogService logService;
    private final TargetService targetService;
    private final UserPropertieService userPropertieService;
    private final CommentService commentService;
    private final OtpService otpService;
    private final DataRepository dataRepository;
    private final WorkflowRepository workflowRepository;
    private final UserShareService userShareService;
    private final RecipientService recipientService;
    private final DocumentService documentService;
    private final SignRequestParamsService signRequestParamsService;
    private final PreFillService preFillService;
    private final ReportService reportService;
    private final ActionService actionService;
    private final SignRequestParamsRepository signRequestParamsRepository;
    private final ObjectMapper objectMapper;
    private final SignWithService signWithService;
    private final SmsProperties smsProperties;
    private final SignService signService;

    public SignBookService(GlobalProperties globalProperties, MessageSource messageSource, AuditTrailService auditTrailService, SignBookRepository signBookRepository, SignRequestService signRequestService, UserService userService, FsAccessFactoryService fsAccessFactoryService, WebUtilsService webUtilsService, FileService fileService, PdfService pdfService, WorkflowService workflowService, MailService mailService, WorkflowStepService workflowStepService, LiveWorkflowService liveWorkflowService, LiveWorkflowStepService liveWorkflowStepService, DataService dataService, LogService logService, TargetService targetService, UserPropertieService userPropertieService, CommentService commentService, OtpService otpService, DataRepository dataRepository, WorkflowRepository workflowRepository, UserShareService userShareService, RecipientService recipientService, DocumentService documentService, SignRequestParamsService signRequestParamsService, PreFillService preFillService, ReportService reportService, ActionService actionService, SignRequestParamsRepository signRequestParamsRepository, ObjectMapper objectMapper, SignWithService signWithService, SmsProperties smsProperties, SignService signService) {
        this.globalProperties = globalProperties;
        this.messageSource = messageSource;
        this.auditTrailService = auditTrailService;
        this.signBookRepository = signBookRepository;
        this.signRequestService = signRequestService;
        this.userService = userService;
        this.fsAccessFactoryService = fsAccessFactoryService;
        this.webUtilsService = webUtilsService;
        this.fileService = fileService;
        this.pdfService = pdfService;
        this.workflowService = workflowService;
        this.mailService = mailService;
        this.workflowStepService = workflowStepService;
        this.liveWorkflowService = liveWorkflowService;
        this.liveWorkflowStepService = liveWorkflowStepService;
        this.dataService = dataService;
        this.logService = logService;
        this.targetService = targetService;
        this.userPropertieService = userPropertieService;
        this.commentService = commentService;
        this.otpService = otpService;
        this.dataRepository = dataRepository;
        this.workflowRepository = workflowRepository;
        this.userShareService = userShareService;
        this.recipientService = recipientService;
        this.documentService = documentService;
        this.signRequestParamsService = signRequestParamsService;
        this.preFillService = preFillService;
        this.reportService = reportService;
        this.actionService = actionService;
        this.signRequestParamsRepository = signRequestParamsRepository;
        this.objectMapper = objectMapper;
        this.signWithService = signWithService;
        this.smsProperties = smsProperties;
        this.signService = signService;
    }

    /**
     * Compte le nombre de signBooks associés à un workflow donné.
     *
     * @param workflowId l'identifiant unique du workflow pour lequel compter les signBooks
     * @return le nombre total de signBooks associés au workflow spécifié
     */
    @Transactional
    public int countSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).get();
        return signBookRepository.countByLiveWorkflowWorkflow(workflow);
    }

    /**
     * Calcule le nombre de livres-signatures à signer pour un utilisateur donné.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur pour lequel calculer le nombre de livres-signatures à signer
     * @return le nombre total de livres-signatures à signer pour l'utilisateur spécifié
     */
    @Transactional
    public Long nbToSignSignBooks(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return signBookRepository.countToSign(user);
    }

    /**
     * Récupère une page des SignBooks pour les gestionnaires en fonction des filtres fournis.
     *
     * @param statusFilter le filtre basé sur le statut de la demande de signature
     * @param recipientsFilter le filtre basé sur les destinataires
     * @param workflowId l'identifiant du workflow pour filtrer les résultats
     * @param docTitleFilter le filtre basé sur le titre du document
     * @param creatorFilter le filtre basé sur le créateur (adresse e-mail)
     * @param dateFilter le filtre basé sur la date (format attendu : yyyy-MM-dd)
     * @param pageable les informations de pagination pour les résultats
     * @return une page contenant la liste des SignBooks correspondant aux critères donnés
     */
    @Transactional
    public Page<SignBook> getSignBooksForManagers(SignRequestStatus statusFilter, String recipientsFilter, Long workflowId, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable, String userEppn, Boolean hided) {
        User user = userService.getByEppn(userEppn);
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getUserByEmail(creatorFilter);
        }
        User userFilter = null;
        if(recipientsFilter != null && !recipientsFilter.equals("%") && !recipientsFilter.isEmpty()) {
            userFilter = userService.getUserByEmail(recipientsFilter);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(9999, Calendar.DECEMBER, 31);
        Date startDateFilter = new Date(0);
        Date endDateFilter = calendar.getTime();
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date formattedDate = formatter.parse(dateFilter);
                LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
                LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
                startDateFilter = Timestamp.valueOf(startLocalDateTime);
                endDateFilter = Timestamp.valueOf(endLocalDateTime);
            } catch (ParseException e) {
                logger.error("unable to parse date : " + dateFilter);
            }
        }

        if(hided) {
            return signBookRepository.findByWorkflowNameHided(userFilter, statusFilter, SignRequestStatus.deleted.equals(statusFilter), workflowId, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable, user);
        } else {
            return signBookRepository.findByWorkflowName(userFilter, statusFilter, SignRequestStatus.deleted.equals(statusFilter), workflowId, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable, user);
        }
    }

    /**
     * Récupère une page de SignBooks en fonction des filtres et des paramètres fournis.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur pour lequel les SignBooks sont récupérés
     * @param authUserEppn l'identifiant EPPN de l'utilisateur authentifié effectuant la requête
     * @param statusFilter un filtre pour le statut du SignBook (par exemple "toSign", "signedByMe", "refusedByMe", etc.)
     * @param recipientsFilter un filtre pour les destinataires des SignBooks
     * @param workflowFilter un filtre pour le type de workflow des SignBooks
     * @param docTitleFilter un filtre pour le titre des documents associés aux SignBooks
     * @param creatorFilter un filtre pour l'identifiant EPPN du créateur des SignBooks
     * @param dateFilter une date pour filtrer les SignBooks sur une journée spécifique (au format "yyyy-MM-dd")
     * @param pageable un objet Pageable définissant la pagination des résultats
     * @return une page de SignBooks correspondant aux filtres et paramètres spécifiés
     */
    @Transactional
    public Page<SignBook> getSignBooks(String userEppn, String authUserEppn, String statusFilter, String recipientsFilter, String workflowFilter, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        User user = userService.getByEppn(userEppn);
        Calendar calendar = Calendar.getInstance();
        calendar.set(9999, Calendar.DECEMBER, 31);
        Date startDateFilter = new Date(0);
        Date endDateFilter = calendar.getTime();
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date formattedDate = formatter.parse(dateFilter);
                LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
                LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
                startDateFilter = Timestamp.valueOf(startLocalDateTime);
                endDateFilter = Timestamp.valueOf(endLocalDateTime);
            } catch (ParseException e) {
                logger.error("unable to parse date : " + dateFilter);
            }
        }
        Page<SignBook> signBooks;
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getByEppn(creatorFilter);
        }
        User userFilter = null;
        if(recipientsFilter != null && !recipientsFilter.equals("%") && !recipientsFilter.isEmpty()) {
            userFilter = userService.getUserByEmail(recipientsFilter);
        }
        if(statusFilter.isEmpty() || statusFilter.equals("all")) {
            if(userFilter != null) {
                signBooks = signBookRepository.findByRecipientAndCreateByEppnIndexed(userFilter, user, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
            } else {
                signBooks = signBookRepository.findByRecipientAndCreateByEppnIndexed(user, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
            }
        } else if(statusFilter.equals("toSign"))  {
            signBooks = signBookRepository.findToSign(user, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
        } else if(statusFilter.equals("signedByMe")) {
            signBooks = signBookRepository.findByRecipientAndActionTypeNotDeleted(user, ActionType.signed, workflowFilter, docTitleFilter, creatorFilterUser, pageable);
        } else if(statusFilter.equals("refusedByMe")) {
            signBooks = signBookRepository.findByRecipientAndActionTypeNotDeleted(user, ActionType.refused, workflowFilter, docTitleFilter, creatorFilterUser, pageable);
        } else if(statusFilter.equals("followByMe")) {
            signBooks = signBookRepository.findByViewersContaining(user, pageable);
        } else if(statusFilter.equals("sharedSign")) {
            signBooks = signBookRepository.findOnShareByEppn(user.getEppn(), userFilter, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
        } else if(statusFilter.equals("hided")) {
            signBooks = signBookRepository.findByHidedById(user, pageable);
        } else if(statusFilter.equals("empty")) {
            signBooks = signBookRepository.findEmpty(user, pageable);
        } else if(statusFilter.equals("deleted")) {
            signBooks = signBookRepository.findByCreateByIdDeleted(user, pageable);
        } else if(statusFilter.equals("completed")) {
            signBooks = signBookRepository.findCompleted(user, pageable);
        } else {
            signBooks = signBookRepository.findByCreateByIdAndStatusAndSignRequestsNotNull(user, SignRequestStatus.valueOf(statusFilter), pageable);
        }
        if(!userEppn.equals(authUserEppn)) {
            List<SignBook> sharedSignBooks = filterByUserShares(userEppn, authUserEppn, signBooks.getContent());
            signBooks = new PageImpl<>(sharedSignBooks, pageable, sharedSignBooks.size());
        }
        for (SignBook signBook : signBooks.getContent()) {
            if(!signBook.getSignRequests().isEmpty()) {
                signBook.setDeleteableByCurrentUser(signRequestService.isDeletetable(signBook.getSignRequests().get(0), userEppn) && (signBook.getCreateBy().getEppn().equals(userEppn)));
            }
        }
        return signBooks;
    }

    /**
     * Récupère une page de tous les SignBooks filtrés selon divers critères.
     *
     * @param statusFilter      Le filtre sur le statut des SignBooks (peut être null).
     * @param workflowFilter    Le filtre sur le workflow des SignBooks (peut être null).
     * @param docTitleFilter    Le filtre sur le titre du document des SignBooks (peut être null).
     * @param creatorFilter     Le filtre sur le créateur des SignBooks (adresse email, peut être null).
     * @param dateFilter        Le filtre sur la date des SignBooks au format "yyyy-MM-dd" (peut être null).
     * @param pageable          L'objet Pageable contenant les informations de pagination.
     * @return                  Une page contenant les SignBooks répondant aux critères spécifiés.
     */
    @Transactional
    public Page<SignBook> getAllSignBooks(String statusFilter, String workflowFilter, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(9999, Calendar.DECEMBER, 31);
        Date startDateFilter = new Date(0);
        Date endDateFilter = calendar.getTime();
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date formattedDate = formatter.parse(dateFilter);
                LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
                LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
                startDateFilter = Timestamp.valueOf(startLocalDateTime);
                endDateFilter = Timestamp.valueOf(endLocalDateTime);
            } catch (ParseException e) {
                logger.error("unable to parse date : " + dateFilter);
            }
        }
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getUserByEmail(creatorFilter);
        }
        SignRequestStatus status = null;
        if(statusFilter != null && !statusFilter.isEmpty()) {
            status = SignRequestStatus.valueOf(statusFilter);
        }
        return signBookRepository.findSignBooksAllPaged(status, SignRequestStatus.deleted.equals(status), workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
    }

    /**
     * Filtre une liste de SignBook en fonction des partages d'utilisateur et des autorisations associées.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur pour lequel les SignBooks doivent être vérifiés
     * @param authUserEppn l'identifiant EPPN de l'utilisateur autorisé à effectuer l'opération
     * @param signBooksToSignToCheck la liste de SignBooks à vérifier
     * @return une liste de SignBooks filtrés qui respectent les critères des partages d'utilisateur
     */
    @Transactional
    public List<SignBook> filterByUserShares(String userEppn, String authUserEppn, List<SignBook> signBooksToSignToCheck) {
        List<SignBook> signBooksToSign = new ArrayList<>();
        for(SignBook signBook : signBooksToSignToCheck) {
            if(!signBook.getSignRequests().isEmpty() && checkAllShareTypesForSignRequest(userEppn, authUserEppn, signBook.getId())) {
                signBooksToSign.add(signBook);
            }
        }
        List<UserShare> userShares = userShareService.getUserSharesByUser(userEppn);
        List<Workflow> workflows = new ArrayList<>();
        workflows.addAll(userShares.stream().map(UserShare::getWorkflow).filter(Objects::nonNull).toList());
        workflows.addAll(
                userShares.stream().map(UserShare::getForm).filter(Objects::nonNull)
                        .toList()
                        .stream().map(Form::getWorkflow).filter(Objects::nonNull).toList());
        if (userShares.stream().noneMatch(us -> us.getAllSignRequests() != null && us.getAllSignRequests())) {
            signBooksToSign = signBooksToSign.stream().filter(signBook -> workflows.contains(signBook.getLiveWorkflow().getWorkflow())).collect(Collectors.toList());
        }
        return signBooksToSign;
    }

    /**
     * Crée un SignBook auto-signé pour un utilisateur spécifié en utilisant son identifiant
     * et met à jour ses propriétés, étapes et statut.
     *
     * @param signBookId l'identifiant du SignBook à créer ou mettre à jour
     * @param userEppn l'identifiant de l'utilisateur (EPPN) pour lequel le SignBook
     *                 est auto-signé
     */
    @Transactional
    public void createSelfSignBook(Long signBookId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        WorkflowStepDto workflowStepDto = new WorkflowStepDto();
        workflowStepDto.setRepeatable(false);
        workflowStepDto.setRepeatableSignType(null);
        workflowStepDto.setAutoSign(false);
        workflowStepDto.setAllSignToComplete(false);
        workflowStepDto.setSignType(null);
        recipientService.addRecipientInStep(workflowStepDto, user.getEmail());
        importWorkflowFromWorkflowStepDto(signBookId, Collections.singletonList(workflowStepDto), userEppn);
        signBook.setStatus(SignRequestStatus.draft);
        pendingSignBook(signBook, null, userEppn, userEppn, false, true);
    }

    /**
     * Termine le processus de téléchargement du carnet de signatures.
     * Met à jour le statut du carnet de signatures et génère un sujet si nécessaire.
     *
     * @param signBookId l'identifiant du carnet de signatures
     * @param userEppn l'identifiant unique de l'utilisateur dans le système (EPPN)
     */
    @Transactional
    public void finishSignBookUpload(Long signBookId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(!StringUtils.hasText(signBook.getSubject())) {
            signBook.setSubject(generateName(null, null, user, false, false, signBookId));
        }
        signBook.setStatus(SignRequestStatus.draft);
    }


    /**
     * Crée un nouveau SignBook avec les paramètres fournis et l'enregistre dans le dépôt.
     *
     * @param subject Le sujet du SignBook.
     * @param workflow Le workflow associé au SignBook.
     * @param workflowName Le nom du workflow. Si vide, un nom sera généré automatiquement.
     * @param userEppn L'identifiant eppn de l'utilisateur créant le SignBook.
     * @param geneateName Indique si un nom doit être généré automatiquement pour le sujet.
     * @param comment Une description ou un commentaire pour le SignBook.
     * @return Le SignBook nouvellement créé.
     */
    @Transactional
    public SignBook createSignBook(String subject, Workflow workflow, String workflowName, String userEppn, boolean geneateName, String comment) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = new SignBook();
        if(!StringUtils.hasText(workflowName)) {
            if(workflow != null) {
                if(workflow.getDescription() != null && !workflow.getDescription().isEmpty()) {
                    workflowName = workflow.getDescription();
                } else if(workflow.getName() != null && !workflow.getName().isEmpty()) {
                    workflowName = workflow.getName();
                } else {
                    workflowName = "Sans nom";
                }
            }
        }
        signBook.setStatus(SignRequestStatus.uploading);
        signBook.setWorkflowName(workflowName);
        signBook.setCreateBy(user);
        addToTeam(signBook, user.getEppn());
        signBook.setCreateDate(new Date());
        signBook.setLiveWorkflow(liveWorkflowService.create(workflowName, workflow));
        signBook.setDescription(comment);
        signBook.setSubject(subject);
        signBookRepository.save(signBook);
        if(geneateName) {
            subject = generateName(null, workflow, user, false, false, signBook.getId());
        }
        signBook.setSubject(subject);
        return signBook;
    }

    /**
     * Démarre un processus de signature simple.
     *
     * @param id l'identifiant du carnet de signatures à démarrer
     * @param pending indique si le processus de signature démarre en attente
     * @param steps une liste d'étapes représentant les étapes du workflow
     * @param userEppn le principal utilisateur (eppn) initiant le processus de signature
     * @param authUserEppn l'utilisateur authentifié (eppn) utilisé pour l'action
     * @param multiSign indique si le processus permet la signature multiple
     * @param singleSignWithAnnotation indique si la signature unique est autorisée avec annotation
     * @throws EsupSignatureRuntimeException si une erreur survient durant le démarrage du processus
     */
    @Transactional
    public void startFastSignBook(Long id, Boolean pending, List<WorkflowStepDto> steps, String userEppn, String authUserEppn, boolean multiSign, boolean singleSignWithAnnotation) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        if(StringUtils.hasText(steps.get(0).getTitle())) {
            signBook.setSubject(steps.get(0).getTitle());
        }
        signBook.setForceAllDocsSign(steps.get(0).getAllSignToComplete());
        sendSignBook(signBook, pending, steps.get(0).getComment(), steps, userEppn, authUserEppn, false);
        if(steps.get(0).getRecipientsCCEmails() != null) {
            addViewers(signBook.getId(), steps.get(0).getRecipientsCCEmails());
        }
        int stepNumber = 0;
        if(steps.get(0).getUserSignFirst() != null && steps.get(0).getUserSignFirst()) {
            stepNumber = 1;
        }
        signBook.getLiveWorkflow().getLiveWorkflowSteps().get(stepNumber).setMultiSign(multiSign);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().get(stepNumber).setSingleSignWithAnnotation(singleSignWithAnnotation);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().get(stepNumber).setSignType(steps.get(0).getSignType());
        signBook.getLiveWorkflow().getLiveWorkflowSteps().get(stepNumber).setMinSignLevel(steps.get(0).getMinSignLevel());
        signBook.getLiveWorkflow().getLiveWorkflowSteps().get(stepNumber).setMaxSignLevel(steps.get(0).getMaxSignLevel());
    }

    /**
     * Met à jour un SignBook en fonction des attributs fournis.
     *
     * @param id l'identifiant unique du SignBook à mettre à jour
     * @param subject le nouveau sujet à attribuer au SignBook, s'il n'est pas vide
     * @param description la nouvelle description à attribuer au SignBook, si elle n'est pas vide
     * @param viewers une liste de viewers à ajouter au SignBook
     * @return le SignBook mis à jour
     */
    @Transactional
    public SignBook updateSignBook(Long id, String subject, String description, List<String> viewers) {
        SignBook signBook = getById(id);
        if(StringUtils.hasText(subject)) {
            signBook.setSubject(subject);
        }
        if(StringUtils.hasText(description)) {
            signBook.setDescription(description);
        }
        addViewers(id, viewers);
        return signBook;
    }

    /**
     * Met à jour un SignBook avec les informations fournies concernant la première étape.
     *
     * @param signBookId l'identifiant unique du SignBook à mettre à jour
     * @param steps une liste d'objets WorkflowStepDto contenant les informations des étapes,
     *              seule la première étape sera utilisée pour la mise à jour
     * @return le SignBook mis à jour
     */
    @Transactional
    public SignBook updateSignBookWithStep(Long signBookId, List<WorkflowStepDto> steps) {
        SignBook signBook = updateSignBook(signBookId, steps.get(0).getTitle(), steps.get(0).getDescription(), steps.get(0).getRecipientsCCEmails());
        signBook.setForceAllDocsSign(steps.get(0).getForceAllSign());
        return signBook;
    }

    /**
     * Initialise un SignBook avec un Workflow si l'utilisateur est celui qui l'a créé.
     *
     * @param signBookId l'identifiant du SignBook à initialiser
     * @param workflowId l'identifiant du Workflow à associer au SignBook
     * @param userEppn le eppn de l'utilisateur effectuant l'initialisation
     */
    @Transactional
    public void initSignBook(Long signBookId, Long workflowId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(user.equals(signBook.getCreateBy())) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
    }

    private void dispatchSignRequestParams(SignBook signBook) {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            dispatchSignRequestParams(signRequest);
        }
    }

    private void dispatchSignRequestParams(SignRequest signRequest) {
        int docNumber = signRequest.getParentSignBook().getSignRequests().indexOf(signRequest);
        if(!signRequest.getSignRequestParams().isEmpty()) {
            int i = 0;
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                if (liveWorkflowStep.getWorkflowStep() != null) {
                    WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                    if (!liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) {
                        if(!workflowStep.getSignRequestParams().isEmpty()) {
                            for (SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
                                signRequestParams.setSignDocumentNumber(docNumber);
                                for(SignRequestParams signRequestParams1 : workflowStep.getSignRequestParams()) {
                                    if(signRequestParams1.getSignPageNumber().equals(signRequestParams.getSignPageNumber())
                                            && signRequestParams1.getxPos().equals(signRequestParams.getxPos())
                                            && signRequestParams1.getyPos().equals(signRequestParams.getyPos())) {
                                        signRequestParams.setSignWidth(signRequestParams1.getSignWidth());
                                        signRequestParams.setSignHeight(signRequestParams1.getSignHeight());
                                        addSignRequestParamToStep(signRequestParams, liveWorkflowStep);
                                    }
                                }
                            }
                        } else {
                            if(signRequest.getSignRequestParams().size() > i) {
                                signRequest.getSignRequestParams().get(i).setSignDocumentNumber(docNumber);
                                addSignRequestParamToStep(signRequest.getSignRequestParams().get(i), liveWorkflowStep);
                            }
                        }
                    }
                } else if(signRequest.getSignRequestParams().size() > i) {
                    if(liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) continue;
                    addSignRequestParamToStep(signRequest.getSignRequestParams().get(i), liveWorkflowStep);
                    logger.info("add signRequestParams to liveWorkflowStep " + liveWorkflowStep.getId());
                }
                i++;
            }
        } else {
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                if(liveWorkflowStep.getWorkflowStep() != null) {
                    WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                    liveWorkflowStep.getSignRequestParams().addAll(workflowStep.getSignRequestParams());
                    for(SignRequestParams signRequestParams : workflowStep.getSignRequestParams()) {
                        addSignRequestParamToStep(signRequestParams, liveWorkflowStep);
                    }
                }
            }
        }
    }

    private void addSignRequestParamToStep(SignRequestParams signRequestParams, LiveWorkflowStep liveWorkflowStep) {
        if(liveWorkflowStep.getSignRequestParams().stream().noneMatch(s -> s.getSignPageNumber().equals(signRequestParams.getSignPageNumber())
                && s.getxPos().equals(signRequestParams.getxPos())
                && s.getyPos().equals(signRequestParams.getyPos()))) {
            liveWorkflowStep.getSignRequestParams().add(signRequestParams);
        }
    }

    /**
     * Importe un workflow à partir d'une liste de WorkflowStepDto et l'associe au SignBook spécifié.
     *
     * @param signBookId l'identifiant du SignBook auquel le workflow sera associé
     * @param steps une liste de WorkflowStepDto représentant les étapes du workflow à importer
     * @param userEppn l'identifiant unique de l'utilisateur (EPPN) effectuant l'importation
     */
    @Transactional
    public void importWorkflowFromWorkflowStepDto(Long signBookId, List<WorkflowStepDto> steps, String userEppn) {
        SignBook signBook = getById(signBookId);
        logger.info("import workflow steps in signBook " + signBook.getSubject() + " - " + signBook.getId());
        if(steps.get(0).getUserSignFirst() != null && steps.get(0).getUserSignFirst()) {
            addUserSignFirstStep(signBookId, userEppn);
        }
        workflowService.computeWorkflow(steps, signBook);
    }

    /**
     * Ajoute la signature d'un utilisateur à la première étape d'un SignBook existant.
     *
     * @param signBookId L'identifiant unique du SignBook auquel ajouter l'étape de signature.
     * @param userEppn   L'identifiant EPPN (eduPersonPrincipalName) de l'utilisateur à inclure dans l'étape de signature.
     */
    @Transactional
    public void addUserSignFirstStep(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        User user = userService.getByEppn(userEppn);
        WorkflowStepDto workflowStepDto = new WorkflowStepDto();
        recipientService.addRecipientInStep(workflowStepDto, user.getEmail());
        workflowStepDto.setSignType(SignType.signature);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(signBook, null, workflowStepDto));
    }

    /**
     * Ajoute une nouvelle étape au workflow d'un SignBook existant.
     *
     * @param signBookId L'identifiant du SignBook auquel l'étape doit être ajoutée.
     * @param steps La liste des étapes du workflow à ajouter.
     * @param authUserEppn L'identifiant utilisateur EPPN de l'utilisateur authentifié effectuant l'opération.
     * @throws EsupSignatureRuntimeException Exception levée en cas d'erreur durant la procédure.
     */
    @Transactional
    public void addNewStepToSignBook(Long signBookId, List<WorkflowStepDto> steps, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = signBookRepository.findById(signBookId).get();
        logger.info("add new workflow step to signBook " + signBook.getSubject() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, steps.get(0));
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), steps);
    }

    /**
     * Récupère un SignBook par son identifiant.
     *
     * @param id l'identifiant unique du SignBook.
     * @return le SignBook correspondant à l'identifiant donné ou null si aucun SignBook n'est trouvé.
     */
    @Transactional
    public SignBook getById(Long id) {
        return signBookRepository.findById(id).orElse(null);
    }

    /**
     * Recherche et retourne un objet SignRequest en fonction de l'identifiant fourni.
     * Si aucun SignRequest direct n'est trouvé, l'identifiant est utilisé pour chercher un SignBook
     * et, si applicable, retourne un SignRequest associé basé sur certaines conditions.
     *
     * @param id l'identifiant unique pour rechercher un SignRequest ou un SignBook
     * @return un objet SignRequest correspondant à l'identifiant ou trouvé dans un SignBook associé,
     *         sinon retourne null si aucune correspondance n'est trouvée
     */
    @Transactional
    public SignRequest search(Long id) {
        SignRequest signRequest = signRequestService.getById(id);
        if (signRequest != null) {
            return signRequest;
        } else {
            SignBook signBook = getById(id);
            if (signBook != null) {
                if (!signBook.getSignRequests().isEmpty()) {
                    if (signBook.getSignRequests().size() > 1) {
                        if (signBook.getSignRequests().stream().anyMatch(s -> s.getStatus().equals(SignRequestStatus.pending))) {
                            return signBook.getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.pending)).findFirst().orElseThrow();
                        }
                    } else {
                        return signBook.getSignRequests().get(0);
                    }
                }
            }
        }
        return null;
    }

    private List<SignBook> getByWorkflowId(Long id) {
        return signBookRepository.findByWorkflowId(id);
    }

    /**
     * Supprime un sign book et ses sign requests associées.
     *
     * @param signBookId l'identifiant du sign book à supprimer
     * @param userEppn l'identifiant épintrinique de l'utilisateur demandant la suppression
     * @return true si le sign book a été définitivement supprimé, false s'il a été marqué comme supprimé
     */
    @Transactional
    public Boolean delete(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        if(signBook == null) return false;
        if(signBook.getDeleted()) {
            deleteDefinitive(signBookId, userEppn);
            return true;
        }
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).toList();
        for(Long signRequestId : signRequestsIds) {
            signRequestService.delete(signRequestId, userEppn);
        }
        signBook.setDeleted(true);
        signBook.setUpdateDate(new Date());
        signBook.setUpdateBy(userEppn);
        logger.info("delete signbook : " + signBookId);
        return false;
    }

    /**
     * Restaure les demandes de signature supprimées associées à un SignBook spécifique.
     *
     * @param signBookId l'identifiant du SignBook contenant les demandes de signature à restaurer
     * @param userEppn le principal d'utilisateur (eppn) de l'utilisateur effectuant la restauration
     */
    @Transactional
    public void restore(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getDeleted()) {
                signRequest.setDeleted(false);
                signRequest.getParentSignBook().setDeleted(false);
                logService.create(signRequest.getId(), signRequest.getParentSignBook().getSubject(), signRequest.getParentSignBook().getWorkflowName(), signRequest.getStatus(), "Restauration par l'utilisateur", null, "SUCCESS", null, null, null, null, userEppn, userEppn);
                for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
                    targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "restored", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), userEppn, "");
                }
            }
        }
    }

    /**
     * Supprime définitivement un SignBook, ainsi que ses étapes de workflow, ses requêtes de signature associées et ses données liées.
     * Seule la création par l'utilisateur, un utilisateur système ou un administrateur permet cette suppression.
     *
     * @param signBookId l'identifiant du SignBook à supprimer
     * @param userEppn l'identifiant EPPN de l'utilisateur effectuant la demande de suppression
     * @return true si la suppression a été effectuée avec succès, false sinon
     */
    @Transactional
    public boolean deleteDefinitive(Long signBookId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(signBook != null && (signBook.getCreateBy().equals(user) || userService.getSystemUser().equals(user) || userService.getSchedulerUser().equals(user) || user.getRoles().contains("ROLE_ADMIN"))) {
            signBook.getLiveWorkflow().setCurrentStep(null);
            List<Long> liveWorkflowStepIds = signBook.getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getId).toList();
            signBook.getLiveWorkflow().getLiveWorkflowSteps().clear();
            for (Long liveWorkflowStepId : liveWorkflowStepIds) {
                liveWorkflowStepService.delete(liveWorkflowStepId);
            }
            List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).toList();
            for (Long signRequestId : signRequestsIds) {
                signRequestService.deleteDefinitive(signRequestId, userEppn);
            }
            dataService.deleteBySignBook(signBook);
            signBookRepository.delete(signBook);
            logger.info("definitive delete signbook : " + signBookId + " by " + userEppn);
            return true;
        } else {
            logger.warn("unable to definitive delete signbook : " + signBookId + " by " + userEppn);
            return false;
        }
    }

    /**
     * Vérifie si un utilisateur dispose des droits de gestion pour un SignBook spécifique.
     *
     * @param signBookId l'identifiant unique du SignBook
     * @param userEppn l'identifiant unique de l'utilisateur (eppn)
     * @return true si l'utilisateur dispose des droits de gestion, false sinon
     */
    @Transactional
    public boolean checkUserManageRights(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        if(signBook == null) return false;
        if(signBook.getSignRequests().size() == 1) {
            User user = userService.getByEppn(userEppn);
            Workflow workflow = signBook.getLiveWorkflow().getWorkflow();
            if(workflow != null) {
                if ((!signBook.getLiveWorkflow().getWorkflow().getManagers().isEmpty() && signBook.getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail()))
                    ||
                    signBook.getLiveWorkflow().getWorkflow().getDashboardRoles().stream().anyMatch(r -> user.getRoles().contains(r))
                ) {
                    return true;
                }
            }
        }
        return signBook.getCreateBy().getEppn().equals(userEppn);
    }

    /**
     * Supprime une étape spécifique d'un workflow en fonction de l'identifiant du SignBook et du numéro d'étape donné.
     * Si l'étape actuelle est celle qui doit être supprimée, l'étape suivante devient l'étape actuelle.
     * Les receveurs et leurs actions associés à cette étape sont supprimés de la configuration du SignBook.
     *
     * @param signBookId l'identifiant unique du SignBook (carnet de signatures) contenant l'étape à supprimer
     * @param step le numéro d'étape à supprimer dans le workflow
     * @return {@code true} si l'étape a été correctement supprimée, sinon {@code false}
     */
    @Transactional
    public String removeStep(Long signBookId, int step) {
        SignBook signBook = getById(signBookId);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(currentStepNumber <= step + 1) {
            if(currentStepNumber == step + 1) {
                if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() > currentStepNumber) {
                    signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(currentStepNumber));
                    for (Recipient recipient : signBook.getLiveWorkflow().getLiveWorkflowSteps().get(currentStepNumber).getRecipients()) {
                        for (SignRequest signRequest : signBook.getSignRequests()) {
                            signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
                        }
                    }
                } else {
                    return "L'étape ne peut pas être supprimée, c'est la dernière étape en cours";
                }
            }
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getLiveWorkflowSteps().get(step);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().remove(liveWorkflowStep);
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                for (SignRequest signRequest : signBook.getSignRequests()) {
                    signRequest.getRecipientHasSigned().remove(recipient);
                }
                if(!signBook.getViewers().contains(recipient.getUser())
                    && (signBook.getLiveWorkflow().getWorkflow() == null
                        ||
                        !signBook.getLiveWorkflow().getWorkflow().getManagers().contains(recipient.getUser().getEmail())
                    )
                ) {
                    signBook.getTeam().remove(recipient.getUser());
                }
            }
            liveWorkflowStepService.delete(liveWorkflowStep);
            return null;
        } else {
            return "L'étape ne peut pas être supprimée, elle précède l'étape en cours";
        }
    }

    private void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment, String userEppn, String authUserEppn) {
        Log log = logService.create(signBook.getId(), signBook.getSubject(), signBook.getWorkflowName(), signRequestStatus, action, comment, returnCode, null, null, null, null, userEppn, authUserEppn);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
        }
    }

    /**
     * Récupère les journaux d'activités associés à un SignBook spécifique.
     *
     * @param signBookId l'identifiant unique du SignBook dont les journaux doivent être récupérés
     * @return une liste de journaux d'activités (Log) associés aux demandes de signature du SignBook
     */
    @Transactional
    public List<Log> getLogsFromSignBook(Long signBookId) {
        SignBook signBook = getById(signBookId);
        List<Log> logs = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logs.addAll(logService.getBySignRequestId(signRequest.getId()));
        }
        return logs;
    }

    /**
     * Récupère l'ensemble des étapes d'un workflow en fonction de l'identifiant du signBook donné.
     *
     * @param signBookId l'identifiant unique du signBook pour lequel les étapes doivent être récupérées
     * @return une liste contenant toutes les étapes du workflow associées au signBook, sauf la première étape
     */
    @Transactional
    public List<LiveWorkflowStep> getAllSteps(Long signBookId) {
        SignBook signBook = getById(signBookId);
        List<LiveWorkflowStep> allSteps = new ArrayList<>(signBook.getLiveWorkflow().getLiveWorkflowSteps());
        if (!allSteps.isEmpty()) {
            allSteps.remove(0);
        }
        return allSteps;
    }

    /**
     * Ajoute une étape au workflow actif d'un SignBook en fonction de l'identifiant du SignBook,
     * des informations de l'étape,*/
    @Transactional
    public void addLiveStep(Long id, WorkflowStepDto step, Integer stepNumber, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        if(stepNumber == null) stepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(BooleanUtils.isTrue(step.getRepeatable())) {
            signBook.getLiveWorkflow().getCurrentStep().setRepeatable(false);
        }
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, step);
        if (stepNumber == -1) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        } else {
            if (stepNumber >= currentStepNumber - 1) {
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
                if(stepNumber == currentStepNumber - 1) {
                    signBook.getLiveWorkflow().setCurrentStep(liveWorkflowStep);
                    pendingSignBook(authUserEppn, id);
                }
            } else {
                if(signBook.getStatus().equals(SignRequestStatus.draft)) {
                    signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
                    signBook.getLiveWorkflow().setCurrentStep(liveWorkflowStep);
                } else {
                    throw new EsupSignatureRuntimeException("L'étape ne peut pas être ajoutée car le circuit est déjà démarré");
                }
            }
        }
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(step));
    }

    /**
     * Ajoute des utilisateurs en tant que spectateurs (viewers) d'un SignBook spécifique
     * ou supprime les spectateurs si aucune adresse mail n'est fournie.
     *
     * @param signBookId l'identifiant unique du SignBook auquel les spectateurs doivent être ajoutés
     * @param recipientsCCEmails liste des adresses email des utilisateurs à ajouter en tant que spectateurs.
     *                           Si la liste est vide ou*/
    @Transactional
    public void addViewers(Long signBookId, List<String> recipientsCCEmails) {
        SignBook signBook = getById(signBookId);
        if(recipientsCCEmails != null && !recipientsCCEmails.isEmpty()) {
            for (String recipientCCEmail : recipientsCCEmails) {
                if(EmailValidator.getInstance().isValid(recipientCCEmail)) {
                    User user = userService.getUserByEmail(recipientCCEmail);
                    if (!signBook.getViewers().contains(user) && !signBook.getCreateBy().equals(user)) {
                        signBook.getViewers().add(user);
                        addToTeam(signBook, user.getEppn());
                        if (globalProperties.getSendCreationMailToViewers() && !signBook.getStatus().equals(SignRequestStatus.draft) && !signBook.getStatus().equals(SignRequestStatus.uploading)) {
                            mailService.sendCCAlert(signBook, Collections.singletonList(recipientCCEmail));
                        }
                    }
                }
            }
        } else {
            signBook.getViewers().clear();
        }
    }

    private List<SignBook> getSharedSignBooks(String userEppn) {
        List<SignBook> sharedSignBook = new ArrayList<>();
        for(UserShare userShare : userShareService.getByToUsersEppnInAndShareTypesContains(Collections.singletonList(userEppn), ShareType.sign)) {
            if(userShare.getWorkflow() != null) {
                sharedSignBook.addAll(getByWorkflowId(userShare.getWorkflow().getId()));
            } else if(userShare.getForm() != null) {
                List<SignRequest> signRequests = signRequestService.getToSignRequests(userShare.getUser().getEppn());
                for (SignRequest signRequest : signRequests) {
                    Data data = dataService.getBySignBook(signRequest.getParentSignBook());
                    if(data.getForm().equals(userShare.getForm())) {
                        sharedSignBook.add(signRequest.getParentSignBook());
                        break;
                    }
                }
            }
        }
        return sharedSignBook;
    }

    /**
     * Récupère toutes les titres des documents correspondant à la recherche effectuée par un utilisateur.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur effectuant la recherche
     * @param searchString le texte de recherche à utiliser pour filtrer les titres des documents
     * @return une liste triée par ordre alphabétique contenant les titres des documents correspondant aux critères de recherche
     */
    @Transactional
    public List<String> getAllDocTitles(String userEppn, String searchString) {
        User user = userService.getByEppn(userEppn);
        Set<String> docTitles = new HashSet<>(signBookRepository.findSubjects(user, "%"+searchString+"%"));
        return docTitles.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    /**
     * Récupère une liste des noms de workflow associés à un utilisateur spécifié par son eppn.
     *
     * @param userEppn L'identifiant eppn de l'utilisateur pour lequel les noms de workflows seront récupérés.
     * @return Une liste triée de chaînes représentant les noms des workflows associés, ignorant les noms null ou vides.
     */
    @Transactional
    public List<String> getWorkflowNames(String userEppn) {
        User user = userService.getByEppn(userEppn);
        List<String> workflowNames = signBookRepository.findAllWorkflowNames(user);
        return workflowNames.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    /**
     * Modifie l'état de visibilité d'un carnet de signatures pour un utilisateur donné.
     * Si l'utilisateur a déjà masqué le carnet de signatures, il sera à nouveau visible.
     * Si l'utilisateur ne l'a pas masqué, il sera caché pour cet utilisateur.
     *
     * @param id L'identifiant unique du carnet de signatures.
     * @param userEpppn L'identifiant professionnel principal (EPPN) de l'utilisateur.
     * @return true si le carnet de signatures vient d'être masqué pour l'utilisateur,
     *         false s'il vient d'être rendu visible.
     */
    @Transactional
    public boolean toggleHideSignBook(Long id, String userEpppn) {
        SignBook signBook = getById(id);
        User user = userService.getByEppn(userEpppn);
        if(signBook.getHidedBy().contains(user)) {
            signBook.getHidedBy().remove(user);
            return false;
        } else {
            signBook.getHidedBy().add(user);
            return true;
        }
    }

    /**
     * Compte le nombre d'éléments vides associés à un utilisateur spécifique.
     *
     * @param userEppn l'identifiant EPPN de l'utilisateur pour lequel le comptage doit être effectué
     * @return le nombre d'éléments vides correspondant à cet utilisateur
     */
    @Transactional
    public int countEmpty(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return Math.toIntExact(signBookRepository.countEmpty(user));
    }

    /**
     * Envoie un formulaire pour signature en générant un SignBook et en configurant le flux de travail
     * et les étapes nécessaires à la signature ou validation selon les paramètres fournis.
     *
     * @param dataId Identifiant de la donnée associée au formulaire à signer.
     * @param steps Liste des étapes du flux de travail, représentées par des objets WorkflowStepDto.
     * @param targetEmails Liste des adresses e-mails cibles où envoyer les notifications de signature.
     * @param targetUrls Liste des URLs cibles où les documents signés seront envoyés.
     * @param userEppn Identifiant unique de l'utilisateur initiant la demande de signature (user principal).
     * @param authUserEppn Identifiant unique de l'utilisateur authentifié agissant pour le compte de l'utilisateur principal.
     * @param forceSendEmail Indique si l'envoi d'un e-mail d'invitation pour la signature doit être forcé.
     * @param formDatas Données du formulaire sous forme de paires clé-valeur à mettre à jour.
     * @param formReplaceInputStream Flux d'entrée du fichier à remplacer dans le document original.
     * @param title Titre attribué au SignBook. Si null ou vide, le titre par défaut du formulaire sera utilisé.
     * @param sendEmailAlert Indique si une alerte par e-mail doit être envoyée pour informer les utilisateurs ciblés.
     * @param comment Commentaire facultatif à inclure dans le SignBook.
     * @return Un objet SignBook représentant le lot des documents gérés pour la signature avec leur flux de travail associé.
     * @throws EsupSignatureRuntimeException Si le formulaire ne peut pas être généré ou si une erreur survient dans le traitement.
     */
    @Transactional
    public SignBook sendForSign(Long dataId, List<WorkflowStepDto> steps, List<String> targetEmails, List<String> targetUrls, String userEppn, String authUserEppn, boolean forceSendEmail, Map<String, String> formDatas, InputStream formReplaceInputStream, String title, Boolean sendEmailAlert, String comment) {
        User user = userService.createUserWithEppn(userEppn);
        User authUser = userService.createUserWithEppn(authUserEppn);
        Data data = dataService.getById(dataId);
        Form form = data.getForm();
        Workflow modelWorkflow = data.getForm().getWorkflow();
        Workflow computedWorkflow = workflowService.computeWorkflow(modelWorkflow.getId(), steps, user.getEppn(), false);
        if(title == null || title.isEmpty()) {
            title = form.getTitle();
        }
        SignBook signBook = createSignBook(title, modelWorkflow, null, user.getEppn(), true, comment);
        SignRequest signRequest = signRequestService.createSignRequest(signBook.getSubject(), signBook, user.getEppn(), authUser.getEppn());
        if(form.getWorkflow().getOwnerSystem() != null && form.getWorkflow().getOwnerSystem()) {
            User systemUser = userService.getSystemUser();
            signBook.setCreateBy(systemUser);
            signRequest.setCreateBy(systemUser);
            addToTeam(signBook, systemUser.getEppn());
        }
        replaceSignRequestParamsWithDtoParams(steps, signRequest);
        byte[] toAddFile;
        try {
            toAddFile = dataService.generateFile(data, formReplaceInputStream);
        } catch(IOException e) {
            throw new EsupSignatureRuntimeException("Ce formulaire ne peut pas être instancié car il ne possède pas de modèle");
        }
        if(computedWorkflow.getWorkflowSteps().isEmpty()) {
            toAddFile = pdfService.convertToPDFA(toAddFile);
        }
        String fileName = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", "") + ".pdf";
        MultipartFile multipartFile = new DssMultipartFile(fileName, fileName, "application/pdf", toAddFile);
        signRequestService.addDocsToSignRequest(signRequest, true, 0, form.getSignRequestParams(), multipartFile);
        workflowService.importWorkflow(signBook, computedWorkflow, steps);
        dispatchSignRequestParams(signBook);
        signRequestService.nextWorkFlowStep(signBook);
        Workflow workflow = workflowService.getById(form.getWorkflow().getId());
        targetService.copyTargets(workflow.getTargets(), signBook, targetEmails);
        if (targetUrls != null) {
            for (String targetUrl : targetUrls) {
                signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl, true, false, false, false));
            }
        }
        data.setSignBook(signBook);
        dataRepository.save(data);
        pendingSignBook(signBook, data, user.getEppn(), authUser.getEppn(), forceSendEmail, sendEmailAlert);
        data.setStatus(SignRequestStatus.pending);
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), steps);
        if(workflow.getCounter() != null) {
            workflow.setCounter(workflow.getCounter() + 1);
        } else {
            workflow.setCounter(0);
        }
        if(formDatas != null && !formDatas.isEmpty()) {
            dataService.updateDatas(form, data, formDatas, user, authUser);
        }
        return signBook;
    }

    private void replaceSignRequestParamsWithDtoParams(List<WorkflowStepDto> steps, SignRequest signRequest) {
        List<SignRequestParams> signRequestParamses = steps.stream().flatMap(s->s.getSignRequestParams().stream().map(SignRequestParamsWsDto::getSignRequestParams)).toList();
        for(SignRequestParams signRequestParams : signRequestParamses) {
            if(StringUtils.hasText(signRequestParams.getPdSignatureFieldName())) {
                SignRequestParams signRequestParamsWsDto = signRequestParamsService.getSignatureField(signRequest.getOriginalDocuments().get(0).getMultipartFile(), signRequestParams.getPdSignatureFieldName());
                if(signRequestParamsWsDto != null) {
                    signRequestParams.setxPos(signRequestParamsWsDto.getxPos());
                    signRequestParams.setyPos(signRequestParamsWsDto.getyPos());
                }
            }
        }
        if (!signRequestParamses.isEmpty()) {
            signRequestParamsRepository.saveAll(signRequestParamses);
            signRequest.getSignRequestParams().clear();
            signRequestService.addAllSignRequestParamsToSignRequest(signRequest, signRequestParamses);
        }
    }

    /**
     * Envoie un résumé d'alertes d'e-mails pour un utilisateur spécifique, incluant les demandes de signature à traiter
     * et les demandes de signature partagées.
     *
     * @param recipientUserEppn le EPPN (EduPersonPrincipalName) de l'utilisateur destinataire.
     * @throws EsupSignatureMailException si une erreur survient lors de l'envoi de l'e-mail.
     */
    @Transactional
    public void sendEmailAlertSummary(String recipientUserEppn) throws EsupSignatureMailException {
        User recipientUser = userService.getByEppn(recipientUserEppn);
        Date date = new Date();
        List<SignRequest> toSignSignRequests = signRequestService.getToSignRequests(recipientUser.getEppn());
        toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser.getEppn()));
        if (!toSignSignRequests.isEmpty()) {
            recipientUser.setLastSendAlertDate(date);
            mailService.sendSignRequestSummaryAlert(Collections.singletonList(recipientUser.getEmail()), toSignSignRequests);
        }
    }

    /**
     * Ajoute des documents dans un carnet de signatures existant.
     * Si le carnet n'est pas éditable, une exception est levée.
     * Après vérification des permissions PDF, cette méthode crée une ou plusieurs
     * demandes de signature, ajoute les documents correspondants à ces demandes,
     * et met à jour le carnet ainsi que son statut si nécessaire.
     * En cas d'erreur lors de l'ajout des documents, la création est annulée.
     *
     * @param signBookId L'identifiant du carnet de signatures dans lequel ajouter les documents
     * @param multipartFiles Un tableau de fichiers multipart contenant les documents à*/
    @Transactional
    public void addDocumentsToSignBook(Long signBookId, MultipartFile[] multipartFiles, String authUserEppn) {
        SignBook signBook = getById(signBookId);
        int i = signBook.getSignRequests().size();
        if(!signBook.isEditable()) {
            throw new EsupSignatureRuntimeException("Ajout impossible, la demande est déjà démarrée");
        }
        for (MultipartFile multipartFile : multipartFiles) {
            pdfService.checkPdfPermitions(multipartFile);
            SignRequest signRequest = signRequestService.createSignRequest(fileService.getNameOnly(multipartFile.getOriginalFilename()), signBook, authUserEppn, authUserEppn);
            try {
                signRequestService.addDocsToSignRequest(signRequest, true, i, new ArrayList<>(), multipartFile);
                if (signBook.getStatus().equals(SignRequestStatus.pending)) {
                    signRequestService.pendingSignRequest(signRequest, authUserEppn);
                    addToTeam(signBook, authUserEppn);
                }
            } catch (EsupSignatureIOException e) {
                logger.warn("revert signbook creation due to error : " + e.getMessage());
                deleteDefinitive(signBookId, authUserEppn);
                throw new EsupSignatureIOException(e.getMessage(), e);
            }
            i++;
        }
        if(!StringUtils.hasText(signBook.getSubject())) {
            signBook.setSubject(generateName(null, null, signBook.getCreateBy(), false, false, signBookId));
        }
    }

    /**
     * Crée un parapheur (SignBook) à partir des informations fournies, ajoute les documents à signer,
     **/
    @Transactional
    public Map<SignBook, String> createAndSendSignBook(String title, MultipartFile[] multipartFiles, Boolean pending, List<WorkflowStepDto> steps, String createByEppn, boolean forceSendEmail, Boolean forceAllSign, String targetUrl) throws EsupSignatureException {
        User authUser = userService.createUserWithEppn(createByEppn);
        if(authUser == null) {
            throw new EsupSignatureException("user not found");
        }
        if(forceAllSign == null) forceAllSign = false;
        if(title == null || title.isEmpty()) {
            if(multipartFiles.length == 1) {
                title = fileService.getNameOnly(multipartFiles[0].getOriginalFilename());
            } else {
                if(steps.size() > 1 || steps.get(0).getRecipients().size() > 1) {
                    title = "Parapheur à plusieurs étapes/signataires";
                } else {
                    title = "Parapheur pour " + steps.get(0).getRecipients().get(0).getEmail();
                }
            }
        }
        SignBook signBook = createSignBook(title, null, "Demande générée", createByEppn, true, null);
        addDocumentsToSignBook(signBook.getId(), multipartFiles, createByEppn);
        signBook.setForceAllDocsSign(forceAllSign);
        addViewers(signBook.getId(), steps.stream().map(WorkflowStepDto::getRecipientsCCEmails).filter(Objects::nonNull).flatMap(List::stream).toList());
        if(targetUrl != null && !targetUrl.isEmpty()) {
            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl, true, false, false, false));
        }
        for(SignRequest signRequest : signBook.getSignRequests()) {
            replaceSignRequestParamsWithDtoParams(steps, signRequest);
        }
        return sendSignBook(signBook, pending, steps.get(0).getComment(), steps, createByEppn, createByEppn, forceSendEmail);
    }

    /**
     * Envoie un SignBook avec les étapes spécifiées pour démarrer un workflow de signature.
     *
     * @param signBook L'objet SignBook représentant la demande de signature à envoyer.
     * @param pending Si true ou null, le SignBook est mis en attente; sinon, le SignBook est traité comme un brouillon.
     * @param comment Un commentaire facultatif associé au SignBook.
     * @param steps Liste des étapes du workflow à importer dans le SignBook.
     * @param userEppn Identifiant ePPN de l'utilisateur initiant l'action.
     * @param authUserEppn Identifiant ePPN de l'utilisateur authentifié réalisant l'action.
     * @param forceSendEmail Si true, force l'envoi d'emails de notification.
     * @return Une map contenant le SignBook en tant que clé et un message d'information comme valeur.
     * @throws EsupSignatureRuntimeException Exception levée en cas d'erreur durant le traitement du SignBook.
     */
    @Transactional
    public Map<SignBook, String> sendSignBook(SignBook signBook, Boolean pending, String comment, List<WorkflowStepDto> steps, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureRuntimeException {
        logger.info(userEppn + " envoi d'une demande de signature à " + StringUtils.collectionToCommaDelimitedString(steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).map(RecipientWsDto::getEmail).flatMap(String::lines).toList()));
        importWorkflowFromWorkflowStepDto(signBook.getId(), steps, userEppn);
        String message = null;
        if (pending == null || pending) {
            pendingSignBook(signBook, null, userEppn, authUserEppn, forceSendEmail, true);
        } else {
            updateStatus(signBook, SignRequestStatus.draft,  "Création de la demande " + signBook.getId(), "SUCCESS", null, userEppn, authUserEppn);
            message = "Après vérification/annotation, vous devez cliquer sur 'Démarrer le circuit' pour transmettre la demande aux participants";
        }
        if (comment != null && !comment.isEmpty()) {
            signBook.setDescription(comment);
        }
        Map<SignBook, String> signBookStringMap = new HashMap<>();
        signBookStringMap.put(signBook, message);
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), steps);
        return signBookStringMap;
    }

    /**
     * Initialise le workflow d'un carnet de signatures (signBook).
     *
     * @param signBookId L'identifiant du carnet de signatures à initialiser.
     * @param steps La liste des étapes du workflow à appliquer.
     * @param targetEmails La liste des adresses e-mail cibles, si nécessaire.
     * @param userEppn L'identifiant EPPN (eduPersonPrincipalName) de l'utilisateur effectuant l'opération.
     * @param authUserEppn L'identifiant EPPN de l'utilisateur authentifié effectuant l'opération.
     * @param pending Indicateur spécifiant si le carnet de signatures doit être mis en attente.
     * @param sendEmailAlert Indicateur spécifiant si une alerte e-mail*/
    @Transactional
    public void initSignBookWorkflow(Long signBookId, List<WorkflowStepDto> steps, List<String> targetEmails, String userEppn, String authUserEppn, Boolean pending, Boolean sendEmailAlert) throws EsupSignatureRuntimeException {
        List<RecipientWsDto> recipients = steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList();
        if(signRequestService.checkTempUsers(signBookId, recipients)) {
            throw new EsupSignatureRuntimeException("Merci de compléter tous les utilisateurs externes");
        }
        SignBook signBook = getById(signBookId);
        if(signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getStatus().equals(SignRequestStatus.uploading)) {
            List<Target> targets = new ArrayList<>(signBook.getLiveWorkflow().getWorkflow().getTargets());
            if(signBook.getLiveWorkflow().getWorkflow().getWorkflowSteps().isEmpty()) {
                workflowService.computeWorkflow(steps, signBook);
            } else {
                Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow().getId(), steps, userEppn, false);
                workflowService.importWorkflow(signBook, workflow, steps);
                signRequestService.nextWorkFlowStep(signBook);
            }
            dispatchSignRequestParams(signBook);
            targetService.copyTargets(targets, signBook, targetEmails);
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), steps);
            if (pending != null && pending) {
                pendingSignBook(signBook, null, userEppn, authUserEppn, false, sendEmailAlert);
            }
        }
        if(signBook.getLiveWorkflow().getWorkflow().getOwnerSystem() != null && signBook.getLiveWorkflow().getWorkflow().getOwnerSystem()) {
            signBook.setCreateBy(userService.getSystemUser());
            for(SignRequest signRequest : signBook.getSignRequests()) {
                signRequest.setCreateBy(userService.getSystemUser());
            }
        }
    }

    /**
     * Met un SignBook en attente de signature en utilisant les informations fournies.
     *
     * @param authUserEppn le nom principal de l'utilisateur autorisé (EPPN) qui effectue l'action
     * @param id l'identifiant unique du SignBook à mettre en attente
     */
    @Transactional
    public void pendingSignBook(String authUserEppn, Long id) {
        SignBook signBook = getById(id);
        pendingSignBook(signBook, null, authUserEppn, authUserEppn, false, true);
    }

    @Transactional
    public void pendingSignBook(SignBook signBook, Data data, String userEppn, String authUserEppn, boolean forceSendEmail, boolean sendEmailAlert) throws EsupSignatureRuntimeException {
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
                signBook.getLiveWorkflow().getCurrentStep().setSignType(SignType.signature);
                liveWorkflowStepService.addRecipient(liveWorkflowStep, recipientService.createRecipient(userService.getSystemUser()));
            }
            if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
                if (liveWorkflowStep != null) {
                    signRequestService.pendingSignRequest(signRequest, userEppn);
                    addToTeam(signBook, userEppn);
                    if (!emailSended && sendEmailAlert) {
                        try {
                            mailService.sendEmailAlerts(signBook, userEppn, data, forceSendEmail);
                            mailService.sendCCAlert(signBook, null);
                            emailSended = true;
                        } catch (EsupSignatureMailException e) {
                            throw new EsupSignatureRuntimeException(e.getMessage());
                        }
                    }
                    if(signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
                        for(SignRequest signRequest1 : signBook.getSignRequests()) {
                            List<SignRequestParams> signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
                            if(liveWorkflowStep.getWorkflowStep().getCertificat() != null) {
                                if (!signRequestParamses.isEmpty()) {
                                    signRequestParamses.get(0).setExtraDate(true);
                                    signRequestParamses.get(0).setAddExtra(true);
                                    signRequestParamses.get(0).setExtraOnTop(true);
                                    signRequestParamses.get(0).setAddWatermark(true);
                                    signRequestParamses.get(0).setSignWidth(200);
                                    signRequestParamses.get(0).setSignHeight(100);
                                    signRequestParamses.get(0).setExtraText(signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getCertificat().getKeystore().getFileName().replace(",", "\n"));
                                }
                                try {
                                    signRequestParamsService.copySignRequestParams(signRequest1.getId(), signRequestParamses);
                                    signRequestService.sign(signRequest1, "", "autoCert", "default", null, null,"system", "system", null, "");
                                                                    } catch (IOException | EsupSignatureMailException e) {
                                    refuse(signRequest1.getId(), "Signature refusée par le système automatique", "system", "system");
                                    logger.error("auto sign fail", e);
                                    throw new EsupSignatureRuntimeException("Erreur lors de la signature automatique : " + e.getMessage());
                                }
                            } else {
                                try {
                                    signRequestParamsService.copySignRequestParams(signRequest1.getId(), signRequestParamses);
                                    signRequestService.sign(signRequest1, "", "sealCert", "default", null, null,"system", "system", null, "");
                                } catch (IOException | EsupSignatureRuntimeException e) {
                                    logger.error("auto sign fail", e);
                                    refuse(signRequest1.getId(), "Signature refusée par le système automatique", "system", "system");
                                    throw new EsupSignatureRuntimeException("Erreur lors de la signature automatique : " + e.getMessage());
                                }
                            }

                        }
                        if(signRequestService.isMoreWorkflowStep(signBook)) {
                            pendingSignBook(signBook, data, userEppn, authUserEppn, forceSendEmail, sendEmailAlert);
                        } else {
                            completeSignBook(signBook, userEppn, "Tous les documents sont signés");
                            logger.info("Circuit " + signBook.getId() + " terminé");
                            return;
                        }
                    } else {
                        if(!signRequest.getSignRequestParams().isEmpty()) {
                            dispatchSignRequestParams(signRequest);
                        }
                    }
                } else {
                    completeSignBook(signBook, userEppn, "Tous les documents sont signés");
                    logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                    return;
                }
            }
        }
        updateStatus(signBook, SignRequestStatus.pending, "Circuit démarré pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), userEppn, authUserEppn);
        logger.info("Circuit " + signBook.getId() + " démarré pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
        if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null) {
            for (Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if (recipient.getUser().getUserType().equals(UserType.external)) {
                    try {
                        otpService.generateOtpForSignRequest(signBook.getId(), recipient.getUser().getId(), null, true);
                    } catch (EsupSignatureMailException e) {
                        throw new EsupSignatureRuntimeException(e.getMessage());
                    }
                }
            }
        }
    }

    private void completeSignBook(SignBook signBook, String userEppn, String message) throws EsupSignatureRuntimeException {
        if (!signBook.getCreateBy().equals(userService.getSchedulerUser())) {
            try {
                Set<String> toMails = mailService.sendCompletedMail(signBook, userEppn);
                if(signBook.getLiveWorkflow().getWorkflow() == null || signBook.getLiveWorkflow().getWorkflow().getSendAlertToAllRecipients()) {
                    mailService.sendCompletedCCMail(signBook, userEppn, toMails);
                    for(User externalUser : signBook.getTeam().stream().filter(u -> u.getUserType().equals(UserType.external)).toList()) {
                        otpService.generateOtpForSignRequest(signBook.getId(), externalUser.getId(), externalUser.getPhone(), false);
                    }
                }
            } catch (EsupSignatureMailException e) {
                throw new EsupSignatureRuntimeException(e.getMessage());
            }
        }
        signRequestService.completeSignRequests(signBook.getSignRequests(), userEppn);
        Data data = dataService.getBySignBook(signBook);
        if(data != null) {
            data.setStatus(SignRequestStatus.completed);
        }
        updateStatus(signBook, SignRequestStatus.completed, message, "SUCCESS", "", userEppn, userEppn);
        signBook.setEndDate(new Date());
    }

    /**
     * Scelle tous les documents associés au SignBook identifié par l'ID donné.
     * Cette méthode parcourt chaque demande de signature dans le SignBook
     * et applique un scellement sur chacun d'eux.
     *
     * @param id l'identifiant unique du SignBook dont tous les documents doivent être scellés
     * @throws EsupSignatureRuntimeException si une erreur liée à la signature survient
     * @throws IOException si une erreur d'entrée/sortie survient pendant le processus
     */
    @Transactional
    public void sealAllDocs(Long id) throws EsupSignatureRuntimeException, IOException {
        SignBook signBook = getById(id);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signService.seal(signRequest.getId());
        }
    }

    private List<SignRequest> getSharedToSignSignRequests(String userEppn) {
        List<SignRequest> sharedSignRequests = new ArrayList<>();
        List<SignBook> sharedSignBooks = getSharedSignBooks(userEppn);
        for(SignBook signBook: sharedSignBooks) {
            sharedSignRequests.addAll(signBook.getSignRequests());
        }
        return sharedSignRequests;
    }

    /**
     * Initialise le processus de signature pour une requête de signature donnée.
     *
     * @param signRequestId L'identifiant unique de la requête de signature.
     * @param signRequestParamsJsonString Chaîne JSON contenant les paramètres de la requête de signature.
     * @param comment Commentaire fourni par l'utilisateur concernant la signature.
     * @param formData Chaîne JSON contenant les données du formulaire pour la signature.
     * @param password Mot de passe de l'utilisateur pour authentification.
     * @param signWith Type de signature utilisée (par exemple, certificat, image, etc.).
     * @param userShareId Identifiant de partage de l'utilisateur, si applicable.
     * @param userEppn Identifiant unique (eppn) de l'utilisateur effectuant la signature.
     * @param authUserEppn Identifiant unique (eppn) de l'utilisateur authentifié.
     * @return Le statut de l'étape courante après l'initialisation de la signature.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de l'accès aux données ou à des fichiers.
     * @throws EsupSignatureRuntimeException Si une exception liée au processus de signature se produit.
     */
    @Transactional
    public StepStatus initSign(Long signRequestId, String signRequestParamsJsonString, String comment, String formData, String password, String signWith, String sealCertificat, Long userShareId, String userEppn, String authUserEppn) throws IOException, EsupSignatureRuntimeException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        if(signRequest.getAuditTrail() == null) {
            signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
        }
        SignType signType = signRequest.getCurrentSignType();
        if(signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) && signWith == null) signWith = "imageStamp";
        String finalSignWith = signWith;
        if(signWith == null ||
                (globalProperties.getAuthorizedSignTypes().stream().noneMatch(s -> s.getValue() <= SignWith.valueOf(finalSignWith).getValue())
                && !signWithService.getAuthorizedSignWiths(userEppn, signRequest, false).contains(SignWith.valueOf(signWith)))) {
            throw new EsupSignatureRuntimeException("Le type de signature " + signWith + " n'est pas autorisé");
        }
        Map<String, String> formDataMap = null;
        List<String> toRemoveKeys = new ArrayList<>();
        Data data = null;
        if(formData != null) {
            try {
                TypeReference<Map<String, String>> type = new TypeReference<>(){};
                formDataMap = objectMapper.readValue(formData, type);
                formDataMap.remove("_csrf");
                data = dataService.getBySignBook(signRequest.getParentSignBook());
                if(data != null && data.getForm() != null) {
                    List<Field> fields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), data.getForm().getFields(), userService.getByEppn(userEppn), signRequest);
                    for(Map.Entry<String, String> entry : formDataMap.entrySet()) {
                        Optional<Field> formfield = fields.stream().filter(f -> f.getName().equals(entry.getKey())).findFirst();
                        if(formfield.isPresent()) {
                            if(formfield.get().getWorkflowSteps().contains(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep())) {
                                if(formfield.get().getExtValueType() == null || !formfield.get().getExtValueType().equals("system")) {
                                    data.getDatas().put(entry.getKey(), entry.getValue());
                                } else {
                                    if(formfield.get().getDefaultValue() != null && !formfield.get().getDefaultValue().isEmpty()) {
                                        data.getDatas().put(entry.getKey(), formfield.get().getDefaultValue());
                                    }
                                }
                            }
                        } else {
                            toRemoveKeys.add(entry.getKey());
                        }
                    }
                    for (String toRemoveKey : toRemoveKeys) {
                        formDataMap.remove(toRemoveKey);
                    }
                }
            } catch (IOException e) {
                logger.error("form datas error", e);
            }
        }
        List<SignRequestParams> signRequestParamses;
        if (signRequestParamsJsonString == null) {
            signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
            for(SignRequestParams signRequestParamse : signRequestParamses) {
                User user = userService.getByEppn(userEppn);
                signRequestParamse.setAddExtra(true);
                signRequestParamse.setExtraDate(true);
                signRequestParamse.setAddWatermark(true);
                signRequestParamse.setSignImageNumber(user.getDefaultSignImageNumber());
                if(user.getFavoriteSignRequestParams() != null) {
                    signRequestParamse.setAddImage(user.getFavoriteSignRequestParams().getAddImage());
                    signRequestParamse.setAddWatermark(user.getFavoriteSignRequestParams().getAddWatermark());
                    signRequestParamse.setAddExtra(user.getFavoriteSignRequestParams().getAddExtra());
                    signRequestParamse.setExtraText(user.getFavoriteSignRequestParams().getExtraText());
                    signRequestParamse.setTextPart(user.getFavoriteSignRequestParams().getTextPart());
                    signRequestParamse.setExtraDate(user.getFavoriteSignRequestParams().getExtraDate());
                    signRequestParamse.setExtraType(user.getFavoriteSignRequestParams().getExtraType());
                    signRequestParamse.setExtraName(user.getFavoriteSignRequestParams().getExtraName());
                    signRequestParamse.setExtraOnTop(user.getFavoriteSignRequestParams().getExtraOnTop());
                }
            }
        } else {
            signRequestParamses = userService.getSignRequestParamsesFromJson(signRequestParamsJsonString, userEppn);
        }
        signRequestParamsService.copySignRequestParams(signRequest.getId(), signRequestParamses);
        if (signRequest.getCurrentSignType().equals(SignType.signature) && (SignWith.valueOf(signWith).equals(SignWith.nexuCert))) {
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep() != null && signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getSignRequestParams() != null && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getSignRequestParams().isEmpty()) {
                signRequestParamsService.copySignRequestParams(signRequest.getId(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getWorkflowStep().getSignRequestParams());
            }
            return StepStatus.nexu_redirect;
        } else {
            StepStatus stepStatus = signRequestService.sign(signRequest, password, signWith, sealCertificat, data, formDataMap, userEppn, authUserEppn, userShareId, comment);
            if(stepStatus.equals(StepStatus.last_end)) {
                try {
                    if(globalProperties.getSealAllDocs() ||
                        (signRequest.getParentSignBook().getLiveWorkflow() != null
                            && ((signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa) && BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSealVisa()))
                                || (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null
                                    && BooleanUtils.isTrue(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getSealAtEnd()))
                                )
                        )
                    ) {
                        sealAllDocs(signRequest.getParentSignBook().getId());
                    }
                    completeSignBook(signRequest.getParentSignBook(), authUserEppn, "Tous les documents sont signés");
                    Document signedDocument = signRequest.getLastSignedDocument();
                    auditTrailService.closeAuditTrail(signRequest.getToken(), signedDocument, signedDocument.getInputStream());
               } catch(IOException e) {
                    throw new EsupSignatureRuntimeException(e.getMessage());
                }
            } else if(stepStatus.equals(StepStatus.completed)) {
                if(signRequestService.isCurrentStepCompleted(signRequest)) {
                    signRequest.getSignRequestParams().clear();
                    pendingSignBook(signRequest.getParentSignBook(), null, userEppn, authUserEppn, false, true);
                }
            }
            return stepStatus;
        }
    }

    /**
     * Initialise un processus de signature en masse pour un utilisateur donné.
     *
     * @param userEppn L'identifiant unique (eppn) de l'utilisateur pour lequel le processus de signature est initialisé.
     * @param authUserEppn L'identifiant unique (eppn) de l'utilisateur actuellement authentifié.
     * @param ids Une chaîne JSON contenant une liste d'identifiants des demandes de signature à traiter.
     * @param httpSession La session HTTP active permettant de récupérer des informations utilisateur partagées.
     * @param password Le mot de passe fourni pour assurer l'authentification nécessaire au processus de signature.
     * @param signWith La méthode ou le type d'outil de signature à utiliser (par exemple, nexuCert, imageStamp).
     * @return Une chaîne contenant un message d'erreur ou null si le processus de signature s'est déroulé sans problème.
     * @throws IOException Si une erreur d'entrée/sortie se produit lors de la conversion des identifiants à partir de la chaîne JSON.
     * @throws EsupSignatureRuntimeException Si une exception liée à l'exécution de la logique de signature se produit.
     */
    @Transactional
    public String initMassSign(String userEppn, String authUserEppn, String ids, HttpSession httpSession, String password, String signWith, String sealCertificat) throws IOException, EsupSignatureRuntimeException {
        String error = null;
        TypeReference<List<String>> type = new TypeReference<>(){};
        List<String> idsString = objectMapper.readValue(ids, type);
        List<Long> idsLong = new ArrayList<>();
        idsString.forEach(s -> idsLong.add(Long.parseLong(s)));
        Object userShareString = httpSession.getAttribute("userShareId");
        Report report = reportService.createReport(authUserEppn);
        Long userShareId = null;
        if(userShareString != null) {
            userShareId = Long.valueOf(userShareString.toString());
        }
        List<StepStatus> stepStatuses = new ArrayList<>();
        for (Long id : idsLong) {
            SignRequest selectedSignRequest = signRequestService.getById(id);
            selectedSignRequest.getSignRequestParams().addAll(selectedSignRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams());
            StepStatus stepStatus = StepStatus.not_completed;
            for(SignRequest signRequest : selectedSignRequest.getParentSignBook().getSignRequests()) {
                if (!signRequest.getStatus().equals(SignRequestStatus.pending)) {
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.badStatus);
                } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEppn().equals(authUserEppn))) {
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.userNotInCurrentStep);
                    error = messageSource.getMessage("report.reportstatus." + ReportStatus.userNotInCurrentStep, null, Locale.FRENCH);
                } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().getValue() > SignWith.valueOf(signWith).getValue()) {
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.signTypeNotCompliant);
                    error = messageSource.getMessage("report.reportstatus." + ReportStatus.signTypeNotCompliant, null, Locale.FRENCH);
                } else if (signRequest.getSignRequestParams().isEmpty() && SignWith.valueOf(signWith).equals(SignWith.imageStamp)) {
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.noSignField);
                    error = messageSource.getMessage("report.reportstatus." + ReportStatus.noSignField, null, Locale.FRENCH);
                } else if (signRequest.getStatus().equals(SignRequestStatus.pending)) {
                    stepStatus = initSign(signRequest.getId(), null, null, null, password, signWith, sealCertificat, userShareId, userEppn, authUserEppn);
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.signed);
                } else {
                    reportService.addSignRequestToReport(report.getId(), signRequest.getId(), ReportStatus.error);
                }
            }
            stepStatuses.add(stepStatus);
        }
        if(stepStatuses.stream().allMatch(s -> s.equals(StepStatus.nexu_redirect))) {
            return "initNexu";
        }
        if(!stepStatuses.stream().allMatch(s -> s.equals(StepStatus.completed) || s.equals(StepStatus.last_end))) {
            error = messageSource.getMessage("report.reportstatus." + ReportStatus.error, null, Locale.FRENCH);
        }
        return error;
    }

    private void refuseSignBook(SignBook signBook, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
        }
        updateStatus(signBook, SignRequestStatus.refused, "Cette demande a été refusée, ceci annule toute la procédure", "SUCCESS", comment, userEppn, authUserEppn);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", null, "SUCCESS", null, null, null, signBook.getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if(recipient.getUser().getEppn().equals(userEppn)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    if(action != null) {
                        action.setActionType(ActionType.refused);
                        action.setUserIp(webUtilsService.getClientIp());
                        action.setDate(new Date());
                    } else {
                        action = actionService.getEmptyAction();
                        action.setActionType(ActionType.refused);
                        action.setUserIp(webUtilsService.getClientIp());
                        action.setDate(new Date());
                        signRequest.getRecipientHasSigned().put(recipient, action);
                    }
                    recipientService.allSigned(signRequest, recipient);
                }
            }
        }
        Data data = dataService.getBySignBook(signBook);
        if(data != null) {
            data.setStatus(SignRequestStatus.refused);
        }
        signBook.setEndDate(new Date());
        mailService.sendRefusedMail(signBook, comment, userEppn);
    }

    /**
     * Refuse une demande de signature spécifiée. Cette méthode met à jour l'état
     * de la demande et effectue les étapes nécessaires en fonction de la configuration
     * et de l'état actuel du document et du SignBooks.
     *
     * @param signRequestId l'identifiant unique de la demande de signature à refuser
     * @param comment le commentaire fourni par l'utilisateur pour expliquer le refus
     * @param userEppn l'identifiant unique (eppn) de l'utilisateur initiant le refus
     * @param authUserEppn l'identifiant unique (eppn) de l'utilisateur authentifié
     * @throws EsupSignatureRuntimeException si une erreur se produit lors du traitement
     */
    @Transactional
    public void refuse(Long signRequestId, String comment, String userEppn, String authUserEppn) throws EsupSignatureRuntimeException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getSignRequests().size() > 1 && (signBook.getForceAllDocsSign() == null || !signBook.getForceAllDocsSign())) {
            commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", null, "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
                targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "refused", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), authUserEppn, comment);
            }
            for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                if (recipient.getUser().getEppn().equals(userEppn)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    action.setActionType(ActionType.refused);
                    action.setUserIp(webUtilsService.getClientIp());
                    action.setDate(new Date());
                    recipientService.allSigned(signRequest, recipient);
                }
            }
            List<SignRequest> signRequests = new ArrayList<>(signBook.getSignRequests());
            signRequests.remove(signRequest);
            if (signRequests.stream().allMatch(signRequest1 -> signRequest1.getStatus().equals(SignRequestStatus.refused))) {
                refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
            } else {
                if (signRequests.stream().noneMatch(signRequest1 -> signRequest1.getStatus().equals(SignRequestStatus.pending))) {
                    if (signRequestService.isMoreWorkflowStep(signRequest.getParentSignBook())) {
                        nextStepAndPending(signBook.getId(), null, userEppn, authUserEppn);
                    } else {
                        if (signBook.getSignRequests().stream().allMatch(signRequest1 -> signRequest1.getStatus().equals(SignRequestStatus.completed))) {
                            completeSignBook(signBook, userEppn, "Tous les documents sont signés");
                        } else if (signBook.getSignRequests().stream().allMatch(signRequest1 -> signRequest1.getStatus().equals(SignRequestStatus.refused))) {
                            refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
                        } else {
                            completeSignBook(signBook, userEppn, "La demande est terminée mais au moins un des documents à été refusé");
                        }
                    }
                }
            }
        } else {
            refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
            for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
                targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "refused", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), authUserEppn, comment);
            }
        }
    }

    /**
     * Démarre un workflow en créant un SignBook et en y ajoutant les signataires, documents et cibles associés.
     *
     * @param id l'identifiant ou le token du workflow à démarrer
     * @param multipartFiles les fichiers à inclure dans le workflow
     * @param createByEppn l'identifiant EPPN de l'utilisateur initiateur du workflow
     * @param title le titre du SignBook créé
     * @param steps la liste des étapes du workflow (WorkflowStepDto)
     * @param targetEmails les adresses email des destinataires cibles
     * @param targetUrls les URLs des cibles associées au workflow
     * @param scanSignatureFields indique si les champs de signature doivent être automatiquement détectés
     * @param sendEmailAlert indique si une alerte email doit être envoyée aux destinataires
     * @param comment un commentaire pouvant être attaché au SignBook
     * @return une liste des identifiants des SignRequests créées dans le workflow
     * @throws EsupSignatureRuntimeException si une erreur survient lors de l'exécution du workflow
     */
    @Transactional
    public List<Long> startWorkflow(String id, MultipartFile[] multipartFiles, String createByEppn, String title, List<WorkflowStepDto> steps, List<String> targetEmails, List<String> targetUrls, Boolean scanSignatureFields, Boolean sendEmailAlert, String comment) throws EsupSignatureRuntimeException {
        logger.info("starting workflow " + id + " by " + createByEppn);
        Workflow workflow = workflowService.getByIdOrToken(id);
        User user = userService.createUserWithEppn(createByEppn);
        SignBook signBook = createSignBook(title, workflow, "", user.getEppn(), false, comment);
        signBook.getLiveWorkflow().setWorkflow(workflow);
        for(MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(multipartFile.getOriginalFilename(), signBook, createByEppn, createByEppn);
            replaceSignRequestParamsWithDtoParams(steps, signRequest);
            signRequestService.addDocsToSignRequest(signRequest, scanSignatureFields, 0, new ArrayList<>(), multipartFile);
        }
        signBook.setSubject(generateName(null, workflow, user, false, false, signBook.getId()));
        if (targetUrls != null) {
            for (String targetUrl : targetUrls) {
                if (signBook.getLiveWorkflow().getTargets().stream().noneMatch(t -> t != null && t.getTargetUri().equals(targetUrl))) {
                    Target target = targetService.createTarget(targetUrl, true, false, false, false);
                    signBook.getLiveWorkflow().getTargets().add(target);
                }
            }
        }
        initSignBookWorkflow(signBook.getId(), steps, targetEmails, createByEppn, createByEppn, true, sendEmailAlert);
        return signBook.getSignRequests().stream().map(SignRequest::getId).toList();
    }

    /**
     * Import un liveWorkflow au SignBook spécifié.
     *
     * @param signBook SignBook auquel le workflow doit être ajouté.
     * @param authUserEppn ePPN de l'utilisateur authentifié effectuant l'opération.
     * @param workflowSignBookId ID du workflow à ajouter au SignBook.
     * @throws EsupSignatureRuntimeException Exception levée en cas d'erreur lors de l'ajout du workflow.
     */
    @Transactional
    public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        workflowService.importWorkflow(signBook, workflow, new ArrayList<>());
        dispatchSignRequestParams(signBook);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook, null, authUserEppn, authUserEppn, false, true);
    }

    /**
     * Effectue une transition vers l'étape suivante d'un SignBook et le place en attente.
     *
     * @param signBookId l'identifiant du SignBook à traiter
     * @param data les données associées au traitement de l'étape
     * @param userEppn l'identifiant EPPN de l'utilisateur effectuant l'action
     * @param authUserEppn l'identifiant EPPN de l'utilisateur authentifié
     * @throws EsupSignatureRuntimeException si une erreur survient lors du passage à l'étape suivante ou de la mise en attente
     */
    @Transactional
    public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(signBookId);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook, data, userEppn, authUserEppn, true, true);
    }

    /**
     * Démarre un workflow en direct associé à un SignBook.
     * Si le workflow contient des étapes, la première étape est définie comme l'étape courante.
     * Si le paramètre 'start' est vrai, le SignBook est mis en attente pour la signature selon les utilisateurs spécifiés.
     *
     * @param signBookId l'identifiant unique du SignBook à utiliser
     * @param userEppn l'identifiant de l'utilisateur principal (eppn) initiant l'action
     * @param authUserEppn l'identifiant de l'utilisateur authentifié (eppn) effectuant l'action
     * @param start un indicateur pour déclencher ou non le démarrage du workflow en attente
     * @return true si le workflow en direct contient des étapes et a été démarré correctement, false sinon
     * @throws EsupSignatureRuntimeException si des erreurs spécifiques à l'application surviennent lors du démarrage
     */
    @Transactional
    public boolean startLiveWorkflow(Long signBookId, String userEppn, String authUserEppn, Boolean start) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(signBookId);
        if(!signBook.getLiveWorkflow().getLiveWorkflowSteps().isEmpty()) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
            if(start != null && start) {
                pendingSignBook(signBook, null, userEppn, authUserEppn, false, true);
            }
            return true;
        }else {
            return false;
        }
    }

    /**
     * Importe des fichiers à partir d'une source définie associée à un workflow donné.
     *
     * @param workflowId l'identifiant du workflow à partir duquel les fichiers doivent être importés
     * @param user l'utilisateur qui initie l'importation des fichiers
     * @param authUser l'utilisateur authentifié qui autorise l'importation
     * @return le nombre de fichiers importés depuis la source
     * @throws EsupSignatureRuntimeException en cas d'erreur ou d'exception lors de l'importation des fichiers
     */
    @Transactional
    public int importFilesFromSource(Long workflowId, User user, User authUser) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowService.getById(workflowId);
        int nbImportedFiles = 0;
        if (workflow.getDocumentsSourceUri() != null && StringUtils.hasText(workflow.getDocumentsSourceUri())) {
            logger.info("retrieve from " + workflow.getProtectedDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(workflow.getDocumentsSourceUri());
            if (fsAccessService != null) {
                fsAccessService.open();
                fsAccessService.createURITree(workflow.getDocumentsSourceUri());
                List<FsFile> fsFiles = new ArrayList<>(fsAccessService.listFiles(workflow.getDocumentsSourceUri() + "/"));
                if (!fsFiles.isEmpty()) {
                    int j = 0;
                    for (FsFile fsFile : fsFiles) {
                        logger.info("adding file : " + fsFile.getName());
                        ByteArrayOutputStream baos = fileService.copyInputStream(fsFile.getInputStream());
                        Map<String, String> metadatas = pdfService.readMetadatas(new ByteArrayInputStream(baos.toByteArray()));
                        String documentName = fsFile.getName();
                        if (metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
                            documentName = metadatas.get("Title");
                        }
                        SignBook signBook = createSignBook(fileService.getNameOnly(documentName), workflow, "", user.getEppn(), true, null);
                        signBook.getLiveWorkflow().setWorkflow(workflow);
                        SignRequest signRequest = signRequestService.createSignRequest(null, signBook, user.getEppn(), authUser.getEppn());
                        if (fsFile.getCreateBy() != null && userService.getByEppn(fsFile.getCreateBy()) != null) {
                            user = userService.getByEppn(fsFile.getCreateBy());
                        }
                        signRequestService.addDocsToSignRequest(signRequest, true, j, new ArrayList<>(), new DssMultipartFile(fsFile.getName(), fsFile.getName(), fsFile.getContentType(), baos.toByteArray()));
                        if (workflow.getScanPdfMetadatas()) {
                            String signType = metadatas.get("sign_type_default_val");
                            User creator = userService.createUserWithEppn(metadatas.get("Creator"));
                            if (creator != null) {
                                signRequest.setCreateBy(creator);
                                signBook.setCreateBy(creator);
                                addToTeam(signBook, creator.getEppn());
                            } else {
                                User systemUser = userService.getSystemUser();
                                signRequest.setCreateBy(systemUser);
                                signBook.setCreateBy(systemUser);
                                addToTeam(signBook, systemUser.getEppn());
                            }
                            int i = 0;
                            for (String metadataKey : metadatas.keySet()) {
                                String[] keySplit = metadataKey.split("_");
                                if (keySplit[0].equals("sign") && keySplit[1].contains("step")) {
                                    try {
                                        ObjectMapper mapper = new ObjectMapper();
                                        TypeReference<List<String>> type = new TypeReference<>() {
                                        };
                                        List<String> recipientList = mapper.readValue(metadatas.get(metadataKey), type);
                                        WorkflowStep workflowStep = null;
                                        if (workflow.getWorkflowSteps().size() > i) {
                                            workflowStep = workflow.getWorkflowSteps().get(i);
                                        }
                                        WorkflowStepDto workflowStepDto = recipientService.convertRecipientEmailsToStep(recipientList).get(0);
                                        workflowStepDto.setSignType(SignType.fromString(signType));
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, workflowStep, workflowStepDto);
                                        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
                                    } catch (Exception e) {
                                        throw new EsupSignatureRuntimeException(e.getMessage(), e);
                                    }
                                    i++;
                                }
                                if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
                                    String metadataTarget = metadatas.get(metadataKey);
                                    for(Target target : workflow.getTargets()) {
                                        signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri() + "/" + metadataTarget, target.getSendDocument(), target.getSendReport(), target.getSendAttachment(), target.getSendZip()));
                                    }
                                    logger.info("target set to : " + new ArrayList<>(signBook.getLiveWorkflow().getTargets()).get(0).getTargetUri());
                                }
                            }
                            j++;
                        } else {
                            targetService.copyTargets(workflow.getTargets(), signBook, null);
                            workflowService.importWorkflow(signBook, workflow, new ArrayList<>());
                            dispatchSignRequestParams(signBook);
                        }
                        fsAccessService.remove(fsFile);
                        nextStepAndPending(signBook.getId(), null, user.getEppn(), authUser.getEppn());
                        nbImportedFiles++;
                    }
                } else {
                    logger.info("aucun fichier à importer depuis : " + workflow.getProtectedDocumentsSourceUri());
                }
                fsAccessService.close();
            } else {
                logger.warn("aucun service de fichier n'est disponible");
            }
        }
        return nbImportedFiles;
    }

    /**
     * Récupère le prochain SignBook qu'un utilisateur doit signer, en fonction de l'état
     * des demandes de signature et des critères d'accès.
     *
     * @param signRequestId l'identifiant de la demande de signature actuelle
     * @param userEppn le nom principal de l'utilisateur authentifié
     * @param authUserEppn le nom principal de l'utilisateur ayant l'autorisation
     *                     d'accéder aux données
     * @return le prochain SignBook à signer, ou null s'il n'y en a pas
     */
    @Transactional
    public SignBook getNextSignBook(Long signRequestId, String userEppn, String authUserEppn) {
        SignRequest currentSignRequest = signRequestService.getById(signRequestId);
        List<SignBook> signBooksToSign = getSignBooks(userEppn, userEppn, "toSign", null, null, null, null, null, Pageable.unpaged()).toList();
        List<SignBook> signBooks = signBooksToSign.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).sorted(Comparator.comparingLong(SignBook::getId)).collect(Collectors.toList());
        if(!userEppn.equals(authUserEppn)) {
            signBooks = signBooks.stream().filter(signRequest -> userShareService.checkShareForSignRequest(userEppn, authUserEppn, signRequest, ShareType.sign)).toList();
        }
        int indexOfSignRequest = signBooks.indexOf(currentSignRequest.getParentSignBook());
        if (indexOfSignRequest + 1 >= signBooks.size()) {
            return signBooks.stream().filter(signBook -> !signBook.getId().equals(currentSignRequest.getParentSignBook().getId())).min(Comparator.comparingLong(SignBook::getId)).orElse(null);
        } else {
            if (currentSignRequest.getParentSignBook().getSignRequests().size() == 1) {
                return signBooks.get(indexOfSignRequest + 1);
            } else {
                if (indexOfSignRequest + currentSignRequest.getParentSignBook().getSignRequests().size() + 1 >= signBooks.size()) {
                    return signBooks.get(0);
                } else {
                    return signBooks.get(indexOfSignRequest + currentSignRequest.getParentSignBook().getSignRequests().size() + 1);
                }
            }
        }
    }

    /**
     * Retourne la prochaine demande de signature en attente dans le même SignBook ou dans un SignBook supplémentaire si fourni.
     *
     * @param signRequestId l'identifiant de la demande de signature actuelle
     * @param nextSignBook un SignBook supplémentaire contenant des demandes de signature
     * @return la prochaine demande de signature en attente si elle existe, sinon null
     */
    @Transactional
    public SignRequest getNextSignRequest(Long signRequestId, SignBook nextSignBook) {
        SignRequest currentSignRequest = signRequestService.getById(signRequestId);
        Optional<SignRequest> nextSignRequest = currentSignRequest.getParentSignBook().getSignRequests().stream().filter(s -> s.getStatus().equals(SignRequestStatus.pending) && !s.getId().equals(signRequestId)).findAny();
        if(nextSignRequest.isPresent()) {
            return nextSignRequest.get();
        }
        if(nextSignBook != null) {
            return nextSignBook.getSignRequests().get(0);
        }
        return null;
    }

    /**
     * Génère un fichier ZIP contenant plusieurs documents signés correspondant aux identifiants fournis.
     *
     * @param ids      liste des identifiants des SignBooks pour lesquels les documents signés doivent être récupérés.
     * @param response objet HttpServletResponse utilisé pour écrire le fichier ZIP result.
     * @throws IOException            si une erreur survient lors de l'écriture des fichiers dans la réponse HTTP.
     * @throws EsupSignatureFsException si une erreur liée au système de fichiers survient lors de la récupération des fichiers signés.
     */
    @Transactional
    public void getMultipleSignedDocuments(List<Long> ids, HttpServletResponse response) throws IOException, EsupSignatureFsException {
        List<FsFile> fsFiles = new ArrayList<>();
        for(Long id : ids) {
            SignBook signBook = getById(id);
            for (SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported)) {
                    FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
                    if(fsFile != null) {
                        fsFiles.add(fsFile);
                    }
                }
            }
        }
        if(fsFiles.isEmpty()) {
            throw new EsupSignatureRuntimeException("Aucun document à exporter : " + StringUtils.collectionToDelimitedString(ids, ","));
        }
        response.setContentType("application/zip; charset=utf-8");
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8) + ".zip");
        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
        int i = 0;
        for(FsFile fsFile : fsFiles) {
            zipOutputStream.putNextEntry(new ZipEntry(i + "_" + fsFile.getName()));
            IOUtils.copy(fsFile.getInputStream(), zipOutputStream);
            zipOutputStream.write(fsFile.getInputStream().readAllBytes());
            zipOutputStream.closeEntry();
            i++;
        }
        zipOutputStream.close();
    }

    /**
     * Cette méthode permet de récupérer plusieurs documents signés avec leurs rapports et de les compresser dans un fichier ZIP à télécharger.
     *
     * @param ids Liste des identifiants des SignBooks à traiter.
     * @param httpServletRequest Requête HTTP initiée par le client.
     * @param httpServletResponse Réponse HTTP utilisée pour envoyer le fichier ZIP généré au client.
     * @throws Exception En cas d'erreur lors de la récupération des données ou de la génération du fichier ZIP.
     */
    @Transactional
    public void getMultipleSignedDocumentsWithReport(List<Long> ids, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        httpServletResponse.setContentType("application/zip; charset=utf-8");
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8) + ".zip");
        Map<byte[], String> documents = new HashMap<>();
        for(Long id : ids) {
            SignBook signBook = getById(id);
            for (SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported))
                    documents.put(signRequestService.getZipWithDocAndReport(signRequest, httpServletRequest, httpServletResponse), signBook.getSubject());
            }
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(httpServletResponse.getOutputStream());
        int i = 0;
        for(Map.Entry<byte[], String> document : documents.entrySet()) {
            zipOutputStream.putNextEntry(new ZipEntry(i + "_" + document.getValue() + ".zip"));
            IOUtils.copy(new ByteArrayInputStream(document.getKey()), zipOutputStream);
            zipOutputStream.write(document.getKey());
            zipOutputStream.closeEntry();
            i++;
        }
        zipOutputStream.close();
    }

    /**
     * Sauvegarde un SignBook existant en tant que Workflow.
     *
     * @param signBookId L'identifiant du SignBook à sauvegarder.
     * @param title Le titre du Workflow à créer.
     * @param description La description du Workflow.
     * @param userEppn L'utilisateur qui initie la création du Workflow.
     * @throws EsupSignatureRuntimeException Si une erreur survient lors de l'exécution.
     */
    @Transactional
    public void saveSignBookAsWorkflow(Long signBookId, String title, String description, String userEppn) throws EsupSignatureRuntimeException {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        Workflow workflow = workflowService.createWorkflow(title, description, user);
        workflow.getViewers().addAll(signBook.getViewers());
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            List<RecipientWsDto> recipients = new ArrayList<>();
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                recipients.add(new RecipientWsDto(recipient.getUser().getEmail()));
            }
            WorkflowStep toSaveWorkflowStep = workflowStepService.createWorkflowStep("" , liveWorkflowStep.getAllSignToComplete(), liveWorkflowStep.getSignType(), false, recipients.toArray(RecipientWsDto[]::new));
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    private boolean needToSign(SignRequest signRequest, String userEppn) {
        boolean needSignInWorkflow = recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn);
        Recipient recipient = signRequest.getRecipientHasSigned().keySet().stream().filter(recipient1 -> recipient1.getUser().getEppn().equals(userEppn)).max(Comparator.comparing(Recipient::getId)).get();
        boolean needSign = signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none);
        return needSign || needSignInWorkflow;
    }

    /**
     * Vérifie si un utilisateur dispose des droits pour signer une demande de signature donnée.
     *
     * @param signRequest la demande de signature à vérifier
     * @param userEppn le eppn (identifiant unique) de l'utilisateur pour lequel les droits doivent être vérifiés
     * @param authUserEppn le eppn (identifiant unique) de l'utilisateur actuellement authentifié
     * @return true si l'utilisateur a les droits pour signer la demande de signature, false sinon
     */
    public boolean checkUserSignRights(SignRequest signRequest, String userEppn, String authUserEppn) {
        if(userEppn.equals(authUserEppn) || userShareService.checkShareForSignRequest(userEppn, authUserEppn, signRequest.getParentSignBook(), ShareType.sign)) {
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null) {
                Optional<Recipient> recipient = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().filter(r -> r.getUser().getEppn().equals(userEppn)).findFirst();
                if (recipient.isPresent()
                        && (signRequest.getStatus().equals(SignRequestStatus.pending) || signRequest.getStatus().equals(SignRequestStatus.draft))
                        && !signRequest.getRecipientHasSigned().isEmpty()
                        && signRequest.getRecipientHasSigned().get(recipient.get()).getActionType().equals(ActionType.none)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Récupère les images de signature associées à une demande de signature.
     *
     * @param id Identifiant de la demande de signature.
     * @param userEppn Identifiant EPPN de l'utilisateur effectuant l'opération.
     * @param authUserEppn Identifiant EPPN de l'utilisateur authentifié dans le contexte de l'opération.
     * @param userShareId Identifiant de l'utilisateur partagé (le cas échéant).
     * @return Une liste contenant les images de signature au format base64 prêtes à être utilisées ou affichées.
     * @throws EsupSignatureUserException Exception levée en cas d'erreur liée à l'utilisateur.
     * @throws IOException Exception levée en cas d'erreur d'entrée/sortie lors de la récupération des images.
     */
    @Transactional(readOnly = true)
    public List<String> getSignImagesForSignRequest(Long id, String userEppn, String authUserEppn, Long userShareId) throws EsupSignatureUserException, IOException {
        SignRequest signRequest = signRequestService.getById(id);
        User user = userService.getByEppn(userEppn);
        LinkedList<String> signImages = new LinkedList<>();
        if (!signRequest.getSignedDocuments().isEmpty() || !signRequest.getOriginalDocuments().isEmpty()) {
            List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
            if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
                if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
                    if(userShareId != null) {
                        try {
                            UserShare userShare = userShareService.getById(userShareId);
                            if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
                                user = userService.getByEppn(authUserEppn);
                            }
                        } catch (Exception e) {
                            logger.warn("unable to get shared user");
                        }
                    }
                    if (!user.getSignImages().isEmpty() && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
                        for (Document signImage : user.getSignImages()) {
                            signImages.add(fileService.getBase64Image(signImage));
                        }
                    }
                }
            }
        }
        signImages.add(fileService.getBase64Image(userService.getDefaultImage(authUserEppn), "default-image.png"));
        if(StringUtils.hasText(user.getName()) && StringUtils.hasText(user.getFirstname())) {
            signImages.add(fileService.getBase64Image(userService.getDefaultParaphe(authUserEppn), "default-paraphe.png"));
        }
        return signImages;
    }

    /**
     * Vérifie si une demande de signature est signable en fonction de son statut,
     * de l'état de suppression, des droits de l'utilisateur, de la présence de documents
     * originaux et de la nécessité de signer pour l'utilisateur.
     *
     * @param id l'identifiant unique de la demande de signature
     * @param userEppn l'identifiant de l'utilisateur supposé signer la demande
     * @param authUserEppn l'identifiant de l'utilisateur authentifié effectuant la vérification
     * @return true si la demande est signable, false sinon
     */
    @Transactional
    public boolean checkSignRequestSignable(Long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        boolean signable = false;
        if (signRequest.getStatus().equals(SignRequestStatus.pending)
                && !signRequest.getDeleted()
                && checkUserSignRights(signRequest, userEppn, authUserEppn)
                && !signRequest.getOriginalDocuments().isEmpty()
                && needToSign(signRequest, userEppn)) {
            signable = true;
            for(Document document : signRequest.getOriginalDocuments()) {
                if(document.getSize() == 0) {
                    return false;
                }
            }
        }
        return signable;
    }

    /**
     * Envoie les demandes de signature associées à un SignBook vers les cibles définies.
     * La méthode traite divers types de destinations, notamment les systèmes REST, les systèmes de fichiers,
     * et les envois par e-mail. Chaque cible peut nécessiter l'envoi des documents signés, des pièces jointes,
     * et éventuellement d'un rapport généré.
     *
     * @param id l'identifiant unique du SignBook à traiter
     * @param authUserEppn le nom principal (EPPN) de l'utilisateur authentifié initiant l'opération
     * @throws EsupSignatureRuntimeException si une erreur survient lors de l'envoi des demandes vers les cibles
     */
    @Transactional
    public void sendSignRequestsToTarget(Long id, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets() != null && !signBook.getLiveWorkflow().getTargets().isEmpty()) {
            List<SignRequest> signRequests = signBook.getSignRequests();
            String title = signBook.getSubject();
            Set<Target> targets = signBook.getLiveWorkflow().getTargets();
            boolean allTargetsDone = true;
            for (Target target : targets) {
                if (!target.getTargetOk()) {
                    if(target.getNbRetry() == 5) {
                        mailService.sendAdminError("La destination suivante n'est plus accessible : " + target.getTargetUri(), "La destination suivante n'est plus accessible : " + target.getTargetUri()  + ". Merci de contrôler la validité de la destination puis de relancer les exports au niveau de la configuration du circuit");
                        logger.error("La destination suivante n'est plus accessible : " + target.getTargetUri() + ". Merci de contrôler la validité de la destination puis de relancer les exports au niveau de la configuration du circuit");
                    }
                    if(target.getNbRetry() > 5) {
                        allTargetsDone = false;
                        continue;
                    }
                    DocumentIOType documentIOType = fsAccessFactoryService.getPathIOType(target.getTargetUri());
                    String targetUrl = target.getTargetUri();
                    if (documentIOType != null && !documentIOType.equals(DocumentIOType.none)) {
                        if (!documentIOType.equals(DocumentIOType.mail)) {
                            Map<InputStream, String> inputStreams = new HashMap<>();
                            for (SignRequest signRequest : signRequests) {
                                if (fsAccessFactoryService.getPathIOType(target.getTargetUri()).equals(DocumentIOType.rest)) {
                                    SignRequestStatus status = SignRequestStatus.completed;
                                    if (signRequest.getRecipientHasSigned().values().stream().anyMatch(action -> action.getActionType().equals(ActionType.refused))) {
                                        status = SignRequestStatus.refused;
                                    }
                                    try {
                                        targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), status.name(), "end", authUserEppn, "");
                                        target.setTargetOk(true);
                                        signRequestService.updateStatus(signRequest.getId(), signRequest.getStatus(), "Exporté vers " + targetUrl, null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
                                    } catch (Exception e) {
                                        logger.error("rest export fail : " + target.getTargetUri(), e);
                                        allTargetsDone = false;
                                    }
                                } else {
                                    String name = generateName(signRequest.getId(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), true, false, null);
                                    try {
                                        String finalTargetUrl = targetUrl;
                                        if(!target.getSendZip() && target.getSendAttachment()) {
                                            if (!finalTargetUrl.endsWith("/")) {
                                                finalTargetUrl += "/";
                                            }
                                            finalTargetUrl += documentService.sanitizeFileName(signRequest.getTitle()) + "/";
                                        }
                                        if (!signRequest.getAttachments().isEmpty() && (globalProperties.getExportAttachements() || target.getSendAttachment())) {
                                            for (Document attachment : signRequest.getAttachments()) {
                                                inputStreams.put(attachment.getInputStream(), attachment.getFileName());
                                            }
                                        }
                                        if (target.getSendDocument()) {
                                            Document signedFile = signRequest.getLastSignedDocument();
                                            String extension = "." + fileService.getExtension(signedFile.getFileName());
                                            if(!name.endsWith(extension)) name += extension;
                                            inputStreams.put(signedFile.getInputStream(), name);
                                        }
                                        if (target.getSendReport()) {
                                            try {
                                                byte[] fileBytes = reportService.getReportBytes(signRequest);
                                                if (fileBytes != null) {
                                                    inputStreams.put(new ByteArrayInputStream(fileBytes), name + "-report.zip");
                                                }
                                            } catch (Exception e) {
                                                target.setNbRetry(target.getNbRetry() + 1);
                                                throw new EsupSignatureRuntimeException(e.getMessage(), e);
                                            }
                                        }
                                        if(!target.getSendZip()) {
                                            for (Map.Entry<InputStream, String> inputStream : inputStreams.entrySet()) {
                                                try {
                                                    documentService.exportDocument(finalTargetUrl, inputStream.getKey(), inputStream.getValue(), null);
                                                } catch (EsupSignatureRuntimeException e) {
                                                    target.setNbRetry(target.getNbRetry() + 1);
                                                    targetService.forceSave(target);
                                                    logger.error(e.getMessage() + " : " + targetUrl);
                                                    allTargetsDone = false;
                                                }
                                            }
                                            inputStreams = new HashMap<>();
                                            target.setTargetOk(true);
                                        }
                                    } catch (EsupSignatureFsException e) {
                                        logger.error("fs export fail : " + target.getProtectedTargetUri(), e);
                                        allTargetsDone = false;
                                        target.setNbRetry(target.getNbRetry() + 1);
                                    }
                                }
                            }
                            if(target.getSendZip()) {
                                try {
                                    ByteArrayInputStream zip = new ByteArrayInputStream(fileService.zipDocuments(inputStreams));
                                    documentService.exportDocument(target.getTargetUri(), zip, signBook.getSubject() + ".zip", null);
                                    target.setTargetOk(true);
                                } catch (EsupSignatureFsException | IOException e) {
                                    logger.error("fs export fail : " + target.getProtectedTargetUri(), e);
                                    allTargetsDone = false;
                                }
                            }
                        }
                    }
                }
            }
            for (Target target : targets) {
                if (!target.getTargetOk()) {
                    DocumentIOType documentIOType = fsAccessFactoryService.getPathIOType(target.getTargetUri());
                    String targetUrl = target.getTargetUri();
                    if (documentIOType != null && !documentIOType.equals(DocumentIOType.none)) {
                        if (documentIOType.equals(DocumentIOType.mail)) {
                            logger.info("send by email to " + targetUrl);
                            try {
                                for (String email : targetUrl.replace("mailto:", "").split(",")) {
                                    User user = userService.getUserByEmail(email);
                                    if (user != null && !signBook.getViewers().contains(user)) {
                                        signBook.getViewers().add(user);
                                        addToTeam(signBook, user.getEppn());
                                    }
                                }
                                mailService.sendFile(title, signBook, targetUrl, target.getSendDocument(), target.getSendReport());
                                target.setTargetOk(true);
                            } catch (MessagingException | IOException e) {
                                logger.error("unable to send mail to : " + target.getTargetUri(), e);
                                allTargetsDone = false;
                            }
                        }
                    }
                }
            }
            if (allTargetsDone) {
                for (SignRequest signRequest : signRequests) {
                    signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.exported, "Exporté vers toutes les destinations", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
                }
                signRequests.get(0).getParentSignBook().setStatus(SignRequestStatus.exported);
            } else {
                throw new EsupSignatureRuntimeException("unable to send to all targets");
            }
        }
    }

    /**
     * Archive les demandes de signature associées à un SignBook spécifié.
     * Ce processus vérifie si le SignBook doit être archivé, détermine l'URI cible pour l'archivage,
     * et archive les documents signés correspondants. Une fois les documents archivés, leur état est mis à jour.
     *
     * @param signBookId L'identifiant unique du SignBook à archiver.
     * @param authUserEppn L'identifiant unique EPPN de l'utilisateur authentifié réalisant cette opération.
     * @throws EsupSignatureRuntimeException Si une erreur survient lors de l'opération d'archivage.
     */
    @Transactional
    public void archiveSignRequests(Long signBookId, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(signBookId);
        if(!needToBeArchived(signBook)) {
            return;
        }
        String archiveUri = globalProperties.getArchiveUri();
        if(signBook.getLiveWorkflow().getWorkflow() != null && StringUtils.hasText(signBook.getLiveWorkflow().getWorkflow().getArchiveTarget())) {
            if(signBook.getEndDate().after(signBook.getLiveWorkflow().getWorkflow().getStartArchiveDate())) {
                if(StringUtils.hasText(signBook.getLiveWorkflow().getWorkflow().getArchiveTarget())) {
                    archiveUri = signBook.getLiveWorkflow().getWorkflow().getArchiveTarget();
                }
            } else {
                return;
            }
        }
        if(archiveUri != null) {
            logger.info("start archiving documents");
            boolean result = true;
            for(SignRequest signRequest : signBook.getSignRequests()) {
                Document signedFile = signRequest.getLastSignedDocument();
                if(signedFile != null) {
                    String subPath = "/" + signRequest.getParentSignBook().getWorkflowName().replaceAll("[^a-zA-Z0-9]", "_") + "/";
                    if (signRequest.getExportedDocumentURI() == null) {
                        String name = generateName(signRequest.getId(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), false, true, null);
                        if(signRequest.getParentSignBook().getSignRequests().size() > 1) {
                            name = fileService.getNameOnly(signedFile.getFileName());
                        }
                        String documentUri = documentService.archiveDocument(signedFile, archiveUri, subPath, signedFile.getId() + "_" + name);
                        if (documentUri != null) {
                            signRequest.setExportedDocumentURI(documentUri);
                            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.completed, "Archivé vers " + archiveUri, null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
                            signRequest.setArchiveStatus(ArchiveStatus.archived);
                            logger.info("archive done to " + subPath + name + " in " + archiveUri);
                        } else {
                            logger.error("unable to archive " + subPath + name + " in " + archiveUri);
                            result = false;
                        }
                    }
                }
            }
            if(result) {
                signBook.setArchiveStatus(ArchiveStatus.archived);
            }
        } else {
            logger.debug("archive document was skipped");
        }
    }

    /**
     * Nettoie les fichiers liés à un SignBook en fonction de son identifiant et
     * de l'utilisateur authentifié.
     *
     * @param signBookId L'identifiant du SignBook à nettoyer.
     * @param authUserEppn L'identifiant de l'utilisateur authentifié responsable de l'action.
     */
    @Transactional
    public void cleanFiles(Long signBookId, String authUserEppn) {
        SignBook signBook = getById(signBookId);
        int nbDocOnDataBase = 0;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.cleanDocuments(signRequest, authUserEppn);
            nbDocOnDataBase += signRequest.getSignedDocuments().size();
        }
        if(nbDocOnDataBase == 0) {
            logger.info(signBook.getSubject() + " :  " + signBook.getId() + " cleaned");
            signBook.setArchiveStatus(ArchiveStatus.cleaned);
        }
    }

    /**
     * Vérifie si un SignBook doit être exporté.
     *
     * @param signBookId L'identifiant du SignBook à vérifier.
     * @return true si le SignBook est terminé, qu'il possède un workflow actif et des cibles définies,
     *         sinon false.
     */
    @Transactional
    public boolean needToBeExported(Long signBookId) {
        SignBook signBook = getById(signBookId);
        return signBook.getStatus().equals(SignRequestStatus.completed) && signBook.getLiveWorkflow() != null && !signBook.getLiveWorkflow().getTargets().isEmpty();
    }

    /**
     * Détermine si un SignBook doit être archivé en fonction de son état de workflow en cours.
     *
     * @param signBook L'objet SignBook à évaluer. Ce dernier contient des informations sur le workflow en cours.
     * @return true si le SignBook doit être archivé, false sinon. La condition est remplie si le workflow en cours existe
     *         mais n'est pas défini, ou si une date de début d'archivage est spécifiée, qu'une cible d'archivage est
     *         renseignée, et que la date de début d'archivage se situe avant la date actuelle.
     */
    @Transactional
    public boolean needToBeArchived(SignBook signBook) {
        return signBook.getLiveWorkflow() != null
                && (signBook.getLiveWorkflow().getWorkflow() == null
                    || (signBook.getLiveWorkflow().getWorkflow().getStartArchiveDate() != null
                    && StringUtils.hasText(signBook.getLiveWorkflow().getWorkflow().getArchiveTarget())
                    && signBook.getLiveWorkflow().getWorkflow().getStartArchiveDate().before(new Date())
                    )
        );
    }

    private String generateName(Long signRequestId, Workflow workflow, User user, Boolean target, Boolean archive, Long signBookId) {
        SignBook signBook;
        SignRequest signRequest = null;
        if(signBookId != null) {
            signBook = getById(signBookId);
            if(!signBook.getSignRequests().isEmpty()) {
                signRequest = signBook.getSignRequests().get(0);
            }
        } else {
            signRequest = signRequestService.getById(signRequestId);
            signBook = signRequest.getParentSignBook();
        }
        String template = globalProperties.getNamingTemplate();
        if(archive && StringUtils.hasText(globalProperties.getNamingTemplateArchive())) {
            template = globalProperties.getNamingTemplateArchive();
        }
        if(workflow == null) {
            workflow = signBook.getLiveWorkflow().getWorkflow();
        }
        if(workflow != null) {
            if(target) {
                if(workflow.getTargetNamingTemplate() != null && !workflow.getTargetNamingTemplate().isEmpty()) {
                    template = workflow.getTargetNamingTemplate();
                } else {
                    template = "[signedFileName]";
                }
            } else {
                if(workflow.getNamingTemplate() != null && !workflow.getNamingTemplate().isEmpty()) {
                    template = workflow.getNamingTemplate();
                }
            }
        }
        if(!StringUtils.hasText(signBook.getSubject())) {
            if(!signBook.getSignRequests().isEmpty() && signRequest != null) {
                signBook.setSubject(fileService.getNameOnly(signRequest.getOriginalDocuments().get(0).getFileName()));
                if(signBook.getSignRequests().size() > 1) {
                    signBook.setSubject(fileService.getNameOnly(signRequest.getOriginalDocuments().get(0).getFileName()) + ", ...");
                }
            } else {
                if(workflow != null) {
                    signBook.setSubject(workflow.getName());
                } else {
                    signBook.setSubject("Sans titre");
                }
            }
        }
        int order = 0;
        if(workflow != null) {
            order = signBookRepository.countByLiveWorkflowWorkflow(workflow);
        }
        if(template.isEmpty()) {
            template = globalProperties.getNamingTemplate();
        }
        if(template.contains("[id]")) {
            template = template.replace("[id]", signBook.getId() + "");
        }
        if(template.contains("[title]")) {
            template = template.replace("[title]", signBook.getSubject());
        }
        if(template.contains("[originalFileName]")) {
            if(!signBook.getSignRequests().isEmpty() && signRequest != null && !signRequest.getOriginalDocuments().isEmpty()) {
                template = template.replace("[originalFileName]", signRequest.getOriginalDocuments().get(0).getFileName());
            } else {
                logger.warn("no original file name");
                template = template.replace("[originalFileName]", "");
            }
        }
        if(template.contains("[signedFileName]")) {
            if(!signBook.getSignRequests().isEmpty() && signRequest != null && !signRequest.getSignedDocuments().isEmpty()) {
                template = template.replace("[signedFileName]", signRequest.getSignedDocuments().get(0).getFileName());
            } else {
                logger.warn("no signed file name");
                template = template.replace("[signedFileName]", "");
            }
        }
        if(template.contains("[fileNameOnly]")) {
            if(!signBook.getSignRequests().isEmpty() && signRequest != null && !signRequest.getSignedDocuments().isEmpty()) {
                template = template.replace("[fileNameOnly]", fileService.getNameOnly(signRequest.getSignedDocuments().get(0).getFileName()));
            } if(!signBook.getSignRequests().isEmpty() && signRequest != null && !signRequest.getOriginalDocuments().isEmpty()) {
                template = template.replace("[fileNameOnly]", fileService.getNameOnly(signRequest.getOriginalDocuments().get(0).getFileName()));
            } else {
                logger.warn("no file name");
                template = template.replace("[fileNameOnly]", "");
            }
        }
        if(template.contains("[fileExtension]") && signRequest != null) {
            template = template.replace("[fileExtension]", fileService.getExtension(signRequest.getSignedDocuments().get(0).getFileName()));
        }
        if(template.contains("[worflowName]")) {
            template = template.replace("[worflowName]", signBook.getWorkflowName());
        }
        if(template.contains("[workflowName]")) {
            template = template.replace("[workflowName]", signBook.getWorkflowName());
        }
        if(template.contains("[user.eppn]")) {
            template = template.replace("[user.eppn]", user.getEppn());
        }
        if(template.contains("[user.name]")) {
            template = template.replace("[user.name]", user.getFirstname() + " " + user.getName());
        }
        if(template.contains("[user.initials]")) {
            template = template.replace("[user.initials]", user.getName().substring(0,1).toUpperCase() + user.getFirstname().substring(0,1).toUpperCase());
        }
        if(template.contains("[UUID]")) {
            template = template.replace("[UUID]", UUID.randomUUID().toString());
        }
        if(template.contains("[order]")) {
            template = template.replace("[order]", order + "");
        }
        if(template.contains("[timestamp]")) {
            Date date = Calendar.getInstance().getTime();
            template = template.replace("[timestamp]", date.getTime() + "");
        }
        if(template.contains("[date-fr]")) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("ddMMyyyyhhmm");
            String strDate = dateFormat.format(date);
            template = template.replace("[date-fr]", strDate);
        }
        if(template.contains("[date-en]")) {
            Date date = Calendar.getInstance().getTime();
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmm");
            String strDate = dateFormat.format(date);
            template = template.replace("[date-en]", strDate);
        }
        if(signBook.getSignRequests().size() == 1) {
            Data data = dataService.getBySignRequest(signBook.getSignRequests().get(0));
            if(data != null) {
                for(Map.Entry<String, String> entry: data.getDatas().entrySet()) {
                    if(template.contains("[form." + entry.getKey() + "]")) {
                        template = template.replace("[form." + entry.getKey() + "]", entry.getValue());
                    }
                }

            }
        }
        if(archive || target) {
            return template.substring(0, Math.min(template.length(), 256));
        }
        return template;
    }

    /**
     * Récupère une liste de créateurs (users) selon les filtres fournis.
     *
     * @param userEppn L'identifiant EPPN de l'utilisateur pour lequel récupérer les créateurs.
     * @param workflowFilter Filtre associé au workflow pour restreindre les résultats.
     * @param docTitleFilter Filtre sur le titre des documents pour affiner la recherche.
     * @param creatorFilter Identifiant EPPN du créateur à filtrer spécifiquement, si fourni.
     * @return Une liste d'objets UserDto contenant les informations des créateurs correspondants aux critères.
     */
    public List<UserDto> getCreators(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter) {
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getByEppn(creatorFilter);
        }
        User user = userService.getByEppn(userEppn);
        return signBookRepository.findUserByRecipientAndCreateBy(user, workflowFilter, docTitleFilter, creatorFilterUser);
    }

    /**
     * Récupère une liste de SignBook associés à l'utilisateur spécifié.
     *
     * @param userEppn l'identifiant unique de l'utilisateur (eppn)
     * @return une liste de SignBook associés à l'utilisateur
     */
    public List<SignBook> getSignBookForUsers(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return signBookRepository.findByTeamContaining(user);
    }

    /**
     * Transfère toutes les demandes de signature en cours d'un utilisateur à un autre si cet utilisateur est remplacé.
     *
     * @param authUserEppn l'identifiant EPPN de l'utilisateur authentifié.
     * @return le nombre de demandes de signature transférées.
     */
    @Transactional
    public int transfer(String authUserEppn) {
        int i = 0;
        User user = userService.getByEppn(authUserEppn);
        User replacedByUser = user.getCurrentReplaceByUser();
        if(replacedByUser != null) {
            List<SignRequest> signRequests = getSignBookForUsers(authUserEppn).stream().filter(signBook -> signBook.getStatus().equals(SignRequestStatus.pending)).flatMap(signBook -> signBook.getSignRequests().stream().distinct()).collect(Collectors.toList());
            for(SignRequest signRequest : signRequests) {
                transfertSignRequest(signRequest.getId(), true, user, replacedByUser, false);
                i++;
            }
        }
        return i;
    }

    /**
     * Transfère une demande de signature d'un utilisateur à un autre.
     *
     * @param signRequestId l'identifiant de la demande de signature à transférer
     * @param userEppn l'identifiant eppn de l'utilisateur actuel (propriétaire de la demande de signature)
     * @param replacedByUserEmail l'adresse email de l'utilisateur vers lequel la demande de signature sera transférée
     * @param keepFollow un indicateur booléen pour savoir si l'utilisateur actuel doit continuer de suivre la demande de signature après le transfert
     * @throws EsupSignatureRuntimeException si le transfert est impossible
     */
    @Transactional
    public void transfertSignRequest(Long signRequestId, String userEppn, String replacedByUserEmail, boolean keepFollow) {
        if(checkSignRequestSignable(signRequestId, userEppn, userEppn)) {
            User user = userService.getByEppn(userEppn);
            User replacedByUser = userService.getUserByEmail(replacedByUserEmail);
            if (user.equals(replacedByUser)) {
                throw new EsupSignatureRuntimeException("Transfer impossible");
            }
            transfertSignRequest(signRequestId, false, user, replacedByUser, keepFollow);
        } else {
            throw new EsupSignatureRuntimeException("Transfer impossible");
        }
    }

    /**
     * Transfère une demande de signature d'un utilisateur à un autre. Permet de remplacer les tâches de signature et/ou de
     * conserver les droits de suivi pour l'utilisateur initial.
     *
     * @param signRequestId l'identifiant unique de la demande de signature à transférer
     * @param transfertAll détermine si toutes les étapes de workflow associées doivent être transférées
     *                     (true pour toutes les étapes, false pour seulement l'étape actuelle)
     * @param user l'utilisateur source de transfert, c'est-à-dire l'utilisateur actuellement associé à la demande
     * @param replacedByUser l'utilisateur remplaçant, c'est-à-dire l'utilisateur qui prendra en charge la demande de signature
     * @param keepFollow détermine si l'utilisateur source doit garder les droits de suivi sur la demande de signature
     */
    @Transactional
    public void transfertSignRequest(Long signRequestId, boolean transfertAll, User user, User replacedByUser, boolean keepFollow) {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        signRequest.getParentSignBook().getTeam().remove(user);
        addToTeam(signRequest.getParentSignBook(), user.getEppn());
        List<LiveWorkflowStep> liveWorkflowSteps = new ArrayList<>();
        if(transfertAll) {
            liveWorkflowSteps.addAll(signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps());
        } else {
            liveWorkflowSteps.add(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep());
        }
        for(LiveWorkflowStep liveWorkflowStep : liveWorkflowSteps) {
            for(Recipient recipient : liveWorkflowStep.getRecipients()) {
                if(recipient.getUser().equals(user) && signRequest.getRecipientHasSigned().get(recipient) != null && signRequest.getRecipientHasSigned().get(recipient).getActionType() != null && signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none)) {
                    recipient.setUser(replacedByUser);
                }
            }
        }
        mailService.sendSignRequestAlert(Collections.singletonList(replacedByUser.getEmail()), signRequest.getParentSignBook());
        if(keepFollow) {
            addViewers(signRequest.getParentSignBook().getId(), Collections.singletonList(user.getEmail()));
        }
    }

    @Transactional
    public void anonymize(String userEppn, User anonymous) {
        User user = userService.getByEppn(userEppn);
        for(SignBook signBook : signBookRepository.findByCreateByEppn(userEppn)) {
            signBook.setCreateBy(anonymous);
        }
        for(SignBook signBook : signBookRepository.findByHidedById(user, Pageable.unpaged())) {
            signBook.getHidedBy().remove(user);
        }
    }

    /**
     * Méthode pour nettoyer les SignBooks en cours d'upload ou vides.
     * Cette méthode identifie et supprime les SignBooks qui sont dans l'une
     * des conditions suivantes :
     *
     * - Les SignBooks qui ne contiennent aucun élément.
     * - Les SignBooks dont le statut est "uploading".
     *
     * La suppression se fait de manière définitive en appelant la méthode
     * deleteDefinitive pour chaque SignBook correspondant.
     */
    @Transactional
    public void cleanUploadingSignBooks() {
        List<SignBook> toDelete = new ArrayList<>();
        toDelete.addAll(signBookRepository.findEmpties());
        toDelete.addAll(signBookRepository.findByStatus(SignRequestStatus.uploading));
        for(SignBook signBook : toDelete){
            deleteDefinitive(signBook.getId(), "system");
        }
    }

    /**
     * Vérifie si un utilisateur a les droits de visualisation d'un signBook spécifique.
     *
     * @param userEppn l'identifiant unique de l'utilisateur concerné par la vérification des droits
     * @param authUserEppn l'identifiant unique de l'utilisateur authentifié effectuant la demande
     * @param signBookId l'identifiant unique du signBook à vérifier
     * @return true si l'utilisateur possède les droits de visualisation, false sinon
     */
    @Transactional
    public boolean checkUserViewRights(String userEppn, String authUserEppn, Long signBookId) {
        SignBook signBook = getById(signBookId);
        if(signBook == null) return false;
        List<Recipient> recipients = new ArrayList<>();
        for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            recipients.addAll(liveWorkflowStep.getRecipients());
        }
        if(!signBook.getSignRequests().isEmpty() && checkAllShareTypesForSignRequest(userEppn, authUserEppn, signBook.getId())
                || signBook.getViewers().stream().anyMatch(u -> u.getEppn().equals(authUserEppn))
                || signBook.getCreateBy().getEppn().equals(authUserEppn)
                || recipientService.recipientsContainsUser(recipients, authUserEppn) > 0
                || (signBook.getLiveWorkflow().getWorkflow() != null && workflowService.checkWorkflowManageRights(signBook.getLiveWorkflow().getWorkflow().getId(), authUserEppn))) {
            return true;
        }
        return false;
    }

    /**
     * Vérifie si tous les types de partage sont présents pour une demande de signature entre deux utilisateurs
     * pour un SignBook donné.
     *
     * @param fromUserEppn EPPN de l'utilisateur source qui partage une demande de signature.
     * @param toUserEppn EPPN de l'utilisateur destinataire du partage.
     * @param signBookId Identifiant du livre de signature concerné.
     * @return {@code true} si au moins un type de partage est valide pour la demande de signature,
     *         sinon {@code false}.
     */
    @Transactional
    public Boolean checkAllShareTypesForSignRequest(String fromUserEppn, String toUserEppn, Long signBookId) {
        SignBook signBook = getById(signBookId);
        for(ShareType shareType : ShareType.values()) {
            if(userShareService.checkShareForSignRequest(fromUserEppn, toUserEppn, signBook, shareType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renouvelle le code OTP (One-Time Password) pour une demande de signature en fonction de l'identifiant d'URL fourni.
     *
     * @param urlId l'identifiant unique de l'URL associée à l'OTP et à la demande de signature
     * @param signature indique si la signature liée à la demande doit être prise en compte
     * @return true si le renouvellement de l'OTP a réussi, false sinon
     */
    @Transactional
    public boolean renewOtp(String urlId, boolean signature) {
        Otp otp = otpService.getOtpFromDatabase(urlId);
        if(otp != null) {
            SignBook signBook = otp.getSignBook();
            if (signBook != null) {
                SignRequest signRequest = signBook.getSignRequests().stream().filter(s -> s.getArchiveStatus().equals(ArchiveStatus.none) || !s.getDeleted()).findFirst().orElse(null);
                if (signRequest != null) {
                    List<Recipient> recipients = signRequest.getRecipientHasSigned().keySet().stream().filter(r -> r.getUser().getUserType().equals(UserType.external)).toList();
                    for (Recipient recipient : recipients) {
                        try {
                            otpService.generateOtpForSignRequest(signBook.getId(), recipient.getUser().getId(), recipient.getUser().getPhone(), signature);
                            return true;
                        } catch (EsupSignatureMailException e) {
                            logger.error(e.getMessage());
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Finalise une requête de signature en fonction de son identifiant et de l'utilisateur authentifié.
     *
     * @param id l'identifiant unique de la requête de signature à compléter
     * @param authUserEppn le eppn (eduPersonPrincipalName) de l'utilisateur authentifié
     * @param text le texte associé à la finalisation de la requête
     */
    @Transactional
    public void completeSignRequest(Long id, String authUserEppn, String text) {
        SignRequest signRequest = signRequestService.getById(id);
        completeSignBook(signRequest.getParentSignBook(), authUserEppn, text);
    }

    /**
     * Met à jour l'état de la demande de signature en la passant à l'état "en attente".
     *
     * @param id L'identifiant unique de la demande de signature.
     * @param data Les données associées à la demande de signature.
     * @param userEppn L'identifiant de l'utilisateur effectuant cette opération (EPPN - EduPersonPrincipalName).
     * @param authUserEppn L'identifiant de l'utilisateur authentifié effectuant l'action (EPPN - EduPersonPrincipalName).
     * @param forceSendEmail Indique si un email doit être envoyé de manière forcée lors de la mise à jour.
     */
    @Transactional
    public void pendingSignRequest(Long id, Data data, String userEppn, String authUserEppn, boolean forceSendEmail) {
        SignRequest signRequest = signRequestService.getById(id);
        pendingSignBook(signRequest.getParentSignBook(), data, userEppn, authUserEppn, forceSendEmail, true);
    }

    private void addToTeam(SignBook signBook, String userEppn) {
        User user = userService.getByEppn(userEppn);
        if(signBook.getTeam().stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            signBook.getTeam().add(user);
        }
    }

    /**
     * Récupère les sujets des SignBooks pour les managers correspondant à un workflow et une recherche donnée.
     *
     * @param workflowId l'identifiant du workflow pour lequel récupérer les SignBooks
     * @param searchString la chaîne de recherche pour filtrer les sujets
     * @return une liste de chaînes représentant les sujets des SignBooks correspondant aux critères
     */
    @Transactional
    public List<String> getSignBooksForManagersSubjects(Long workflowId, String searchString) {
        return signBookRepository.findByWorkflowNameSubjects(workflowId, "%"+searchString+"%");
    }

    /**
     * Clone une demande de signature existante et crée un nouveau carnet de signature associé.
     *
     * Cette méthode permet de cloner une demande de signature en respectant les autorisations de clonage définies.
     * Un nouveau carnet de signatures et une nouvelle demande de signature sont créés avec les documents fournis,
     * tout en préservant les étapes du flux de travail initial.
     *
     * @param id L'identifiant de la demande de signature à cloner.
     * @param multipartFiles Les fichiers à associer à la nouvelle demande de signature.
     * @param comment Un commentaire à ajouter à la nouvelle demande ou au carnet cloné.
     * @param authUserEppn L'identifiant de l'utilisateur authentifié qui initie le clonage.
     * @return L'identifiant de la nouvelle demande de signature créée.
     * @throws RuntimeException Si le processus de clonage n'est pas autorisé pour la demande spécifiée.
     */
    @Transactional
    public Long clone(Long id, MultipartFile[] multipartFiles, String comment, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getLiveWorkflow().getWorkflow() != null && ! signBook.getLiveWorkflow().getWorkflow().getAuthorizeClone()) {
            throw new RuntimeException("clonage non autorisé pour : " + id);
        }
        String name = "Demande simple";
        if(signBook.getLiveWorkflow().getWorkflow() != null) name = signBook.getLiveWorkflow().getWorkflow().getName();
        SignBook newSignBook = createSignBook(
                signBook.getSubject(),
                signBook.getLiveWorkflow().getWorkflow(),
                name,
                authUserEppn,
                true,
                comment
        );
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            newSignBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.cloneLiveWorkflowStep(newSignBook, null, liveWorkflowStep));
        }
        newSignBook.getLiveWorkflow().setCurrentStep(newSignBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
        SignRequest newSignRequest = signRequestService.createSignRequest(signRequest.getTitle(), newSignBook, authUserEppn, authUserEppn);
        signRequestService.addDocsToSignRequest(newSignRequest, true, 0, new ArrayList<>(), multipartFiles);
        pendingSignBook(newSignBook, null, authUserEppn, authUserEppn, false, true);
        signRequestService.addAttachement(null, globalProperties.getRootUrl() + "/user/signrequests/" + id, newSignRequest.getId(), authUserEppn);
        return newSignRequest.getId();
    }

    @Transactional
    public List<ExternalAuth> getExternalAuths(Long id, List<OidcOtpSecurityService> securityServices) {
        List<ExternalAuth> externalAuths = new ArrayList<>();
        SignBook signBook = getById(id);
        if(signBook.getLiveWorkflow().getWorkflow() != null && !signBook.getLiveWorkflow().getWorkflow().getExternalAuths().isEmpty()) {
            externalAuths.addAll(signBook.getLiveWorkflow().getWorkflow().getExternalAuths());
            if(BooleanUtils.isTrue(globalProperties.getSmsRequired())) {
                externalAuths.remove(ExternalAuth.open);
            }
        } else {
            if(securityServices.stream().anyMatch(s -> s instanceof ProConnectSecurityServiceImpl)) {
                externalAuths.add(ExternalAuth.proconnect);
            }
            if(securityServices.stream().anyMatch(s -> s instanceof FranceConnectSecurityServiceImpl)) {
                externalAuths.add(ExternalAuth.franceconnect);
            }
            if(BooleanUtils.isFalse(globalProperties.getSmsRequired())) {
                externalAuths.add(ExternalAuth.open);
            } else {
                if(BooleanUtils.isTrue(smsProperties.getEnableSms())) {
                    externalAuths.add(ExternalAuth.sms);
                }
            }
        }
        return externalAuths;
    }

    /**
     * Vérifie si un SignBook contient des utilisateurs temporaires (utilisateur pas encore présent en base).
     *
     * @param signBookId l'identifiant du carnet de signatures à vérifier
     * @return true si le carnet de signatures contient des utilisateurs temporaires, false sinon
     */
    @Transactional
    public boolean isTempUsers(Long signBookId) {
        SignBook signBook = signBookRepository.findById(signBookId).orElseThrow();
        return !userService.getTempUsers(signBook).isEmpty();
    }
}
