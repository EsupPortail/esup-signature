package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.validation.reports.Reports;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.FOPService;
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
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.metric.CustomMetricsService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.support.MutableSortDefinition;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.beans.support.SortDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    private final GlobalProperties globalProperties;

    @Resource
    private MessageSource messageSource;

    @Resource
    private AuditTrailService auditTrailService;

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Resource
    private FsAccessFactoryService fsAccessFactoryService;

    @Resource
    private WebUtilsService webUtilsService;

    @Resource
    private FileService fileService;

    @Resource
    private PdfService pdfService;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private MailService mailService;

    @Resource
    private WorkflowStepService workflowStepService;

    @Resource
    private LiveWorkflowService liveWorkflowService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private DataService dataService;

    @Resource
    private LogService logService;

    @Resource
    private TargetService targetService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private CommentService commentService;

    @Resource
    private OtpService otpService;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private UserShareService userShareService;

    @Resource
    private SignService signService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private ValidationService validationService;

    @Resource
    private DocumentService documentService;

    @Resource
    private SignRequestParamsService signRequestParamsService;

    @Resource
    private CustomMetricsService customMetricsService;

    @Resource
    private PreFillService preFillService;

    @Resource
    private ReportService reportService;

    @Resource
    private ActionService actionService;

    @Resource
    private FOPService fopService;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private ObjectMapper objectMapper;

    public GlobalProperties getGlobalProperties() {
        return globalProperties;
    }

    public SignBookService(GlobalProperties globalProperties) {
        this.globalProperties = globalProperties;
    }

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(list::add);
        return list;
    }

    @Transactional
    public int countSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).get();
        return signBookRepository.countByLiveWorkflowWorkflow(workflow);
    }

    public Page<SignBook> getSignBooks(String userEppn, String statusFilter, String recipientsFilter, String workflowFilter, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        User user = userService.getUserByEppn(userEppn);
        Calendar calendar = Calendar.getInstance();
        calendar.set(9999, Calendar.DECEMBER, 31);
        Date startDateFilter = new Date(0);
        Date endDateFilter = calendar.getTime();
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date formattedDate = null;
            try {
                formattedDate = formatter.parse(dateFilter);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
            LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
            startDateFilter = Timestamp.valueOf(startLocalDateTime);
            endDateFilter = Timestamp.valueOf(endLocalDateTime);
        }
        Page<SignBook> signBooks = new PageImpl<>(new ArrayList<>());
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getUserByEppn(creatorFilter);
        }
        User userFilter = null;
        if(recipientsFilter != null && !recipientsFilter.equals("%") && !recipientsFilter.isEmpty()) {
            userFilter = userService.getUserByEppn(recipientsFilter);
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
//            signBooks = signBookRepository.findByViewersContaining(userService.getUserByEppn(userEppn), pageable);
            //TODO
        } else if(statusFilter.equals("hided")) {
            signBooks = signBookRepository.findByHidedById(user, pageable);
        } else if(statusFilter.equals("empty")) {
            signBooks = signBookRepository.findEmpty(user, pageable);
        } else {
            signBooks = signBookRepository.findByCreateByIdAndStatusAndSignRequestsNotNull(user, SignRequestStatus.valueOf(statusFilter), pageable);
        }
        return signBooks;
    }

    @Transactional
    public Page<SignBook> getAllSignBooks(String statusFilter, String workflowFilter, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(9999, Calendar.DECEMBER, 31);
        Date startDateFilter = new Date(0);
        Date endDateFilter = calendar.getTime();
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date formattedDate = null;
            try {
                formattedDate = formatter.parse(dateFilter);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
            LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
            startDateFilter = Timestamp.valueOf(startLocalDateTime);
            endDateFilter = Timestamp.valueOf(endLocalDateTime);
        }
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getUserByEppn(creatorFilter);
        }
        SignRequestStatus status = null;
        if(statusFilter != null && !statusFilter.isEmpty()) {
            status = SignRequestStatus.valueOf(statusFilter);
        }
        Page<SignBook> signBooks = signBookRepository.findSignBooksAllPaged(status, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
        for(SignBook signBook : signBooks) {
            if(signBook.getEndDate() == null &&
                    (signBook.getStatus().equals(SignRequestStatus.completed)
                            || signBook.getStatus().equals(SignRequestStatus.exported)
                            || signBook.getStatus().equals(SignRequestStatus.refused)
                            || signBook.getStatus().equals(SignRequestStatus.signed)
                            || signBook.getStatus().equals(SignRequestStatus.archived)
                            || signBook.getStatus().equals(SignRequestStatus.deleted))) {
                List<Action> actions = signBook.getSignRequests().stream().map(SignRequest::getRecipientHasSigned).map(Map::values).flatMap(Collection::stream).filter(action -> action.getDate() != null).sorted(Comparator.comparing(Action::getDate).reversed()).collect(Collectors.toList());
                if(actions.size() > 0) {
                    signBook.setEndDate(actions.get(0).getDate());
                }
            }
        }
        return signBooks;
    }

    @Transactional
    public SignBook addFastSignRequestInNewSignBook(MultipartFile[] multipartFiles, SignType signType, String userEppn, String authUserEppn) throws EsupSignatureException {
        logger.info("création rapide demande de signature par " + userEppn);
        try {
            String name = fileService.getNameOnly(multipartFiles[0].getOriginalFilename());
            SignBook signBook = addDocsInNewSignBookSeparated(name, "Auto signature", multipartFiles, userEppn);
            initSignBookWorkflow(signBook.getId(), signType, userEppn);
//            pendingSignBook(signBook.getId(), null, user.getEppn(), authUserEppn, false);
            return signBook;
        } catch (EsupSignatureIOException e) {
            throw new EsupSignaturePdfException("Impossible de charger le document suite à une erreur interne", e);
        }
    }

    @Transactional
    public void initSignBookWorkflow(Long signBookId, SignType signType, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(signBook, null,false, null,true, false, false, signType, Collections.singletonList(user.getEmail()), null));
        signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
        workflowService.dispatchSignRequestParams(signBook);
        if(signBook.getSubject().isEmpty()) {
            signBook.setSubject(generateName(signBook, null, user, false));
        }
        signBook.setStatus(SignRequestStatus.draft);
    }

    @Transactional
    public void finishSignBookUpload(Long signBookId, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignBook signBook = signBookRepository.findWithLockingById(signBookId).orElseThrow();
        if(signBook.getSubject().isEmpty()) {
            signBook.setSubject(generateName(signBook, null, user, false));
        }
        signBook.setStatus(SignRequestStatus.draft);
    }

    public List<User> getRecipientsNames(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        return signBookRepository.findRecipientNames(user);
    }

    @Transactional
    public SignBook createSignBook(String subject, Workflow workflow, String workflowName, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignBook signBook = new SignBook();
        if(!StringUtils.hasText(workflowName)) {
            if(workflow != null) {
                if(workflow.getDescription() != null && !workflow.getDescription().isEmpty()) {
                    workflowName = workflow.getDescription();
                } else if(workflow.getTitle() != null && !workflow.getTitle().isEmpty()) {
                    workflowName = workflow.getTitle();
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
        signBook.getTeam().add(user);
        signBook.setCreateDate(new Date());
        signBook.setLiveWorkflow(liveWorkflowService.create(workflowName));
        signBook.setSubject(subject);
        signBookRepository.save(signBook);
        subject = generateName(signBook, workflow, user, false);
        signBook.setSubject(subject);
        return signBook;
    }

    @Transactional
    public SignBook createFullSignBook(String title, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsCCEmails, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, String userEppn, String authUserEppn, boolean forceSendEmail, Boolean forceAllSign) throws EsupSignatureException {
        if(forceAllSign == null) forceAllSign = false;
        if(title == null || title.isEmpty()) {
            title = "Parapheur pour " + recipientsEmails.get(0);
            if(recipientsEmails.size() > 1) {
                title += " ...";
            }
        }
        logger.info(title);
        SignBook signBook = createSignBook(title, null, "Demande simple", userEppn);
        signBook.setForceAllDocsSign(forceAllSign);
        if(recipientsCCEmails != null) {
            addViewers(signBook.getId(), recipientsCCEmails);
        }
        Map<SignBook, String> signBookStringMap = sendSignBook(signBook, signType, allSignToComplete, userSignFirst, pending, comment, recipientsEmails, externalUsersInfos, userEppn, authUserEppn, forceSendEmail);
        return new ArrayList<>(signBookStringMap.keySet()).get(0);
    }

    @Transactional
    public void updateSignBook(Long id, String subject, String description, List<String> viewers) {
        SignBook signBook = getById(id);
        signBook.setSubject(subject);
        signBook.setDescription(description);
        addViewers(id, viewers);
    }

    @Transactional
    public void initSignBook(Long signBookId, Long id, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(user.equals(signBook.getCreateBy())) {
            Workflow workflow = workflowRepository.findById(id).get();
            signBook.getLiveWorkflow().setWorkflow(workflow);
            for(Target target : workflow.getTargets()) {
                signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri()));
            }
        }
    }

    @Transactional
    public SignBook getById(Long id) {
        return signBookRepository.findById(id).orElseThrow();
    }

    public List<SignBook> getByWorkflowId(Long id) {
        return signBookRepository.findByWorkflowId(id);
    }

    @Transactional
    public Boolean delete(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        if(signBook.getStatus().equals(SignRequestStatus.deleted)) {
            deleteDefinitive(signBookId, userEppn);
            return true;
        }
        List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
        for(Long signRequestId : signRequestsIds) {
            signRequestService.deleteSignRequest(signRequestId, userEppn);
        }
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            if(liveWorkflowStep.getSignRequestParams() != null) {
                liveWorkflowStep.getSignRequestParams().clear();
            }
        }
        signBook.setStatus(SignRequestStatus.deleted);
        signBook.setUpdateDate(new Date());
        signBook.setUpdateBy(userEppn);
        logger.info("delete signbook : " + signBookId);
        return false;
    }

    @Transactional
    public boolean deleteDefinitive(Long signBookId, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(signBook.getCreateBy().equals(user) || userService.getSystemUser().equals(user) || user.getRoles().contains("ROLE_ADMIN")) {
            signBook.getLiveWorkflow().setCurrentStep(null);
            List<Long> liveWorkflowStepIds = signBook.getLiveWorkflow().getLiveWorkflowSteps().stream().map(LiveWorkflowStep::getId).collect(Collectors.toList());
            signBook.getLiveWorkflow().getLiveWorkflowSteps().clear();
            for (Long liveWorkflowStepId : liveWorkflowStepIds) {
                liveWorkflowStepService.delete(liveWorkflowStepId);
            }
            List<Long> signRequestsIds = signBook.getSignRequests().stream().map(SignRequest::getId).collect(Collectors.toList());
            for (Long signRequestId : signRequestsIds) {
                signRequestService.deleteDefinitive(signRequestId);
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

    public boolean checkUserManageRights(String userEppn, SignBook signBook) {
        if(signBook.getSignRequests().size() == 1) {
            User user = userService.getUserByEppn(userEppn);
            Data data = getBySignBook(signBook);
            if(data != null && data.getForm() != null && data.getForm().getWorkflow() != null && !data.getForm().getWorkflow().getManagers().isEmpty()) {
                if (data.getForm().getWorkflow().getManagers().contains(user.getEmail())) {
                    return true;
                }
            }
        }
        return signBook.getCreateBy().getEppn().equals(userEppn);
    }

    @Transactional
    public boolean removeStep(Long signBookId, int step) {
        SignBook signBook = getById(signBookId);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(currentStepNumber <= step + 1) {
            if(currentStepNumber == step + 1 && signBook.getLiveWorkflow().getLiveWorkflowSteps().size() > currentStepNumber) {
                signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(currentStepNumber));
                for(Recipient recipient : signBook.getLiveWorkflow().getLiveWorkflowSteps().get(currentStepNumber).getRecipients()) {
                    for (SignRequest signRequest : signBook.getSignRequests()) {
                        signRequest.getRecipientHasSigned().put(recipient, actionService.getEmptyAction());
                    }
                }
            }
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getLiveWorkflowSteps().get(step);
            signBook.getLiveWorkflow().getLiveWorkflowSteps().remove(liveWorkflowStep);
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                for (SignRequest signRequest : signBook.getSignRequests()) {
                    signRequest.getRecipientHasSigned().remove(recipient);
                }
            }
            liveWorkflowStepService.delete(liveWorkflowStep);
            return true;
        } else {
            return false;
        }
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment, String userEppn, String authUserEppn) {
        Log log = logService.create(signBook.getId(), signBook.getStatus().name(), action, returnCode, comment, userEppn, authUserEppn);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
        }
    }

    @Transactional
    public List<Log> getLogsFromSignBook(SignBook signBook) {
        List<Log> logs = new ArrayList<>();
        for (SignRequest signRequest : signBook.getSignRequests()) {
            logs.addAll(logService.getBySignRequestId(signRequest.getId()));
        }
        return logs;
    }

    public List<LiveWorkflowStep> getAllSteps(SignBook signBook) {
        List<LiveWorkflowStep> allSteps = new ArrayList<>(signBook.getLiveWorkflow().getLiveWorkflowSteps());
        if (allSteps.size() > 0) {
            allSteps.remove(0);
        }
        return allSteps;
    }

    @Transactional
    public void addLiveStep(Long id, List<String> recipientsEmails, int stepNumber, Boolean allSignToComplete, SignType signType, boolean repeatable, SignType repeatableSignType, boolean multiSign, Boolean autoSign, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = this.getById(id);
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, repeatable, repeatableSignType, multiSign, autoSign, allSignToComplete, signType, recipientsEmails, null);
        if (stepNumber == -1) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        } else {
            if (stepNumber >= currentStepNumber) {
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
            } else {
                if(signBook.getStatus().equals(SignRequestStatus.draft)) {
                //TODO add step
                    signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
                    signBook.getLiveWorkflow().setCurrentStep(liveWorkflowStep);
                } else {
                    throw new EsupSignatureException("L'étape ne peut pas être ajoutée car le circuit est déjà démarré");
                }
            }
        }
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), recipientsEmails);
        }
    }

    @Transactional
    public void sendCCEmail(Long signBookId, List<String> recipientsCCEmails) throws EsupSignatureMailException {
        SignBook signBook = getById(signBookId);
        if(recipientsCCEmails != null) {
            addViewers(signBookId, recipientsCCEmails);
        }
        mailService.sendCCtAlert(signBook.getViewers().stream().map(User::getEmail).collect(Collectors.toList()), signBook.getSignRequests().get(0));
    }

    @Transactional
    public void addViewers(Long signBookId, List<String> recipientsCCEmails) {
        SignBook signBook = getById(signBookId);
        if(recipientsCCEmails != null && recipientsCCEmails.size() > 0) {
                for (String recipientsEmail : recipientsCCEmails) {
                    User user = userService.getUserByEmail(recipientsEmail);
                    if (!signBook.getViewers().contains(user)) {
                        signBook.getViewers().add(user);
                        addUserInTeam(user.getId(), signBookId);
                    }
                }
        } else {
            signBook.getViewers().clear();
        }
    }

    public Data getBySignRequest(SignRequest signRequest) {
        return getBySignBook(signRequest.getParentSignBook());
    }

    public Data getBySignBook(SignBook signBook) {
        return dataRepository.findBySignBook(signBook);
    }


    public List<String> getAllDocTitles(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        Set<String> docTitles = new HashSet<>(signBookRepository.findSubjects(user));
        return docTitles.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public List<String> getWorkflowNames(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        List<String> workflowNames = signBookRepository.findWorkflowNames(user);
        return workflowNames.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    @Transactional
    public boolean toggle(Long id, String userEpppn) {
        SignBook signBook = getById(id);
        User user = userService.getUserByEppn(userEpppn);
        if(signBook.getHidedBy().contains(user)) {
            signBook.getHidedBy().remove(user);
            return false;
        } else {
            signBook.getHidedBy().add(user);
            return true;
        }
    }

    public int countEmpty(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        return Math.toIntExact(signBookRepository.countEmpty(user));
    }

    @Transactional
    public SignBook sendForSign(Long dataId, List<String> recipientsEmails, List<String> signTypes,  List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, List<String> targetUrls, String userEppn, String authUserEppn, boolean forceSendEmail, Map<String, String> formDatas, InputStream formReplaceInputStream, String signRequestParamsJsonString, String title) throws EsupSignatureException, EsupSignatureIOException, EsupSignatureFsException {
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        if (signRequestParamsJsonString != null) {
            signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
            signRequestParamsRepository.saveAll(signRequestParamses);
        }
        User user = userService.getUserByEppn(userEppn);
        User authUser = userService.getUserByEppn(authUserEppn);
        Data data = dataService.getById(dataId);
        if (recipientsEmails == null) {
            recipientsEmails = new ArrayList<>();
        }
        Form form = data.getForm();
        Workflow modelWorkflow = data.getForm().getWorkflow();
        Workflow computedWorkflow = workflowService.computeWorkflow(modelWorkflow.getId(), recipientsEmails, signTypes, allSignToCompletes, user.getEppn(), false);
        if(title == null || title.isEmpty()) {
            title = form.getTitle();
        }
        SignBook signBook = createSignBook(title, modelWorkflow, null, user.getEppn());
        SignRequest signRequest = signRequestService.createSignRequest(signBook.getSubject(), signBook, user.getEppn(), authUser.getEppn());
        if(form.getWorkflow().getOwnerSystem() != null && form.getWorkflow().getOwnerSystem()) {
            User systemUser = userService.getSystemUser();
            signBook.setCreateBy(systemUser);
            signRequest.setCreateBy(systemUser);
            addUserInTeam(systemUser.getId(), signBook.getId());
        }
        signRequest.getSignRequestParams().addAll(signRequestParamses);
        byte[] inputStream;
        try {
            inputStream = dataService.generateFile(data, formReplaceInputStream);
        } catch(IOException e) {
            throw new EsupSignatureException("Ce formulaire ne peut pas être instancié car il ne possède pas de modèle");
        }
        if(computedWorkflow.getWorkflowSteps().size() == 0) {
            try {
                inputStream = pdfService.convertGS(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String fileName = form.getTitle().replaceAll("[\\\\/:*?\"<>|]", "-").replace("\t", "");
        MultipartFile multipartFile = fileService.toMultipartFile(new ByteArrayInputStream(inputStream), fileName + ".pdf", "application/pdf");
        signRequestService.addDocsToSignRequest(signRequest, true, 0, form.getSignRequestParams(), multipartFile);
        workflowService.importWorkflow(signBook, computedWorkflow, externalUsersInfos);
        signRequestService.nextWorkFlowStep(signBook);
        Workflow workflow = workflowService.getById(form.getWorkflow().getId());
        targetService.copyTargets(workflow.getTargets(), signBook, targetEmails);
        if (targetUrls != null) {
            for (String targetUrl : targetUrls) {
                signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
            }
        }
        data.setSignBook(signBook);
        dataRepository.save(data);
        pendingSignBook(signBook.getId(), data, user.getEppn(), authUser.getEppn(), forceSendEmail);
        data.setStatus(SignRequestStatus.pending);
        for (String recipientEmail : recipientsEmails) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUser.getEppn()), Collections.singletonList(recipientEmail.split("\\*")[1]));
        }
        if(workflow.getCounter() != null) {
            workflow.setCounter(workflow.getCounter() + 1);
        } else {
            workflow.setCounter(0);
        }
        if(formDatas != null && formDatas.size() > 0) {
//            Map<String, String> datas = formDatas.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> String.join(",", e.getValue())));
            dataService.updateDatas(form, data, formDatas, user, authUser);
        }
        return signBook;
    }

    public List<SignBook> getSharedSignBooks(String userEppn) {
        List<SignBook> sharedSignBook = new ArrayList<>();
        for(UserShare userShare : userShareService.getByToUsersInAndShareTypesContains(Collections.singletonList(userEppn), ShareType.sign)) {
            if(userShare.getWorkflow() != null) {
                sharedSignBook.addAll(getByWorkflowId(userShare.getWorkflow().getId()));
            } else if(userShare.getForm() != null) {
                List<SignRequest> signRequests = signRequestService.getToSignRequests(userShare.getUser().getEppn());
                for (SignRequest signRequest : signRequests) {
                    Data data = getBySignBook(signRequest.getParentSignBook());
                    if(data.getForm().equals(userShare.getForm())) {
                        sharedSignBook.add(signRequest.getParentSignBook());
                        break;
                    }
                }
            }
        }
        return sharedSignBook;
    }

    public void sendEmailAlertSummary(User recipientUser) throws EsupSignatureMailException {
        Date date = new Date();
        List<SignRequest> toSignSignRequests = signRequestService.getToSignRequests(recipientUser.getEppn());
        toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser.getEppn()));
        if (toSignSignRequests.size() > 0) {
            recipientUser.setLastSendAlertDate(date);
            mailService.sendSignRequestSummaryAlert(Arrays.asList(recipientUser.getEmail()), toSignSignRequests);
        }
    }

    @Transactional
    public void addDocumentsToSignBook(Long signBookId, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        int i = 0;
        SignBook signBook = signBookRepository.findWithLockingById(signBookId).orElseThrow();
        if(signBook.getSubject().equals(signBook.getWorkflowName()) && multipartFiles.length == 1) {
            signBook.setSubject(fileService.getNameOnly(multipartFiles[0].getOriginalFilename()));
        }
        for (MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(fileService.getNameOnly(multipartFile.getOriginalFilename()), signBook, authUserEppn, authUserEppn);
            try {
                signRequestService.addDocsToSignRequest(signRequest, true, i, new ArrayList<>(), multipartFile);
                if (signBook.getStatus().equals(SignRequestStatus.pending)) {
                    signRequestService.pendingSignRequest(signRequest, authUserEppn);
                }
            } catch (EsupSignatureIOException e) {
                logger.warn("revert signbook creation due to error : " + e.getMessage());
                deleteDefinitive(signBookId, authUserEppn);
                throw new EsupSignatureIOException(e.getMessage(), e);
            }
            i++;
        }
    }

    public SignBook addDocsInNewSignBookSeparated(String title, String workflowName, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        User authUser = userService.getUserByEppn(authUserEppn);
        Workflow workflow = workflowRepository.findByName(workflowName);
        SignBook signBook;
        if (workflow == null) {
            if("custom".equals(workflowName)) {
                workflowName = "Demande personnalisée";
            }
            signBook = createSignBook(title, null, workflowName, authUserEppn);
        } else {
            if(workflow.getCreateBy().equals(authUser) && (title == null || title.isEmpty())) {
                title = fileService.getNameOnly(multipartFiles[0].getOriginalFilename());
            }
            signBook = createSignBook(title, workflow, null, authUserEppn);
        }
        addDocumentsToSignBook(signBook.getId(), multipartFiles, authUserEppn);
        return signBook;
    }

    @Transactional
    public SignBook addDocsInNewSignBookGrouped(String title, MultipartFile[] multipartFiles, String authUserEppn) throws EsupSignatureIOException {
        SignBook signBook = createSignBook(title, null, "", authUserEppn);
        SignRequest signRequest = signRequestService.createSignRequest(null, signBook, authUserEppn, authUserEppn);
        signRequestService.addDocsToSignRequest(signRequest, true, 0, new ArrayList<>(), multipartFiles);
        logger.info("signRequest : " + signRequest.getId() + " added to signBook" + signBook.getSubject() + " - " + signBook.getId());
        return signBook;
    }

    @Transactional
    public Map<SignBook, String> sendSignRequest(String title, MultipartFile[] multipartFiles, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsCCEmails, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, String userEppn, String authUserEppn, boolean forceSendEmail, Boolean forceAllSign, String targetUrl) throws EsupSignatureException, EsupSignatureIOException, EsupSignatureFsException {
        if(forceAllSign == null) forceAllSign = false;
        if(title == null || title.isEmpty()) {
            if(multipartFiles.length == 1) {
                title = fileService.getNameOnly(multipartFiles[0].getOriginalFilename());
            } else {
                title = "Parapheur pour " + recipientsEmails.get(0);
                if(recipientsEmails.size() > 1) {
                    title += " ...";
                }
            }
        }
        logger.info(title);
        SignBook signBook = addDocsInNewSignBookSeparated(title, "Demande simple", multipartFiles, userEppn);
        signBook.setForceAllDocsSign(forceAllSign);
        try {
            sendCCEmail(signBook.getId(), recipientsCCEmails);
        } catch (EsupSignatureMailException e) {
            throw new EsupSignatureException(e.getMessage());
        }
        if(targetUrl != null && !targetUrl.isEmpty()) {
            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
        }
        return sendSignBook(signBook, signType, allSignToComplete, userSignFirst, pending, comment, recipientsEmails, externalUsersInfos, userEppn, authUserEppn, forceSendEmail);
    }

    public Map<SignBook, String> sendSignBook(SignBook signBook, SignType signType, Boolean allSignToComplete, Boolean userSignFirst, Boolean pending, String comment, List<String> recipientsEmails, List<JsonExternalUserInfo> externalUsersInfos, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureException {
        User user = userService.getUserByEppn(userEppn);
        logger.info(userEppn + " envoi d'une demande de signature à " + recipientsEmails);
        String message = null;
        if (allSignToComplete == null) {
            allSignToComplete = false;
        }
        if(userSignFirst != null && userSignFirst) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(signBook, null,false, null, true, false,false, SignType.pdfImageStamp, Collections.singletonList(user.getEmail()), null));
        }
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(signBook, null,false, null, true, false, allSignToComplete, signType, recipientsEmails, externalUsersInfos));
        signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
        workflowService.dispatchSignRequestParams(signBook);
        if (pending != null && pending) {
            pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, forceSendEmail);
        } else {
            message = "Après vérification/annotation, vous devez cliquer sur 'Démarrer le circuit' pour transmettre la demande aux participants";
        }
        if (comment != null && !comment.isEmpty()) {
            signBook.setDescription(comment);
        }
        Map<SignBook, String> signBookStringMap = new HashMap<>();
        signBookStringMap.put(signBook, message);
        if(recipientsEmails != null) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), recipientsEmails);
        }
        return signBookStringMap;
    }

    @Transactional
    public void initWorkflowAndPendingSignBook(Long signRequestId, List<String> recipientsEmails, List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, String userEppn, String authUserEppn, Boolean draft) throws EsupSignatureFsException, EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getStatus().equals(SignRequestStatus.uploading)) {
            if(draft != null) {
                if(draft) {
                    initWorkflow(recipientsEmails, allSignToCompletes, externalUsersInfos, targetEmails, userEppn, authUserEppn, signBook);
                } else {
                    pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, false);
                }
            } else {
                initWorkflow(recipientsEmails, allSignToCompletes, externalUsersInfos, targetEmails, userEppn, authUserEppn, signBook);
                pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, false);
            }
        }
    }

    private void initWorkflow(List<String> recipientsEmails, List<String> allSignToCompletes, List<JsonExternalUserInfo> externalUsersInfos, List<String> targetEmails, String userEppn, String authUserEppn, SignBook signBook) throws EsupSignatureException, EsupSignatureFsException {
        if (signBook.getLiveWorkflow().getWorkflow() != null) {
            List<Target> targets = new ArrayList<>(workflowService.getById(signBook.getLiveWorkflow().getWorkflow().getId()).getTargets());
            Workflow workflow = workflowService.computeWorkflow(signBook.getLiveWorkflow().getWorkflow().getId(), recipientsEmails, new ArrayList<>(), allSignToCompletes, userEppn, false);
            workflowService.importWorkflow(signBook, workflow, externalUsersInfos);
            signRequestService.nextWorkFlowStep(signBook);
            targetService.copyTargets(targets, signBook, targetEmails);
            if(recipientsEmails != null) {
                for (String recipientEmail : recipientsEmails) {
                    userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(recipientEmail.split("\\*")[1]));
                }
            }
        }
    }

    @Transactional
    public void pendingSignBook(Long signBookId, Data data, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureException {
        SignBook signBook = signBookRepository.findWithLockingById(signBookId).orElseThrow();
        LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getCurrentStep();
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber(), "SUCCESS", signBook.getComment(), userEppn, authUserEppn);
        logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getLiveWorkflow().getCurrentStepNumber());
        boolean emailSended = false;
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
                signBook.getLiveWorkflow().getCurrentStep().setSignType(SignType.certSign);
                signBook.getLiveWorkflow().getCurrentStep().getRecipients().add(recipientService.createRecipient(userService.getSystemUser()));
            }
            if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
                if (liveWorkflowStep != null) {
                    signRequestService.pendingSignRequest(signRequest, userEppn);
                    if (!emailSended) {
                        try {
                            mailService.sendEmailAlerts(signRequest, userEppn, data, forceSendEmail);
                            emailSended = true;
                        } catch (EsupSignatureMailException e) {
                            throw new EsupSignatureException(e.getMessage());
                        }
                    }
                    for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                        if (recipient.getUser().getUserType().equals(UserType.external)) {
                            try {
                                otpService.generateOtpForSignRequest(signRequest.getId(), recipient.getUser().getId(), null);
                            } catch (EsupSignatureMailException e) {
                                throw new EsupSignatureException(e.getMessage());
                            }
                        }
                    }
                    if(signBook.getLiveWorkflow().getCurrentStep().getAutoSign()) {
                        for(SignRequest signRequest1 : signBook.getSignRequests()) {
                            List<SignRequestParams> signRequestParamses = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams();
                            if(liveWorkflowStep.getWorkflowStep().getCertificat() != null) {
                                if (signRequestParamses.size() > 0) {
                                    signRequestParamses.get(0).setExtraDate(true);
                                    signRequestParamses.get(0).setAddExtra(true);
                                    signRequestParamses.get(0).setExtraOnTop(true);
                                    signRequestParamses.get(0).setAddWatermark(true);
                                    signRequestParamses.get(0).setSignWidth(200);
                                    signRequestParamses.get(0).setSignHeight(100);
                                    signRequestParamses.get(0).setExtraText(signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getCertificat().getKeystore().getFileName().replace(",", "\n"));
                                    signRequest1.setSignable(true);
                                }
                                try {
                                    sign(signRequest1, "", "groupCert", signRequestParamses, null, "system", "system", null, "");
                                } catch (IOException | EsupSignatureMailException e) {
                                    refuse(signRequest1.getId(), "Signature refusée par le système automatique", "system", "system");
                                    logger.error("auto sign fail", e);
                                    throw new EsupSignatureException("Erreur lors de la signature automatique : " + e.getMessage());
                                }
                            } else {
                                try {
                                    sign(signRequest1, "", "sealCert", signRequestParamses, null, "system", "system", null, "");
                                } catch (IOException | EsupSignatureException e) {
                                    logger.error("auto sign fail", e);
                                    refuse(signRequest1.getId(), "Signature refusée par le système automatique", "system", "system");
                                    throw new EsupSignatureException("Erreur lors de la signature automatique : " + e.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    completeSignBook(signBook.getId(), userEppn, "Tous les documents sont signés");
                    logger.info("Circuit " + signBook.getId() + " terminé car ne contient pas d'étape");
                    break;
                }
            }
        }
    }

    public void completeSignBook(Long signBookId, String userEppn, String message) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        if (!signBook.getCreateBy().equals(userService.getSchedulerUser())) {
            try {
                mailService.sendCompletedMail(signBook, userEppn);
                mailService.sendCompletedCCMail(signBook);
            } catch (EsupSignatureMailException e) {
                throw new EsupSignatureException(e.getMessage());
            }
        }
        signRequestService.completeSignRequests(signBook.getSignRequests(), userEppn);
        Data data = dataService.getBySignBook(signBook);
        if(data != null) {
            data.setStatus(SignRequestStatus.completed);
        }
        if((globalProperties.getSealAllDocs() != null && globalProperties.getSealAllDocs()) || (signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().getSealAtEnd() != null && signBook.getLiveWorkflow().getWorkflow().getSealAtEnd())) {
            try {
                sealAllDocs(signBookId, userEppn);
            } catch (Exception e) {
                logger.warn("seal all skipped : " + e.getMessage());
            }
        }
        updateStatus(signBook, SignRequestStatus.completed, message, "SUCCESS", "", userEppn, userEppn);
        signBook.setEndDate(new Date());
    }

    @Transactional
    public void sealAllDocs(Long id, String userEppn) throws EsupSignatureException {
        SignBook signBook = getById(id);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signService.certSign(signRequest, userEppn, "", SignWith.sealCert);
        }
    }

    public List<SignRequest> getSignRequestsForCurrentUserByStatus(String userEppn, String authUserEppn) {
        List<SignRequest> signRequestList = new ArrayList<>();
        List<SignBook> signBooks = getSignBooks(userEppn, "toSign", null, null, null, null, null, Pageable.unpaged()).toList();
        if(!userEppn.equals(authUserEppn)) {
            for(SignBook signBook: signBooks) {
                for(SignRequest signRequest : signBook.getSignRequests()) {
                    if(userShareService.checkAllShareTypesForSignRequest(userEppn, authUserEppn, signRequest) || signRequestService.getSharedSignedSignRequests(authUserEppn).contains(signRequest)) {
                        signRequestList.add(signRequest);
                    }
                }
            }
        } else {
            for(SignBook signBook: signBooks) {
                signRequestList.addAll(signBook.getSignRequests());
            }
        }
        return signRequestList.stream().sorted(Comparator.comparing(SignRequest::getId)).collect(Collectors.toList());
    }

    public List<SignRequest> getSharedToSignSignRequests(String userEppn) {
        List<SignRequest> sharedSignRequests = new ArrayList<>();
        List<SignBook> sharedSignBooks = getSharedSignBooks(userEppn);
        for(SignBook signBook: sharedSignBooks) {
            sharedSignRequests.addAll(signBook.getSignRequests());
        }
        return sharedSignRequests;
    }

    @Transactional
    public boolean initSign(Long signRequestId, String signRequestParamsJsonString, String comment, String formData, String password, String signWith, Long userShareId, String userEppn, String authUserEppn) throws IOException, EsupSignatureException {
        SignRequest signRequest = getSignRequestsFullById(signRequestId, userEppn, authUserEppn);
        Map<String, String> formDataMap = null;
        List<String> toRemoveKeys = new ArrayList<>();
        if(formData != null) {
            try {
                TypeReference<Map<String, String>> type = new TypeReference<>(){};
                    formDataMap = objectMapper.readValue(formData, type);
                formDataMap.remove("_csrf");
                Data data = dataService.getBySignBook(signRequest.getParentSignBook());
                if(data != null && data.getForm() != null) {
                    List<Field> fields = preFillService.getPreFilledFieldsByServiceName(data.getForm().getPreFillType(), data.getForm().getFields(), userService.getUserByEppn(userEppn), signRequest);
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
        } else {
            signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
        }
        if (signRequest.getCurrentSignType().equals(SignType.nexuSign) || (signWith != null && SignWith.valueOf(signWith).equals(SignWith.nexuCert))) {
            signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
            return false;
        } else {
            sign(signRequest, password, signWith, signRequestParamses, formDataMap, userEppn, authUserEppn, userShareId, comment);
            return true;
        }
    }

    @Transactional
    public String initMassSign(String userEppn, String authUserEppn, String ids, HttpSession httpSession, String password, String signWith) throws IOException, EsupSignatureException {
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
        for (Long id : idsLong) {
            SignRequest signRequest = signRequestService.getById(id);
            if (!signRequest.getStatus().equals(SignRequestStatus.pending)) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.badStatus);
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.nexuSign)) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signTypeNotCompliant);
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEppn().equals(authUserEppn))) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.userNotInCurrentStep);
                error = messageSource.getMessage("report.reportstatus." + ReportStatus.userNotInCurrentStep, null, Locale.FRENCH);
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty()) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.noSignField);
                error = messageSource.getMessage("report.reportstatus." + ReportStatus.noSignField, null, Locale.FRENCH);
            }
            else if (signRequest.getStatus().equals(SignRequestStatus.pending) && initSign(id,null, null, null, password, signWith, userShareId, userEppn, authUserEppn)) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signed);
                error = null;
            }
            else {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.error);
            }
        }
        return error;
    }

    public void sign(SignRequest signRequest, String password, String signWith, List<SignRequestParams> signRequestParamses, Map<String, String> formDataMap, String userEppn, String authUserEppn, Long userShareId, String comment) throws EsupSignatureException, IOException {
        Date date = new Date();
        List<Log> lastSignLogs = new ArrayList<>();
        if(signRequest.getAuditTrail() == null) {
            signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
        }
        User signerUser = userService.getUserByEppn(userEppn);
        if(userShareId != null) {
            UserShare userShare = userShareService.getById(userShareId);
            if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
                signerUser = userService.getByEppn(authUserEppn);
            }
        }
        List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
        SignType signType = signRequest.getCurrentSignType();
        byte[] filledInputStream;
        if(!signRequestService.isNextWorkFlowStep(signRequest.getParentSignBook())) {
            Data data = dataService.getBySignRequest(signRequest);
            if(data != null && data.getForm() != null) {
                Form form = data.getForm();
                for (Field field : form.getFields()) {
                    if ("default".equals(field.getExtValueServiceName()) && "system".equals(field.getExtValueType())) {
                        if (field.getExtValueReturn().equals("id")) {
                            data.getDatas().put(field.getName(), "" + signRequest.getToken());
                            formDataMap.put(field.getName(), "" + signRequest.getToken());
                        }
                    }
                }
            }
        }
        byte[] bytes = toSignDocuments.get(0).getInputStream().readAllBytes();
        Reports reports = validationService.validate(new ByteArrayInputStream(bytes), null);
        if(formDataMap != null && formDataMap.size() > 0 && toSignDocuments.get(0).getContentType().equals("application/pdf")
                && (reports == null || reports.getSimpleReport().getSignatureIdList().size() == 0)) {
            filledInputStream = pdfService.fill(toSignDocuments.get(0).getInputStream(), formDataMap, signRequestService.isStepAllSignDone(signRequest.getParentSignBook()));
        } else {
            filledInputStream = toSignDocuments.get(0).getInputStream().readAllBytes();
        }
        boolean visual = true;
        if(signWith == null || SignWith.valueOf(signWith).equals(SignWith.imageStamp)) {
            byte[] signedInputStream = filledInputStream;
            String fileName = toSignDocuments.get(0).getFileName();
            if(signType.equals(SignType.hiddenVisa)) visual = false;
            if(signRequestParamses.size() == 0 && visual) {
                throw new EsupSignatureException("Il manque une signature !");
            }
            if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf") && visual) {
                for(SignRequestParams signRequestParams : signRequestParamses) {
                    signedInputStream = pdfService.stampImage(signedInputStream, signRequest, signRequestParams, 1, signerUser, date);
                    if(signRequestParams.getSignImageNumber() < 0) {
                        lastSignLogs.add(signRequestService.updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
                        auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", date, null, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
                    } else {
                        lastSignLogs.add(signRequestService.updateStatus(signRequest.getId(), signRequest.getStatus(), "Apposition de la signature", "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
                        auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", date, null, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
                    }
                }
            } else {
                auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Signature simple", "Pas de timestamp", date, null, null, null, null);
            }
            if ((signRequestService.isStepAllSignDone(signRequest.getParentSignBook()))) {
                signedInputStream = pdfService.convertGS(pdfService.writeMetadatas(signedInputStream, fileName, signRequest, lastSignLogs));
            }
            byte[] signedBytes = signedInputStream;

            boolean isComplete = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, signType, comment);
            Document signedDocument = documentService.addSignedFile(signRequest, new ByteArrayInputStream(signedBytes), signRequest.getTitle() + "." + fileService.getExtension(toSignDocuments.get(0).getFileName()), toSignDocuments.get(0).getContentType());
            if(isComplete) {
                try {
                    auditTrailService.closeAuditTrail(signRequest.getToken(), signedDocument, new ByteArrayInputStream(signedBytes));
                } catch(IOException e) {
                    throw new EsupSignatureException(e.getMessage());
                }
            }
        } else {
            reports = validationService.validate(signRequestService.getToValidateFile(signRequest.getId()), null);
            DiagnosticData diagnosticData = reports.getDiagnosticData();
            long nbSignatures = signRequestParamses.stream().filter(srp -> srp.getSignImageNumber() >= 0 && srp.getTextPart() == null).count();
            if(diagnosticData.getAllSignatures().size() == 0) {
                for (SignRequestParams signRequestParams : signRequestParamses) {
                    if (nbSignatures > 1 || signRequestParams.getSignImageNumber() < 0 || StringUtils.hasText(signRequestParams.getTextPart())) {
                        filledInputStream = pdfService.stampImage(filledInputStream, signRequest, signRequestParams, 1, signerUser, date);
                        lastSignLogs.add(signRequestService.updateStatus(signRequest.getId(), signRequest.getStatus(), "Ajout d'un élément", "SUCCESS", signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos(), signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn));
                        auditTrailService.addAuditStep(signRequest.getToken(), userEppn, "Ajout d'un élément", "Pas de timestamp", date, null, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
                    }
                }
            } else {
                logger.warn("skip add visuals because document already signed");
            }
            if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
                signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
                toSignDocuments.get(0).setTransientInputStream(new ByteArrayInputStream(pdfService.addOutLine(signRequest, filledInputStream, signerUser, new Date(), new SimpleDateFormat())));
            }
            Document signedDocument = signService.certSign(signRequest, signerUser.getEppn(), password, SignWith.valueOf(signWith));
            reports = validationService.validate(signedDocument.getInputStream(), null);
            diagnosticData = reports.getDiagnosticData();
            String certificat = new ArrayList<>(diagnosticData.getAllSignatures()).get(diagnosticData.getAllSignatures().size() - 1).getSigningCertificate().toString();
            String timestamp = diagnosticData.getTimestampList().get(0).getSigningCertificate().toString();
            if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().size() > 0) {
                SignRequestParams signRequestParams = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().get(0);
                auditTrailService.addAuditStep(signRequest.getToken(), userEppn, certificat, timestamp, reports.getSimpleReport().getValidationTime(), null, signRequestParams.getSignPageNumber(), signRequestParams.getxPos(), signRequestParams.getyPos());
            } else {
                auditTrailService.addAuditStep(signRequest.getToken(), userEppn, certificat, timestamp, reports.getSimpleReport().getValidationTime(), null, 0, 0, 0);
            }
            boolean isComplete = applyEndOfSignRules(signRequest.getId(), userEppn, authUserEppn, SignType.certSign, comment);
            if(isComplete) {
                try {
                    auditTrailService.closeAuditTrail(signRequest.getToken(), signedDocument, signedDocument.getInputStream());
                } catch(IOException e) {
                    throw new EsupSignatureException(e.getMessage());
                }
            }
        }
        customMetricsService.incValue("esup-signature.signrequests", "signed");
    }

    @Transactional
    public boolean applyEndOfSignRules(Long signRequestId, String userEppn, String authUserEppn, SignType signType, String comment) throws EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        if ( signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) ) {
            if(comment != null && !comment.isEmpty()) {
                commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, null, userEppn);
                signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa",  "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            } else {
                signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.checked, "Visa", "SUCCESS", userEppn, authUserEppn);
            }
        } else {
            if(comment != null && !comment.isEmpty()) {
                commentService.create(signRequest.getId(), comment, 0, 0, 0, null,true, null, userEppn);
                signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            } else {
                signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.signed, "Signature", "SUCCESS", userEppn, authUserEppn);
            }
        }
        recipientService.validateRecipient(signRequest, userEppn);
        if (signRequestService.isSignRequestCompleted(signRequest)) {
            signRequestService.completeSignRequests(Collections.singletonList(signRequest), authUserEppn);
            if (signRequestService.isCurrentStepCompleted(signRequest)) {
                for (Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
                    recipient.setSigned(!signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none));
                }
                if (signRequestService.nextWorkFlowStep(signRequest.getParentSignBook())) {
                    pendingSignBook(signRequest.getParentSignBook().getId(), null, userEppn, authUserEppn, false);
                } else {
                    completeSignBook(signRequest.getParentSignBook().getId(), authUserEppn, "Tous les documents sont signés");
                    return true;
                }
            }
        } else {
            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.pending, "Demande incomplète", "SUCCESS", userEppn, authUserEppn);
        }
        return false;
    }

    public void refuseSignBook(SignBook signBook, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
        mailService.sendRefusedMail(signBook, comment, userEppn);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
        }
        updateStatus(signBook, SignRequestStatus.refused, "Cette demande a été refusée, ceci annule toute la procédure", "SUCCESS", comment, userEppn, authUserEppn);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signBook.getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            for(Recipient recipient : signBook.getLiveWorkflow().getCurrentStep().getRecipients()) {
                if(recipient.getUser().getEppn().equals(userEppn)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    action.setActionType(ActionType.refused);
                    action.setUserIp(webUtilsService.getClientIp());
                    action.setDate(new Date());
                    recipient.setSigned(true);
                }
            }
        }
        Data data = dataService.getBySignBook(signBook);
        if(data != null) {
            data.setStatus(SignRequestStatus.refused);
        }
    }

    @Transactional
    public void refuse(Long signRequestId, String comment, String userEppn, String authUserEppn) throws EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        SignBook signBook = signRequest.getParentSignBook();
        if(signBook.getSignRequests().size() > 1 && (signBook.getForceAllDocsSign() == null || !signBook.getForceAllDocsSign())) {
            commentService.create(signRequest.getId(), comment, 0, 0, 0, null, true, "#FF7EB9", userEppn);
            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), userEppn, authUserEppn);
            for (Recipient recipient : signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients()) {
                if (recipient.getUser().getEppn().equals(userEppn)) {
                    Action action = signRequest.getRecipientHasSigned().get(recipient);
                    action.setActionType(ActionType.refused);
                    action.setUserIp(webUtilsService.getClientIp());
                    action.setDate(new Date());
                    recipient.setSigned(true);
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
                            completeSignBook(signBook.getId(), userEppn, "Tous les documents sont signés");
                        } else if (signBook.getSignRequests().stream().allMatch(signRequest1 -> signRequest1.getStatus().equals(SignRequestStatus.refused))) {
                            refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
                        } else {
                            completeSignBook(signBook.getId(), userEppn, "La demande est terminée mais au moins un des documents à été refusé");
                        }
                    }
                }
            }
        } else {
            refuseSignBook(signRequest.getParentSignBook(), comment, userEppn, authUserEppn);
        }
    }

    public void addStep(Long id, List<String> recipientsEmails, SignType signType, Boolean allSignToComplete, String authUserEppn) throws EsupSignatureException {
        SignRequest signRequest = signRequestService.getById(id);
        addLiveStep(signRequest.getParentSignBook().getId(), recipientsEmails, signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber(), allSignToComplete, signType, false, null, true, false, authUserEppn);
    }

    @Transactional
    public SignRequest startWorkflow(Long id, MultipartFile[] multipartFiles, String createByEppn, String title, List<String> recipientEmails, List<String> allSignToCompletes, List<String> targetEmails, List<String> targetUrls, String signRequestParamsJsonString) throws EsupSignatureFsException, EsupSignatureException, EsupSignatureIOException {
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        if (signRequestParamsJsonString != null) {
            signRequestParamses = signRequestParamsService.getSignRequestParamsFromJson(signRequestParamsJsonString);
            signRequestParamsRepository.saveAll(signRequestParamses);
        }
        Workflow workflow = workflowService.getById(id);
        User user = userService.getByEppn(createByEppn);
        SignBook signBook = createSignBook(title, workflow, "", user.getEppn());
        signBook.getLiveWorkflow().setWorkflow(workflow);
        SignRequest signRequest = signRequestService.createSignRequest(multipartFiles[0].getOriginalFilename(), signBook, createByEppn, createByEppn);
        signRequest.getSignRequestParams().addAll(signRequestParamses);
        signRequestService.addDocsToSignRequest(signRequest, false, 0, new ArrayList<>(), multipartFiles);
        initWorkflowAndPendingSignBook(signRequest.getId(), recipientEmails, allSignToCompletes, null, targetEmails, createByEppn, createByEppn, null);
        if(targetUrls != null) {
            for(String targetUrl : targetUrls) {
                if(signBook.getLiveWorkflow().getTargets().stream().noneMatch(target -> target != null && target.getTargetUri().equals(targetUrl))) {
                    signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
                }
            }
        }
        return signRequest;
    }

    @Transactional
    public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) throws EsupSignatureException {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        workflowService.importWorkflow(signBook, workflow, null);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook.getId(), null, authUserEppn, authUserEppn, false);
    }

    @Transactional
    public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook.getId(), data, userEppn, authUserEppn, true);
    }

    @Transactional
    public boolean startLiveWorkflow(SignBook signBook, String userEppn, String authUserEppn, Boolean start) throws EsupSignatureException {
        if(signBook.getLiveWorkflow().getLiveWorkflowSteps().size() >  0) {
            signBook.getLiveWorkflow().setCurrentStep(signBook.getLiveWorkflow().getLiveWorkflowSteps().get(0));
            if(start != null && start) {
                workflowService.dispatchSignRequestParams(signBook);
                pendingSignBook(signBook.getId(), null, userEppn, authUserEppn, false);
            }
            return true;
        }else {
            return false;
        }
    }

    @Transactional
    public int importFilesFromSource(Long workflowId, User user, User authUser) throws EsupSignatureException {
        Workflow workflow = workflowService.getById(workflowId);
        int nbImportedFiles = 0;
        if (workflow.getDocumentsSourceUri() != null && !workflow.getDocumentsSourceUri().equals("")) {
            logger.info("retrieve from " + workflow.getProtectedDocumentsSourceUri());
            FsAccessService fsAccessService = fsAccessFactoryService.getFsAccessService(workflow.getDocumentsSourceUri());
            if (fsAccessService != null) {
                fsAccessService.open();
                fsAccessService.createURITree(workflow.getDocumentsSourceUri());
                List<FsFile> fsFiles = new ArrayList<>(fsAccessService.listFiles(workflow.getDocumentsSourceUri() + "/"));
                if (fsFiles.size() > 0) {
                    int j = 0;
                    for (FsFile fsFile : fsFiles) {
                        logger.info("adding file : " + fsFile.getName());
                        ByteArrayOutputStream baos = fileService.copyInputStream(fsFile.getInputStream());
                        Map<String, String> metadatas = pdfService.readMetadatas(new ByteArrayInputStream(baos.toByteArray()));
                        String documentName = fsFile.getName();
                        if (metadatas.get("Title") != null && !metadatas.get("Title").isEmpty()) {
                            documentName = metadatas.get("Title");
                        }
                        SignBook signBook = createSignBook(fileService.getNameOnly(documentName), workflow, "", user.getEppn());
                        signBook.getLiveWorkflow().setWorkflow(workflow);
                        SignRequest signRequest = signRequestService.createSignRequest(null, signBook, user.getEppn(), authUser.getEppn());
                        if (fsFile.getCreateBy() != null && userService.getByEppn(fsFile.getCreateBy()) != null) {
                            user = userService.getByEppn(fsFile.getCreateBy());
                        }
                        signRequestService.addDocsToSignRequest(signRequest, true, j, new ArrayList<>(), fileService.toMultipartFile(new ByteArrayInputStream(baos.toByteArray()), fsFile.getName(), fsFile.getContentType()));
                        fsAccessService.remove(fsFile);
                        j++;
                        if (workflow.getScanPdfMetadatas()) {
                            String signType = metadatas.get("sign_type_default_val");
                            User creator = userService.createUserWithEppn(metadatas.get("Creator"));
                            if (creator != null) {
                                signRequest.setCreateBy(creator);
                                signBook.setCreateBy(creator);
                                addUserInTeam(creator.getId(), signBook.getId());
                            } else {
                                User systemUser = userService.getSystemUser();
                                signRequest.setCreateBy(systemUser);
                                signBook.setCreateBy(systemUser);
                                addUserInTeam(systemUser.getId(), signBook.getId());
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
                                        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, workflowStep, false, null, true, false, false, SignType.valueOf(signType), recipientList, null);
                                        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
                                    } catch (Exception e) {
                                        throw new EsupSignatureException(e.getMessage(), e);
                                    }
                                    i++;
                                }
                                if (keySplit[0].equals("sign") && keySplit[1].contains("target")) {
                                    String metadataTarget = metadatas.get(metadataKey);
                                    for(Target target : workflow.getTargets()) {
                                        signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri() + "/" + metadataTarget));
                                    }
                                    logger.info("target set to : " + signBook.getLiveWorkflow().getTargets().get(0).getTargetUri());
                                }
                            }
                        } else {
                            targetService.copyTargets(workflow.getTargets(), signBook, null);
                            workflowService.importWorkflow(signBook, workflow, null);
                        }
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

    @Transactional
    public SignBook getNextSignBook(Long signRequestId, String userEppn) {
        List<SignRequest> signRequestsToSign = signRequestService.getToSignRequests(userEppn);
        if(signRequestsToSign.size() > 1) {
            SignRequest currentSignRequest = signRequestService.getById(signRequestId);
            List<SignRequest> signRequests = signRequestsToSign.stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending)).sorted(Comparator.comparingLong(SignRequest::getId)).collect(Collectors.toList());
            int indexOfSignRequest = signRequests.indexOf(currentSignRequest);
            if (indexOfSignRequest + 1 >= signRequests.size()) {
                return signRequests.stream().filter(signRequest -> !signRequest.getId().equals(signRequestId)).min(Comparator.comparingLong(SignRequest::getId)).orElseThrow().getParentSignBook();
            } else {
                if (currentSignRequest.getParentSignBook().getSignRequests().size() == 1) {
                    return signRequests.get(indexOfSignRequest + 1).getParentSignBook();
                } else {
                    if (indexOfSignRequest + currentSignRequest.getParentSignBook().getSignRequests().size() + 1 >= signRequests.size()) {
                        return signRequests.get(0).getParentSignBook();
                    } else {
                        return signRequests.get(indexOfSignRequest + currentSignRequest.getParentSignBook().getSignRequests().size() + 1).getParentSignBook();
                    }
                }
            }
        } else {
            return null;
        }
    }

    public SignRequest getNextSignRequest(Long signRequestId, String userEppn) {
        SignRequest currentSignRequest = signRequestService.getById(signRequestId);
        Optional<SignRequest> inSameSignBookSignRequest = currentSignRequest.getParentSignBook().getSignRequests().stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.equals(currentSignRequest)).findAny();
        if(inSameSignBookSignRequest.isPresent()) {
            return inSameSignBookSignRequest.get();
        }
        List<SignRequest> signRequests = signRequestService.getToSignRequests(userEppn).stream().filter(signRequest -> signRequest.getStatus().equals(SignRequestStatus.pending) && !signRequest.getId().equals(signRequestId)).sorted(Comparator.comparingLong(SignRequest::getId)).collect(Collectors.toList());
        if(signRequests.size() > 0) {
            return signRequests.get(0);
        } else {
            return null;
        }
    }

    @Transactional
    public void getToSignFileReportResponse(Long signRequestId, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        webUtilsService.copyFileStreamToHttpResponse(signRequest.getTitle() + "-avec_rapport", "application/zip; charset=utf-8", new ByteArrayInputStream(getZipWithDocAndReport(signRequest, httpServletRequest, httpServletResponse)), httpServletResponse);
    }

    public byte[] getZipWithDocAndReport(SignRequest signRequest, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        String name = "";
        InputStream inputStream = null;
        if (!signRequest.getStatus().equals(SignRequestStatus.exported)) {
            if(signService.getToSignDocuments(signRequest.getId()).size() == 1) {
                List<Document> documents = signService.getToSignDocuments(signRequest.getId());
                name = documents.get(0).getFileName();
                inputStream = documents.get(0).getInputStream();
            }
        } else {
            FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
            name = fsFile.getName();
            inputStream = fsFile.getInputStream();
        }

        if(inputStream != null) {
            int i = 0;
            for(Document document : signRequest.getAttachments()) {
                zipOutputStream.putNextEntry(new ZipEntry(i + "_" + document.getFileName()));
                IOUtils.copy(document.getInputStream(), zipOutputStream);
                zipOutputStream.write(document.getInputStream().readAllBytes());
                zipOutputStream.closeEntry();
                i++;
            }

            byte[] fileBytes = inputStream.readAllBytes();
            zipOutputStream.putNextEntry(new ZipEntry(name));
            IOUtils.copy(new ByteArrayInputStream(fileBytes), zipOutputStream);
            zipOutputStream.closeEntry();

            ByteArrayOutputStream auditTrail = auditTrailService.generateAuditTrailPdf(signRequest, httpServletRequest, httpServletResponse);
            zipOutputStream.putNextEntry(new ZipEntry("dossier-de-preuve.pdf"));
            auditTrail.writeTo(zipOutputStream);
            zipOutputStream.closeEntry();

            Reports reports = validationService.validate(new ByteArrayInputStream(fileBytes), null);
            ByteArrayOutputStream reportByteArrayOutputStream = new ByteArrayOutputStream();
            fopService.generateSimpleReport(reports.getXmlSimpleReport(), reportByteArrayOutputStream);
            zipOutputStream.putNextEntry(new ZipEntry("rapport-signature.pdf"));
            IOUtils.copy(new ByteArrayInputStream(reportByteArrayOutputStream.toByteArray()), zipOutputStream);
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
        return outputStream.toByteArray();
    }

    @Transactional
    public void getMultipleSignedDocuments(List<Long> ids, HttpServletResponse response) throws IOException, EsupSignatureFsException {
        response.setContentType("application/zip; charset=utf-8");
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8.toString()) + ".zip");
        List<FsFile> fsFiles = new ArrayList<>();
        for(Long id : ids) {
            SignBook signBook = getById(id);
            for (SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived)) {
                    FsFile fsFile = signRequestService.getLastSignedFsFile(signRequest);
                    if(fsFile != null) {
                        fsFiles.add(fsFile);
                    }
                }
            }
        }
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

    @Transactional
    public void getMultipleSignedDocumentsWithReport(List<Long> ids, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
        httpServletResponse.setContentType("application/zip; charset=utf-8");
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8.toString()) + ".zip");
        Map<byte[], String> documents = new HashMap<>();
        for(Long id : ids) {
            SignBook signBook = getById(id);
            for (SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived))
                    documents.put(getZipWithDocAndReport(signRequest, httpServletRequest, httpServletResponse), signBook.getSubject());
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

    @Transactional
    public void saveWorkflow(Long signBookId, String title, String description, User user) throws EsupSignatureException {
        SignBook signBook = getById(signBookId);
        Workflow workflow = workflowService.createWorkflow(title, description, user);
        for(LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
            List<String> recipientsEmails = new ArrayList<>();
            for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                recipientsEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep toSaveWorkflowStep = workflowStepService.createWorkflowStep("" , liveWorkflowStep.getAllSignToComplete(), liveWorkflowStep.getSignType(), recipientsEmails.toArray(String[]::new));
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    public boolean needToSign(SignRequest signRequest, String userEppn) {
        boolean needSignInWorkflow = recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn);
        Recipient recipient = signRequest.getRecipientHasSigned().keySet().stream().filter(recipient1 -> recipient1.getUser().getEppn().equals(userEppn)).max(Comparator.comparing(Recipient::getId)).get();
        boolean needSign = signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none);
        return needSign || needSignInWorkflow;
    }

    @Transactional
    public SignRequest getSignRequestsFullById(long id, String userEppn, String authUserEppn) {
        SignRequest signRequest = signRequestService.getById(id);
        checkSignRequestSignable(signRequest, userEppn, authUserEppn);
        User user = userService.getUserByEppn(userEppn);
        if ((signRequest.getStatus().equals(SignRequestStatus.pending)
                && (isUserInRecipients(signRequest, userEppn)
                || signRequest.getCreateBy().getEppn().equals(userEppn)))
                || (signRequest.getStatus().equals(SignRequestStatus.draft)
                && signRequest.getCreateBy().getEppn().equals(user.getEppn()))
        ) {
            signRequest.setEditable(true);
        }
        return signRequest;
    }

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

    public boolean isUserInRecipientsAtCurrentStep(SignRequest signRequest, String userEppn) {
        boolean isInRecipients = false;
        List<Recipient> recipients = signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients();
        for(Recipient recipient : recipients) {
            if (recipient.getUser().getEppn().equals(userEppn)) {
                isInRecipients = true;
                break;
            }
        }
        return isInRecipients;
    }

    public boolean checkUserSignRights(SignRequest signRequest, String userEppn, String authUserEppn) {
        if(userEppn.equals(authUserEppn) || userShareService.checkShareForSignRequest(userEppn, authUserEppn, signRequest, ShareType.sign)) {
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

    @Transactional
    public List<String> getSignImagesForSignRequest(Long id, String userEppn, String authUserEppn, Long userShareId) throws EsupSignatureUserException, IOException {
        SignRequest signRequest = signRequestService.getById(id);
        LinkedList<String> signImages = new LinkedList<>();
        if (signRequest.getSignedDocuments().size() > 0 || signRequest.getOriginalDocuments().size() > 0) {
            List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
            if (toSignDocuments.size() == 1 && toSignDocuments.get(0).getContentType().equals("application/pdf")) {
                if(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep() != null && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.visa) && !signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().equals(SignType.hiddenVisa)) {
                    User user = userService.getByEppn(userEppn);
                    if(userShareId != null) {
                        UserShare userShare = userShareService.getById(userShareId);
                        if (userShare.getUser().getEppn().equals(userEppn) && userShare.getSignWithOwnSign() != null && userShare.getSignWithOwnSign()) {
                            user = userService.getByEppn(authUserEppn);
                        }
                    }
                    if (user.getSignImages().size() > 0 && user.getSignImages().get(0) != null && user.getSignImages().get(0).getSize() > 0) {
                        for (Document signImage : user.getSignImages()) {
                            signImages.add(fileService.getBase64Image(signImage));
                        }
                    }
                }
            }
        }
        signImages.add(userService.getDefaultImage(authUserEppn));
        return signImages;
    }

    private void checkSignRequestSignable(SignRequest signRequest, String userEppn, String authUserEppn) {
        if (signRequest.getStatus().equals(SignRequestStatus.pending)
                && checkUserSignRights(signRequest, userEppn, authUserEppn)
                && signRequest.getOriginalDocuments().size() > 0
                && needToSign(signRequest, userEppn)) {
            signRequest.setSignable(true);
            for(Document document : signRequest.getOriginalDocuments()) {
                if(document.getSize() == 0) {
                    signRequest.setSignable(false);
                    break;
                }
            }
        }
    }

    @Transactional
    public void sendSignRequestsToTarget(Long id, String authUserEppn) throws EsupSignatureException, EsupSignatureFsException {
        SignBook signBook = getById(id);
        if(signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0) {
            List<SignRequest> signRequests = signBook.getSignRequests();
            String title = signBook.getSubject();
            List<Target> targets = signBook.getLiveWorkflow().getTargets();
            boolean allTargetsDone = true;
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
                                    if (!signBook.getViewers().contains(user)) {
                                        signBook.getViewers().add(user);
                                        addUserInTeam(user.getId(), signBook.getId());
                                    }
                                }
                                mailService.sendFile(title, signBook, targetUrl);
                                target.setTargetOk(true);
                            } catch (MessagingException | IOException e) {
                                logger.error("unable to send mail to : " + target.getTargetUri(), e);
                                allTargetsDone = false;
                            }
                        } else {
                            for (SignRequest signRequest : signRequests) {
                                if (fsAccessFactoryService.getPathIOType(target.getTargetUri()).equals(DocumentIOType.rest)) {
                                    RestTemplate restTemplate = new RestTemplate();
                                    SignRequestStatus status = SignRequestStatus.completed;
                                    if (signRequest.getRecipientHasSigned().values().stream().anyMatch(action -> action.getActionType().equals(ActionType.refused))) {
                                        status = SignRequestStatus.refused;
                                    }
                                    try {
                                        ResponseEntity<String> response = restTemplate.getForEntity(target.getTargetUri() + "?signRequestId=" + signRequest.getId() + "&status=" + status.name(), String.class);
                                        if (response.getStatusCode().equals(HttpStatus.OK)) {
                                            target.setTargetOk(true);
                                            signRequestService.updateStatus(signRequest.getId(), signRequest.getStatus(), "Exporté vers " + targetUrl, "SUCCESS", authUserEppn, authUserEppn);
                                        } else {
                                            logger.error("rest export fail : " + target.getTargetUri() + " return is : " + response.getStatusCode());
                                            allTargetsDone = false;
                                        }
                                    } catch (Exception e) {
                                        logger.error("rest export fail : " + target.getTargetUri(), e);
                                        allTargetsDone = false;
                                    }
                                } else {
                                    try {
                                        Document signedFile = signRequest.getLastSignedDocument();
                                        if (signRequest.getAttachments().size() > 0 && globalProperties.getExportAttachements()) {
                                            if (!targetUrl.endsWith("/")) {
                                                targetUrl += "/";
                                            }
                                            targetUrl += signRequest.getTitle();
                                            for (Document attachment : signRequest.getAttachments()) {
                                                documentService.exportDocument(documentIOType, targetUrl, attachment, attachment.getFileName());
                                            }
                                        }
                                        String name = generateName(signRequest.getParentSignBook(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), true);
                                        documentService.exportDocument(documentIOType, targetUrl, signedFile, name);
                                        target.setTargetOk(true);
                                    } catch (EsupSignatureFsException e) {
                                        logger.error("fs export fail : " + target.getProtectedTargetUri(), e);
                                        allTargetsDone = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (allTargetsDone) {
                for (SignRequest signRequest : signRequests) {
                    signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.exported, "Exporté vers toutes les destinations", "SUCCESS", authUserEppn, authUserEppn);
                }
                signRequests.get(0).getParentSignBook().setStatus(SignRequestStatus.exported);
            } else {
                throw new EsupSignatureException("unable to send to all targets");
            }
        }
    }


    @Transactional
    public void archiveSignRequests(Long signBookId, String authUserEppn) throws EsupSignatureFsException, EsupSignatureException {
        if(globalProperties.getArchiveUri() != null) {
            logger.info("start archiving documents");
            boolean result = true;
            SignBook signBook = getById(signBookId);
            for(SignRequest signRequest : signBook.getSignRequests()) {
                Document signedFile = signRequest.getLastSignedDocument();
                if(signedFile != null) {
                    String subPath = "/" + signRequest.getParentSignBook().getWorkflowName() + "/";
                    if (signRequest.getExportedDocumentURI() == null) {
                        String name = generateName(signRequest.getParentSignBook(), signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), true);
                        String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath, signedFile.getId() + "_" + name);
                        if (documentUri != null) {
                            signRequest.setExportedDocumentURI(documentUri);
                            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.archived, "Exporté vers l'archivage", "SUCCESS", authUserEppn, authUserEppn);
                        } else {
                            logger.error("unable to archive " + subPath + name);
                            result = false;
                        }
                    }
                }
            }
            if(result) {
                signBook.setStatus(SignRequestStatus.archived);
            }
        } else {
            logger.debug("archive document was skipped");
        }
    }

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
            signBook.setStatus(SignRequestStatus.cleaned);
        }
    }

    @Transactional
    public boolean needToBeExported(Long signBookId) {
        SignBook signBook = getById(signBookId);
        return signBook.getStatus().equals(SignRequestStatus.completed) && signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getTargets().size() > 0;
    }

    public String generateName(SignBook signBook, Workflow workflow, User user, Boolean target) {
        String template = globalProperties.getNamingTemplate();
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
        if((signBook.getSubject() == null || signBook.getSubject().isEmpty())) {
            if(workflow == null) {
                if(signBook.getSignRequests().size() > 0) {
                    signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
                } else {
                    signBook.setSubject("");
                }
            } else {
                if(workflow.getDescription() != null && !workflow.getDescription().isEmpty()) {
                    signBook.setSubject(workflow.getDescription());
                } else if(workflow.getTitle() != null && !workflow.getTitle().isEmpty()) {
                    signBook.setSubject(workflow.getTitle());
                } else if(workflow.getName() != null && !workflow.getName().isEmpty()) {
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
            if(signBook.getSignRequests().size() > 0 && signBook.getSignRequests().get(0).getOriginalDocuments().size() > 0) {
                template = template.replace("[originalFileName]", signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName());
            } else {
                template = template.replace("[originalFileName]", signBook.getSubject());
            }
        }
        if(template.contains("[signedFileName]")) {
            if(signBook.getSignRequests().size() > 0 && signBook.getSignRequests().get(0).getSignedDocuments().size() > 0) {
                template = template.replace("[signedFileName]", signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName());
            } if(signBook.getSignRequests().size() > 0 && signBook.getSignRequests().get(0).getOriginalDocuments().size() > 0) {
                template = template.replace("[originalFileName]", signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName());
            } else {
                template = template.replace("[originalFileName]", signBook.getSubject());
            }
        }
        if(template.contains("[fileNameOnly]")) {
            template = template.replace("[fileNameOnly]", fileService.getNameOnly(signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName()));
        }
        if(template.contains("[fileExtension]")) {
            template = template.replace("[fileExtension]", fileService.getExtension(signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName()));
        }
        if(template.contains("[worflowName]")) {
            template = template.replace("[worflowName]", signBook.getWorkflowName());
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
        return template;
    }

    public List<User> getCreators(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter) {
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getUserByEppn(creatorFilter);
        }
        if(userEppn != null) {
            User user = userService.getUserByEppn(userEppn);
            return signBookRepository.findUserByRecipientAndCreateBy(user, workflowFilter, docTitleFilter, creatorFilterUser);
        } else {
            return signBookRepository.findSignBookAllUserByRecipientAndCreateBy(workflowFilter, docTitleFilter, creatorFilterUser);

        }
    }

    public Page<SignBook> getSignBookByWorkflow(Workflow workflow, String statusFilter, String recipientsFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        List<SignBook> signBooks = getByWorkflowId(workflow.getId());
        if(!statusFilter.equals("")) {
            signBooks = signBooks.stream().filter(signBook -> signBook.getStatus().equals(SignRequestStatus.valueOf(statusFilter))).collect(Collectors.toList());
        }
        if(!creatorFilter.equals("%")) {
            signBooks = signBooks.stream().filter(signBook -> signBook.getCreateBy().getEppn().equals(creatorFilter)).collect(Collectors.toList());
        }
        if(!recipientsFilter.equals("%")) {
            signBooks = signBooks.stream().filter(signBook -> signBook.getSignRequests().get(0).getRecipientHasSigned().keySet().stream().anyMatch(recipient -> recipient.getUser().getEppn().equals(recipientsFilter))).collect(Collectors.toList());
        }
        if(dateFilter != null && !dateFilter.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date formattedDate = null;
            try {
                formattedDate = formatter.parse(dateFilter);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            LocalDateTime nowLocalDateTime = formattedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            LocalDateTime startLocalDateTime = nowLocalDateTime.with(LocalTime.of(0, 0, 0));
            LocalDateTime endLocalDateTime = nowLocalDateTime.with(LocalTime.of(23, 59, 59));
            Date startDateFilter = Timestamp.valueOf(startLocalDateTime);
            Date endDateFilter = Timestamp.valueOf(endLocalDateTime);
            signBooks = signBooks.stream().filter(signBook -> signBook.getCreateDate().after(startDateFilter) && signBook.getCreateDate().before(endDateFilter)).collect(Collectors.toList());
        }
        if(pageable.getSort().iterator().hasNext()) {
            Sort.Order order = pageable.getSort().iterator().next();
            SortDefinition sortDefinition = new MutableSortDefinition(order.getProperty(), true, order.getDirection().isAscending());
            PropertyComparator<SignBook> propertyComparator = new PropertyComparator<>(sortDefinition);
            signBooks.sort(propertyComparator);
        }
        return new PageImpl<>(signBooks.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).collect(Collectors.toList()), pageable, signBooks.size());
    }

    @Transactional
    public void addUserInTeam(Long userId, Long signBookId) {
        User user = userService.getById(userId);
        SignBook signBook = getById(signBookId);
        if(!signBook.getTeam().contains(user)) {
            signBook.getTeam().add(user);
        }
    }

    public List<SignBook> getSignBookForUsers(String userEppn) {
        User user = userService.getUserByEppn(userEppn);
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(9999, Calendar.DECEMBER, 31);
//        Date startDateFilter = new Date(0);
//        Date endDateFilter = calendar.getTime();
//        Page<SignBook> signBooks = signBookRepository.findByRecipientAndCreateByEppnIndexed(user, null, null, null, startDateFilter, endDateFilter, Pageable.unpaged());
//        return signBooks.getContent();
        return signBookRepository.findByTeamContaining(user);
    }

    public int transfer(String authUserEppn) {
        int i = 0;
        User user = userService.getUserByEppn(authUserEppn);
        User replacedByUser = user.getCurrentReplaceUser();
        if(replacedByUser != null) {
            List<SignRequest> signRequests = getSignBookForUsers(authUserEppn).stream().filter(signBook -> signBook.getStatus().equals(SignRequestStatus.pending)).flatMap(signBook -> signBook.getSignRequests().stream().distinct()).collect(Collectors.toList());
            for(SignRequest signRequest : signRequests) {
                signRequest.getParentSignBook().getTeam().remove(user);
                signRequest.getParentSignBook().getTeam().add(replacedByUser);
                for(LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                    for(Recipient recipient : liveWorkflowStep.getRecipients()) {
                        if(recipient.getUser().getEppn().equals(authUserEppn)) {
                            recipient.setUser(replacedByUser);
                        }
                    }
                    for(Recipient recipient : signRequest.getRecipientHasSigned().keySet()) {
                        if(recipient.getUser().getEppn().equals(authUserEppn)) {
                            recipient.setUser(replacedByUser);
                        }
                    }
                }
                i++;
            }
        }
        return i;
    }

    @Transactional
    public void anonymize(String userEppn, User anonymous) {
        for(SignBook signBook : signBookRepository.findByCreateByEppn(userEppn)) {
            signBook.setCreateBy(anonymous);
        }
    }

    @Transactional
    public void cleanUploadingSignBooks() {
        for(SignBook signBook : signBookRepository.findByStatus(SignRequestStatus.uploading)){
            deleteDefinitive(signBook.getId(), "system");
        }
    }
}
