package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
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
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.StepStatus;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

@Service
@EnableConfigurationProperties(GlobalProperties.class)
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

    private final SignService signService;

    private final RecipientService recipientService;

    private final DocumentService documentService;

    private final SignRequestParamsService signRequestParamsService;

    private final PreFillService preFillService;

    private final ReportService reportService;

    private final ActionService actionService;

    private final SignRequestParamsRepository signRequestParamsRepository;

    private final ObjectMapper objectMapper;

    private final SignWithService signWithService;

    public SignBookService(GlobalProperties globalProperties, MessageSource messageSource, AuditTrailService auditTrailService, SignBookRepository signBookRepository, SignRequestService signRequestService, UserService userService, FsAccessFactoryService fsAccessFactoryService, WebUtilsService webUtilsService, FileService fileService, PdfService pdfService, WorkflowService workflowService, MailService mailService, WorkflowStepService workflowStepService, LiveWorkflowService liveWorkflowService, LiveWorkflowStepService liveWorkflowStepService, DataService dataService, LogService logService, TargetService targetService, UserPropertieService userPropertieService, CommentService commentService, OtpService otpService, DataRepository dataRepository, WorkflowRepository workflowRepository, UserShareService userShareService, SignService signService, RecipientService recipientService, DocumentService documentService, SignRequestParamsService signRequestParamsService, PreFillService preFillService, ReportService reportService, ActionService actionService, SignRequestParamsRepository signRequestParamsRepository, ObjectMapper objectMapper, SignWithService signWithService) {
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
        this.signService = signService;
        this.recipientService = recipientService;
        this.documentService = documentService;
        this.signRequestParamsService = signRequestParamsService;
        this.preFillService = preFillService;
        this.reportService = reportService;
        this.actionService = actionService;
        this.signRequestParamsRepository = signRequestParamsRepository;
        this.objectMapper = objectMapper;
        this.signWithService = signWithService;
    }

    @Transactional
    public int countSignBooksByWorkflow(Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).get();
        return signBookRepository.countByLiveWorkflowWorkflow(workflow);
    }

    @Transactional
    public Long nbToSignSignBooks(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return signBookRepository.countToSign(user);
    }

    @Transactional
    public Page<SignBook> getSignBooksForManagers(String userEppn, String authUserEppn, SignRequestStatus statusFilter, String recipientsFilter, Long workflowId, String docTitleFilter, String creatorFilter, String dateFilter, Pageable pageable) {
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getByEppn(creatorFilter);
        }
        User userFilter = null;
        if(recipientsFilter != null && !recipientsFilter.equals("%") && !recipientsFilter.isEmpty()) {
            userFilter = userService.getByEppn(recipientsFilter);
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
        return signBookRepository.findByWorkflowName(userFilter, statusFilter, workflowId, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
    }

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
            userFilter = userService.getByEppn(recipientsFilter);
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
            creatorFilterUser = userService.getByEppn(creatorFilter);
        }
        SignRequestStatus status = null;
        if(statusFilter != null && !statusFilter.isEmpty()) {
            status = SignRequestStatus.valueOf(statusFilter);
        }
        return signBookRepository.findSignBooksAllPaged(status, workflowFilter, docTitleFilter, creatorFilterUser, startDateFilter, endDateFilter, pageable);
    }

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

    @Transactional
    public void finishSignBookUpload(Long signBookId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(!StringUtils.hasText(signBook.getSubject())) {
            signBook.setSubject(generateName(signBookId, null, user, false));
        }
        signBook.setStatus(SignRequestStatus.draft);
    }

    public List<UserDto> getRecipientsNames(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return signBookRepository.findRecipientNames(user);
    }

    @Transactional
    public SignBook createSignBook(String subject, Workflow workflow, String workflowName, String userEppn, boolean geneateName, String comment) {
        User user = userService.getByEppn(userEppn);
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
        addToTeam(signBook, user.getEppn());
        signBook.setCreateDate(new Date());
        signBook.setLiveWorkflow(liveWorkflowService.create(workflowName, workflow));
        signBook.setDescription(comment);
        signBook.setSubject(subject);
        signBookRepository.save(signBook);
        if(geneateName) {
            subject = generateName(signBook.getId(), workflow, user, false);
        }
        signBook.setSubject(subject);
        return signBook;
    }

    @Transactional
    public void startFastSignBook(Long id, Boolean pending, List<WorkflowStepDto> steps, String userEppn, String authUserEppn, boolean forceSendEmail) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        if(StringUtils.hasText(steps.get(0).getTitle())) {
            signBook.setSubject(steps.get(0).getTitle());
        }
        signBook.setForceAllDocsSign(steps.get(0).getAllSignToComplete());
        sendSignBook(signBook, pending, steps.get(0).getComment(), steps, userEppn, authUserEppn, forceSendEmail);
        if(steps.get(0).getRecipientsCCEmails() != null) {
            addViewers(signBook.getId(), steps.get(0).getRecipientsCCEmails());
        }
    }

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

    @Transactional
    public SignBook updateSignBookWithStep(Long signBookId, List<WorkflowStepDto> steps) {
        SignBook signBook = updateSignBook(signBookId, steps.get(0).getTitle(), steps.get(0).getDescription(), steps.get(0).getRecipientsCCEmails());
        signBook.setForceAllDocsSign(steps.get(0).getForceAllSign());
        return signBook;
    }

    @Transactional
    public void initSignBook(Long signBookId, Long workflowId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(user.equals(signBook.getCreateBy())) {
            Workflow workflow = workflowRepository.findById(workflowId).get();
            signBook.getLiveWorkflow().setWorkflow(workflow);
        }
    }

    public void dispatchSignRequestParams(SignBook signBook) {
        for(SignRequest signRequest : signBook.getSignRequests()) {
            dispatchSignRequestParams(signRequest);
        }
    }

    public void dispatchSignRequestParams(SignRequest signRequest) {
        if(!signRequest.getSignRequestParams().isEmpty()) {
            int i = 0;
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                if (liveWorkflowStep.getWorkflowStep() != null) {
                    WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                    if (!liveWorkflowStep.getSignType().equals(SignType.hiddenVisa)) {
                        if(!workflowStep.getSignRequestParams().isEmpty()) {
                            for (SignRequestParams signRequestParams : signRequest.getSignRequestParams()) {
                                for(SignRequestParams signRequestParams1 : workflowStep.getSignRequestParams()) {
                                    if(signRequestParams1.getSignPageNumber().equals(signRequestParams.getSignPageNumber())
                                            && signRequestParams1.getxPos().equals(signRequestParams.getxPos())
                                            && signRequestParams1.getyPos().equals(signRequestParams.getyPos())) {
                                        liveWorkflowStep.getSignRequestParams().add(signRequestParams);
                                    }
                                }
                            }
                        } else {
                            if(signRequest.getSignRequestParams().size() > i) {
                                liveWorkflowStep.getSignRequestParams().add(signRequest.getSignRequestParams().get(i));
                            }
                        }
                    }
                } else if(signRequest.getSignRequestParams().size() > i) {
                    liveWorkflowStep.getSignRequestParams().add(signRequest.getSignRequestParams().get(i));
                }
                i++;
            }
        } else {
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                if(liveWorkflowStep.getWorkflowStep() != null) {
                    WorkflowStep workflowStep = workflowStepService.getById(liveWorkflowStep.getWorkflowStep().getId());
                    liveWorkflowStep.getSignRequestParams().addAll(workflowStep.getSignRequestParams());
                }
            }
        }
    }

    @Transactional
    public void importWorkflowFromWorkflowStepDto(Long signBookId, List<WorkflowStepDto> steps, String userEppn) {
        SignBook signBook = getById(signBookId);
        logger.info("import workflow steps in signBook " + signBook.getSubject() + " - " + signBook.getId());
        if(steps.get(0).getUserSignFirst() != null && steps.get(0).getUserSignFirst()) {
            addUserSignFirstStep(signBookId, userEppn);
        }
        workflowService.computeWorkflow(steps, signBook);
        dispatchSignRequestParams(signBook);
    }

    @Transactional
    public void addUserSignFirstStep(Long signBookId, String userEppn) {
        SignBook signBook = getById(signBookId);
        User user = userService.getByEppn(userEppn);
        WorkflowStepDto workflowStepDto = new WorkflowStepDto();
        recipientService.addRecipientInStep(workflowStepDto, user.getEmail());
        workflowStepDto.setSignType(SignType.pdfImageStamp);
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStepService.createLiveWorkflowStep(signBook, null, workflowStepDto));
    }

    @Transactional
    public void addNewStepToSignBook(Long signBookId, List<WorkflowStepDto> steps, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = signBookRepository.findById(signBookId).get();
        logger.info("add new workflow step to signBook " + signBook.getSubject() + " - " + signBook.getId());
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, steps.get(0));
        signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), steps);
    }

    @Transactional
    public SignBook getById(Long id) {
        return signBookRepository.findById(id).orElse(null);
    }

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

    public List<SignBook> getByWorkflowId(Long id) {
        return signBookRepository.findByWorkflowId(id);
    }

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

    @Transactional
    public boolean deleteDefinitive(Long signBookId, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignBook signBook = getById(signBookId);
        if(signBook != null && (signBook.getCreateBy().equals(user) || userService.getSystemUser().equals(user) || user.getRoles().contains("ROLE_ADMIN"))) {
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

    @Transactional
    public boolean checkUserManageRights(String userEppn, Long signBookId) {
        SignBook signBook = getById(signBookId);
        if(signBook == null) return false;
        if(signBook.getSignRequests().size() == 1) {
            User user = userService.getByEppn(userEppn);
            if(signBook.getLiveWorkflow().getWorkflow() != null && !signBook.getLiveWorkflow().getWorkflow().getManagers().isEmpty()) {
                if (signBook.getLiveWorkflow().getWorkflow().getManagers().contains(user.getEmail()) ||  signBook.getLiveWorkflow().getWorkflow().getDashboardRoles().stream().anyMatch(r -> user.getRoles().contains(r))) {
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
        Log log = logService.create(signBook.getId(), signBook.getSubject(), signBook.getWorkflowName(), signRequestStatus, action, comment, returnCode, null, null, null, null, userEppn, authUserEppn);
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
        if (!allSteps.isEmpty()) {
            allSteps.remove(0);
        }
        return allSteps;
    }

    @Transactional
    public void addLiveStep(Long id, WorkflowStepDto step, Integer stepNumber, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        if(stepNumber == null) stepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        int currentStepNumber = signBook.getLiveWorkflow().getCurrentStepNumber();
        if(step.getRepeatable()) {
            signBook.getLiveWorkflow().getCurrentStep().setRepeatable(false);
        }
        LiveWorkflowStep liveWorkflowStep = liveWorkflowStepService.createLiveWorkflowStep(signBook, null, step);
        if (stepNumber == -1) {
            signBook.getLiveWorkflow().getLiveWorkflowSteps().add(liveWorkflowStep);
        } else {
            if (stepNumber >= currentStepNumber) {
                signBook.getLiveWorkflow().getLiveWorkflowSteps().add(stepNumber, liveWorkflowStep);
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
        }
    }

    public List<SignBook> getSharedSignBooks(String userEppn) {
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

    public List<String> getAllDocTitles(String userEppn) {
        User user = userService.getByEppn(userEppn);
        Set<String> docTitles = new HashSet<>(signBookRepository.findSubjects(user));
        return docTitles.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public List<String> getWorkflowNames(String userEppn) {
        User user = userService.getByEppn(userEppn);
        List<String> workflowNames = signBookRepository.findAllWorkflowNames(user);
        return workflowNames.stream().filter(s -> s != null && !s.isEmpty()).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    @Transactional
    public boolean toggle(Long id, String userEpppn) {
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

    public int countEmpty(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return Math.toIntExact(signBookRepository.countEmpty(user));
    }

    @Transactional
    public SignBook sendForSign(Long dataId, List<WorkflowStepDto> steps, List<String> targetEmails, List<String> targetUrls, String userEppn, String authUserEppn, boolean forceSendEmail, Map<String, String> formDatas, InputStream formReplaceInputStream, String signRequestParamsJsonString, String title, Boolean sendEmailAlert, String comment) {
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        if (signRequestParamsJsonString != null) {
            signRequestParamses = signRequestParamsService.getSignRequestParamsesFromJson(signRequestParamsJsonString, userEppn);
            signRequestParamsRepository.saveAll(signRequestParamses);
        }
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
        signRequest.getSignRequestParams().addAll(signRequestParamses);
        byte[] toAddFile;
        try {
            toAddFile = dataService.generateFile(data, formReplaceInputStream);
        } catch(IOException e) {
            throw new EsupSignatureRuntimeException("Ce formulaire ne peut pas être instancié car il ne possède pas de modèle");
        }
        if(computedWorkflow.getWorkflowSteps().isEmpty()) {
            toAddFile = pdfService.convertGS(toAddFile);
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
                signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
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

    public void sendEmailAlertSummary(User recipientUser) throws EsupSignatureMailException {
        Date date = new Date();
        List<SignRequest> toSignSignRequests = signRequestService.getToSignRequests(recipientUser.getEppn());
        toSignSignRequests.addAll(getSharedToSignSignRequests(recipientUser.getEppn()));
        if (!toSignSignRequests.isEmpty()) {
            recipientUser.setLastSendAlertDate(date);
            mailService.sendSignRequestSummaryAlert(Collections.singletonList(recipientUser.getEmail()), toSignSignRequests);
        }
    }

    @Transactional
    public void addDocumentsToSignBook(Long signBookId, MultipartFile[] multipartFiles, String authUserEppn) {
        int i = 0;
        SignBook signBook = getById(signBookId);
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
            signBook.setSubject(generateName(signBookId, null, signBook.getCreateBy(), false));
        }
    }

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
            signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(targetUrl));
        }
        return sendSignBook(signBook, pending, steps.get(0).getComment(), steps, createByEppn, createByEppn, forceSendEmail);
    }

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

    @Transactional
    public void initSignBookWorkflow(Long signBookId, List<WorkflowStepDto> steps, List<String> targetEmails, String userEppn, String authUserEppn, Boolean pending, Boolean sendEmailAlert) throws EsupSignatureRuntimeException {
        List<RecipientWsDto> recipients = steps.stream().map(WorkflowStepDto::getRecipients).flatMap(List::stream).toList();
        if(signRequestService.checkTempUsers(signBookId, recipients)) {
            throw new EsupSignatureRuntimeException("Merci de compléter tous les utilisateurs externes");
        }
        SignBook signBook = getById(signBookId);
        if(signBook.getStatus().equals(SignRequestStatus.draft) || signBook.getStatus().equals(SignRequestStatus.uploading)) {
            List<Target> targets = new ArrayList<>(workflowService.getById(signBook.getLiveWorkflow().getWorkflow().getId()).getTargets());
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
                signBook.getLiveWorkflow().getCurrentStep().setSignType(SignType.certSign);
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
                                    signRequestService.sign(signRequest1, "", "autoCert", signRequestParamses, null, null,"system", "system", null, "");
                                                                    } catch (IOException | EsupSignatureMailException e) {
                                    refuse(signRequest1.getId(), "Signature refusée par le système automatique", "system", "system");
                                    logger.error("auto sign fail", e);
                                    throw new EsupSignatureRuntimeException("Erreur lors de la signature automatique : " + e.getMessage());
                                }
                            } else {
                                try {
                                    signRequestService.sign(signRequest1, "", "sealCert", signRequestParamses, null, null,"system", "system", null, "");
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
                            completeSignBook(signBook.getId(), userEppn, "Tous les documents sont signés");
                            logger.info("Circuit " + signBook.getId() + " terminé");
                            return;
                        }
                    } else {
                        if(signBook.getLiveWorkflow().getWorkflow() == null) {
                            dispatchSignRequestParams(signRequest);
                        }
                    }
                } else {
                    completeSignBook(signBook.getId(), userEppn, "Tous les documents sont signés");
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
                        otpService.generateOtpForSignRequest(signBook.getId(), recipient.getUser().getId(), null);
                    } catch (EsupSignatureMailException e) {
                        throw new EsupSignatureRuntimeException(e.getMessage());
                    }
                }
            }
        }
    }

    @Transactional
    public void completeSignBook(Long signBookId, String userEppn, String message) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(signBookId);
        if (!signBook.getCreateBy().equals(userService.getSchedulerUser())) {
            try {
                mailService.sendCompletedMail(signBook, userEppn);
                mailService.sendCompletedCCMail(signBook);
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

    @Transactional
    public void sealAllDocs(Long id) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(id);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.seal(signRequest.getId());
        }
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
    public StepStatus initSign(Long signRequestId, String signRequestParamsJsonString, String comment, String formData, String password, String signWith, Long userShareId, String userEppn, String authUserEppn) throws IOException, EsupSignatureRuntimeException {
        SignRequest signRequest = signRequestService.getById(signRequestId);
        if(signRequest.getAuditTrail() == null) {
            signRequest.setAuditTrail(auditTrailService.create(signRequest.getToken()));
        }
        SignType signType = signRequest.getCurrentSignType();
        if(signType.equals(SignType.visa) || signType.equals(SignType.hiddenVisa) && signWith == null) signWith = "imageStamp";
        String finalSignWith = signWith;
        if(signWith == null ||
                (globalProperties.getAuthorizedSignTypes().stream().noneMatch(s -> s.getValue() <= SignWith.valueOf(finalSignWith).getValue())
                && !signWithService.getAuthorizedSignWiths(userEppn, signRequest).contains(SignWith.valueOf(signWith)))) {
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
        } else {
            signRequestParamses = signRequestParamsService.getSignRequestParamsesFromJson(signRequestParamsJsonString, userEppn);
        }
        if (signRequest.getCurrentSignType().equals(SignType.nexuSign) || (SignWith.valueOf(signWith).equals(SignWith.nexuCert))) {
            signRequestParamsService.copySignRequestParams(signRequest, signRequestParamses);
            return StepStatus.nexu_redirect;
        } else {
            StepStatus stepStatus = signRequestService.sign(signRequest, password, signWith, signRequestParamses, data, formDataMap, userEppn, authUserEppn, userShareId, comment);
            if(stepStatus.equals(StepStatus.last_end)) {
                try {
                    if(globalProperties.getSealAllDocs() ||
                        (signRequest.getParentSignBook().getLiveWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getSealAtEnd() !=null && signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getSealAtEnd())
                    ) {
                        sealAllDocs(signRequest.getParentSignBook().getId());
                    }
                    completeSignBook(signRequest.getParentSignBook().getId(), authUserEppn, "Tous les documents sont signés");
                    Document signedDocument = signRequest.getLastSignedDocument();
                    auditTrailService.closeAuditTrail(signRequest.getToken(), signedDocument, signedDocument.getInputStream());
               } catch(IOException e) {
                    throw new EsupSignatureRuntimeException(e.getMessage());
                }
            } else if(stepStatus.equals(StepStatus.completed)) {
                if(signRequestService.isCurrentStepCompleted(signRequest)) {
                    pendingSignBook(signRequest.getParentSignBook(), null, userEppn, authUserEppn, false, true);
                }
            }
            return stepStatus;
        }
    }

    @Transactional
    public String initMassSign(String userEppn, String authUserEppn, String ids, HttpSession httpSession, String password, String signWith) throws IOException, EsupSignatureRuntimeException {
        if (SignWith.valueOf(signWith).equals(SignWith.nexuCert)) {
            return "initNexu";
        }
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
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients().stream().noneMatch(r -> r.getUser().getEppn().equals(authUserEppn))) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.userNotInCurrentStep);
                error = messageSource.getMessage("report.reportstatus." + ReportStatus.userNotInCurrentStep, null, Locale.FRENCH);
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignType().getValue() > SignWith.valueOf(signWith).getValue()) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signTypeNotCompliant);
                error = messageSource.getMessage("report.reportstatus." + ReportStatus.signTypeNotCompliant, null, Locale.FRENCH);
            } else if (signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getSignRequestParams().isEmpty() && SignWith.valueOf(signWith).equals(SignWith.imageStamp)) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.noSignField);
                error = messageSource.getMessage("report.reportstatus." + ReportStatus.noSignField, null, Locale.FRENCH);
            } else if (signRequest.getStatus().equals(SignRequestStatus.pending) && initSign(id,null, null, null, password, signWith, userShareId, userEppn, authUserEppn) != null) {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.signed);
                error = null;
            }
            else {
                reportService.addSignRequestToReport(report.getId(), signRequest, ReportStatus.error);
            }
        }
        return error;
    }

    public void refuseSignBook(SignBook signBook, String comment, String userEppn, String authUserEppn) throws EsupSignatureMailException {
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
            for (Target target : signRequest.getParentSignBook().getLiveWorkflow().getTargets().stream().filter(t -> t != null && fsAccessFactoryService.getPathIOType(t.getTargetUri()).equals(DocumentIOType.rest)).toList()) {
                targetService.sendRest(target.getTargetUri(), signRequest.getId().toString(), "refused", signRequest.getParentSignBook().getLiveWorkflow().getCurrentStepNumber().toString(), authUserEppn, comment);
            }
        }
    }

    @Transactional
    public List<Long> startWorkflow(Long id, MultipartFile[] multipartFiles, String createByEppn, String title, List<WorkflowStepDto> steps, List<String> targetEmails, List<String> targetUrls, List<SignRequestParams> signRequestParamses, Boolean scanSignatureFields, Boolean sendEmailAlert, String comment) throws EsupSignatureRuntimeException {
        logger.info("starting workflow " + id + " by " + createByEppn);
        Workflow workflow = workflowService.getById(id);
        User user = userService.createUserWithEppn(createByEppn);
        SignBook signBook = createSignBook(title, workflow, "", user.getEppn(), false, comment);
        signBook.getLiveWorkflow().setWorkflow(workflow);
        for(MultipartFile multipartFile : multipartFiles) {
            SignRequest signRequest = signRequestService.createSignRequest(multipartFile.getOriginalFilename(), signBook, createByEppn, createByEppn);
            signRequest.getSignRequestParams().addAll(signRequestParamses);
            signRequestService.addDocsToSignRequest(signRequest, scanSignatureFields, 0, new ArrayList<>(), multipartFile);
        }
        signBook.setSubject(generateName(signBook.getId(), workflow, user, false));
        if (targetUrls != null) {
            for (String targetUrl : targetUrls) {
                if (signBook.getLiveWorkflow().getTargets().stream().noneMatch(t -> t != null && t.getTargetUri().equals(targetUrl))) {
                    Target target = targetService.createTarget(targetUrl);
                    signBook.getLiveWorkflow().getTargets().add(target);
                }
            }
        }
        initSignBookWorkflow(signBook.getId(), steps, targetEmails, createByEppn, createByEppn, true, sendEmailAlert);
        return signBook.getSignRequests().stream().map(SignRequest::getId).toList();
    }

    @Transactional
    public void addWorkflowToSignBook(SignBook signBook, String authUserEppn, Long workflowSignBookId) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowService.getById(workflowSignBookId);
        workflowService.importWorkflow(signBook, workflow, new ArrayList<>());
        dispatchSignRequestParams(signBook);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook, null, authUserEppn, authUserEppn, false, true);
    }

    @Transactional
    public void nextStepAndPending(Long signBookId, Data data, String userEppn, String authUserEppn) throws EsupSignatureRuntimeException {
        SignBook signBook = getById(signBookId);
        signRequestService.nextWorkFlowStep(signBook);
        pendingSignBook(signBook, data, userEppn, authUserEppn, true, true);
    }

    @Transactional
    public boolean startLiveWorkflow(SignBook signBook, String userEppn, String authUserEppn, Boolean start) throws EsupSignatureRuntimeException {
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
                                        workflowStepDto.setSignType(SignType.valueOf(signType));
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
                                        signBook.getLiveWorkflow().getTargets().add(targetService.createTarget(target.getTargetUri() + "/" + metadataTarget));
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

    @Transactional
    public SignRequest getNextSignRequest(Long signRequestId, String userEppn, String authUserEppn, SignBook nextSignBook) {
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

    @Transactional
    public void getMultipleSignedDocuments(List<Long> ids, HttpServletResponse response) throws IOException, EsupSignatureFsException {
        response.setContentType("application/zip; charset=utf-8");
        response.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8) + ".zip");
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
        httpServletResponse.setHeader("Content-Disposition", "inline; filename=" + URLEncoder.encode("alldocs", StandardCharsets.UTF_8) + ".zip");
        Map<byte[], String> documents = new HashMap<>();
        for(Long id : ids) {
            SignBook signBook = getById(id);
            for (SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.completed) || signRequest.getStatus().equals(SignRequestStatus.exported) || signRequest.getStatus().equals(SignRequestStatus.archived))
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

    @Transactional
    public void saveSignBookAsWorkflow(Long signBookId, String title, String description, User user) throws EsupSignatureRuntimeException {
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

    public boolean needToSign(SignRequest signRequest, String userEppn) {
        boolean needSignInWorkflow = recipientService.needSign(signRequest.getParentSignBook().getLiveWorkflow().getCurrentStep().getRecipients(), userEppn);
        Recipient recipient = signRequest.getRecipientHasSigned().keySet().stream().filter(recipient1 -> recipient1.getUser().getEppn().equals(userEppn)).max(Comparator.comparing(Recipient::getId)).get();
        boolean needSign = signRequest.getRecipientHasSigned().get(recipient).getActionType().equals(ActionType.none);
        return needSign || needSignInWorkflow;
    }

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

    @Transactional
    public List<String> getSignImagesForSignRequest(Long id, String userEppn, String authUserEppn, Long userShareId) throws EsupSignatureUserException, IOException {
        SignRequest signRequest = signRequestService.getById(id);
        User user = userService.getByEppn(userEppn);
        LinkedList<String> signImages = new LinkedList<>();
        if (!signRequest.getSignedDocuments().isEmpty() || !signRequest.getOriginalDocuments().isEmpty()) {
            List<Document> toSignDocuments = signService.getToSignDocuments(signRequest.getId());
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
                    DocumentIOType documentIOType = fsAccessFactoryService.getPathIOType(target.getTargetUri());
                    String targetUrl = target.getTargetUri();
                    if (documentIOType != null && !documentIOType.equals(DocumentIOType.none)) {
                        if (!documentIOType.equals(DocumentIOType.mail)) {
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
                                    try {
                                        Document signedFile = signRequest.getLastSignedDocument();
                                        if (!signRequest.getAttachments().isEmpty() && globalProperties.getExportAttachements()) {
                                            if (!targetUrl.endsWith("/")) {
                                                targetUrl += "/";
                                            }
                                            targetUrl += signRequest.getTitle();
                                            for (Document attachment : signRequest.getAttachments()) {
                                                documentService.exportDocument(documentIOType, targetUrl, attachment, attachment.getFileName());
                                            }
                                        }
                                        String name = generateName(id, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), true);
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
                                        addToTeam(signBook, user.getEppn());
                                    }
                                }
                                mailService.sendFile(title, signBook, targetUrl);
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

    @Transactional
    public void archiveSignRequests(Long signBookId, String authUserEppn) throws EsupSignatureRuntimeException {
        if(globalProperties.getArchiveUri() != null) {
            logger.info("start archiving documents");
            boolean result = true;
            SignBook signBook = getById(signBookId);
            for(SignRequest signRequest : signBook.getSignRequests()) {
                Document signedFile = signRequest.getLastSignedDocument();
                if(signedFile != null) {
                    String subPath = "/" + signRequest.getParentSignBook().getWorkflowName().replaceAll("[^a-zA-Z0-9]", "_") + "/";
                    if (signRequest.getExportedDocumentURI() == null) {
                        String name = generateName(signBookId, signRequest.getParentSignBook().getLiveWorkflow().getWorkflow(), signRequest.getCreateBy(), true);
                        String documentUri = documentService.archiveDocument(signedFile, globalProperties.getArchiveUri(), subPath, signedFile.getId() + "_" + name);
                        if (documentUri != null) {
                            signRequest.setExportedDocumentURI(documentUri);
                            signRequestService.updateStatus(signRequest.getId(), SignRequestStatus.archived, "Exporté vers l'archivage", null, "SUCCESS", null, null, null, null, authUserEppn, authUserEppn);
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
        return signBook.getStatus().equals(SignRequestStatus.completed) && signBook.getLiveWorkflow() != null && !signBook.getLiveWorkflow().getTargets().isEmpty();
    }

    @Transactional
    public String generateName(Long signBookId, Workflow workflow, User user, Boolean target) {
        SignBook signBook = getById(signBookId);
        String template = globalProperties.getNamingTemplate();
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
            if(!signBook.getSignRequests().isEmpty()) {
                signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
                if(signBook.getSignRequests().size() > 1) {
                    signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()) + ", ...");
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
            if(!signBook.getSignRequests().isEmpty() && !signBook.getSignRequests().get(0).getOriginalDocuments().isEmpty()) {
                template = template.replace("[originalFileName]", signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName());
            } else {
                logger.warn("no original file name");
                template = template.replace("[originalFileName]", "");
            }
        }
        if(template.contains("[signedFileName]")) {
            if(!signBook.getSignRequests().isEmpty() && !signBook.getSignRequests().get(0).getSignedDocuments().isEmpty()) {
                template = template.replace("[signedFileName]", signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName());
            } else {
                logger.warn("no signed file name");
                template = template.replace("[signedFileName]", "");
            }
        }
        if(template.contains("[fileNameOnly]")) {
            if(!signBook.getSignRequests().isEmpty() && !signBook.getSignRequests().get(0).getSignedDocuments().isEmpty()) {
                template = template.replace("[fileNameOnly]", fileService.getNameOnly(signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName()));
            } if(!signBook.getSignRequests().isEmpty() && !signBook.getSignRequests().get(0).getOriginalDocuments().isEmpty()) {
                template = template.replace("[fileNameOnly]", fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
            } else {
                logger.warn("no file name");
                template = template.replace("[fileNameOnly]", "");
            }
        }
        if(template.contains("[fileExtension]")) {
            template = template.replace("[fileExtension]", fileService.getExtension(signBook.getSignRequests().get(0).getSignedDocuments().get(0).getFileName()));
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
        return template;
    }

    public List<UserDto> getCreators(String userEppn, String workflowFilter, String docTitleFilter, String creatorFilter) {
        User creatorFilterUser = null;
        if(creatorFilter != null) {
            creatorFilterUser = userService.getByEppn(creatorFilter);
        }
        User user = userService.getByEppn(userEppn);
        return signBookRepository.findUserByRecipientAndCreateBy(user, workflowFilter, docTitleFilter, creatorFilterUser);
    }

    public List<SignBook> getSignBookForUsers(String userEppn) {
        User user = userService.getByEppn(userEppn);
        return signBookRepository.findByTeamContaining(user);
    }

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
        for(SignBook signBook : signBookRepository.findByCreateByEppn(userEppn)) {
            signBook.setCreateBy(anonymous);
        }
    }

    @Transactional
    public void cleanUploadingSignBooks() {
        List<SignBook> toDelete = new ArrayList<>();
        toDelete.addAll(signBookRepository.findEmpties());
        toDelete.addAll(signBookRepository.findByStatus(SignRequestStatus.uploading));
        for(SignBook signBook : toDelete){
            deleteDefinitive(signBook.getId(), "system");
        }
    }

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

    @Transactional
    public boolean renewOtp(String urlId) {
        Otp otp = otpService.getOtpFromDatabase(urlId);
        if(otp != null) {
            SignBook signBook = otp.getSignBook();
            if (signBook != null) {
                SignRequest signRequest = signBook.getSignRequests().stream().filter(s -> !s.getStatus().equals(SignRequestStatus.cleaned) || !s.getDeleted()).findFirst().orElse(null);
                if (signRequest != null) {
                    List<Recipient> recipients = signRequest.getRecipientHasSigned().keySet().stream().filter(r -> r.getUser().getUserType().equals(UserType.external)).toList();
                    for (Recipient recipient : recipients) {
                        try {
                            otpService.generateOtpForSignRequest(signBook.getId(), recipient.getUser().getId(), recipient.getUser().getPhone());
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

    @Transactional
    public void completeSignRequest(Long id, String authUserEppn, String text) {
        SignRequest signRequest = signRequestService.getById(id);
        completeSignBook(signRequest.getParentSignBook().getId(), authUserEppn, text);
    }

    @Transactional
    public void pendingSignRequest(Long id, Data data, String userEppn, String authUserEppn, boolean forceSendEmail) {
        SignRequest signRequest = signRequestService.getById(id);
        pendingSignBook(signRequest.getParentSignBook(), data, userEppn, authUserEppn, forceSendEmail, true);
    }

    public void addToTeam(SignBook signBook, String userEppn) {
        User user = userService.getByEppn(userEppn);
        if(signBook.getTeam().stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            signBook.getTeam().add(user);
        }
    }

    public List<String> getSignBooksForManagersSubjects(Long workflowId) {
        return signBookRepository.findByWorkflowNameSubjects(workflowId);
    }

    public List<UserDto> getSignBooksForManagersCreators(Long workflowId) {
        return signBookRepository.findByWorkflowNameCreators(workflowId);
    }

    public List<UserDto> getSignBooksForManagersRecipientsUsers(Long workflowId) {
        return signBookRepository.findByWorkflowNameRecipientsUsers(workflowId);
    }
}
