package org.esupportail.esupsignature.service.view;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.dto.view.ui.AdminUiStatusDto;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.dto.json.UserSignatureStateDto;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.WorkflowStepDto;
import org.esupportail.esupsignature.dto.view.FrontendGlobalProperties;
import org.esupportail.esupsignature.dto.view.signrequest.CommentFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.FieldFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.ShowSignRequestBackDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignRequestUiCommonDto;
import org.esupportail.esupsignature.dto.view.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.dto.view.ui.UiConfigDto;
import org.esupportail.esupsignature.dto.view.ui.UiCountersDto;
import org.esupportail.esupsignature.dto.view.ui.UiDataDto;
import org.esupportail.esupsignature.dto.view.ui.UiHomeBootstrapDto;
import org.esupportail.esupsignature.dto.view.ui.UiMeDto;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.esupportail.esupsignature.entity.AuditTrail;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.FieldPropertieService;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.ReportService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.SignWithService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class UiFetchService {

    private static final Logger logger = LoggerFactory.getLogger(UiFetchService.class);

    private final GlobalProperties globalProperties;
    private final SmsProperties smsProperties;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final DataService dataService;
    private final WorkflowService workflowService;
    private final UserShareService userShareService;
    private final UserService userService;
    private final UserPropertieService userPropertieService;
    private final FieldPropertieService fieldPropertieService;
    private final RecipientService recipientService;
    private final SignWithService signWithService;
    private final ReportService reportService;
    private final AuditTrailService auditTrailService;
    private final LogService logService;
    private final PreAuthorizeService preAuthorizeService;
    private final SignService signService;
    private final Environment environment;
    private final BuildProperties buildProperties;
    private final ValidationService validationService;
    private final CertificatService certificatService;
    private final DSSService dssService;
    private final SessionRepositoryCustom sessionRepositoryCustom;
    private final UiFetchMapper uiFetchMapper;

    public UiFetchService(GlobalProperties globalProperties,
                          SmsProperties smsProperties,
                          SignRequestService signRequestService,
                          SignBookService signBookService,
                          DataService dataService,
                          WorkflowService workflowService,
                          UserShareService userShareService,
                          UserService userService,
                          UserPropertieService userPropertieService,
                          FieldPropertieService fieldPropertieService,
                          RecipientService recipientService,
                          SignWithService signWithService,
                          ReportService reportService,
                          AuditTrailService auditTrailService,
                          LogService logService,
                          PreAuthorizeService preAuthorizeService,
                          SignService signService,
                          Environment environment,
                          @Autowired(required = false) BuildProperties buildProperties,
                          ValidationService validationService,
                          CertificatService certificatService,
                          @Autowired(required = false) DSSService dssService,
                          SessionRepositoryCustom sessionRepositoryCustom,
                          UiFetchMapper uiFetchMapper) {
        this.globalProperties = globalProperties;
        this.smsProperties = smsProperties;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.dataService = dataService;
        this.workflowService = workflowService;
        this.userShareService = userShareService;
        this.userService = userService;
        this.userPropertieService = userPropertieService;
        this.fieldPropertieService = fieldPropertieService;
        this.recipientService = recipientService;
        this.signWithService = signWithService;
        this.reportService = reportService;
        this.auditTrailService = auditTrailService;
        this.logService = logService;
        this.preAuthorizeService = preAuthorizeService;
        this.signService = signService;
        this.environment = environment;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.certificatService = certificatService;
        this.dssService = dssService;
        this.sessionRepositoryCustom = sessionRepositoryCustom;
        this.uiFetchMapper = uiFetchMapper;
    }

    public UiDataDto buildUiData(String userEppn, String authUserEppn, HttpSession httpSession) {
        UiConfigDto config = buildUiConfig(userEppn, httpSession != null ? httpSession.getMaxInactiveInterval() : null);
        UiCountersDto counters = buildUiCounters(userEppn, authUserEppn);
        UiMeDto currentUser = buildUiMe(userEppn, authUserEppn, httpSession);
        Map<String, String> preferences = getUiPreferences(authUserEppn);
        AdminUiStatusDto adminStatus = authUserEppn != null && userService.getRoles(authUserEppn).contains("ROLE_ADMIN")
                ? buildAdminUiStatus()
                : null;
        return uiFetchMapper.toUiDataDto(config, counters, currentUser, preferences, adminStatus);
    }

    @Transactional(readOnly = true)
    public UiHomeBootstrapDto buildUiHomeBootstrap(String userEppn, String authUserEppn, Long startFormId, Long startWorkflowId) {
        return uiFetchMapper.toUiHomeBootstrapDto(
                startFormId,
                startWorkflowId,
                "/ws-secure/ui/warnings/read",
                "/user/search",
                "/user/search-titles",
                buildHomeSignBookItems(userEppn, authUserEppn, "toSign"),
                buildHomeSignBookItems(userEppn, authUserEppn, "pending")
        );
    }

    private List<UiHomeBootstrapDto.SignBookItem> buildHomeSignBookItems(String userEppn, String authUserEppn, String statusFilter) {
        if (userEppn == null || authUserEppn == null) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createDate"));
        return signBookService.getSignBooks(userEppn, authUserEppn, statusFilter, null, null, null, null, null, pageable)
                .getContent()
                .stream()
                .map(signBook -> toHomeSignBookItem(signBook, userEppn))
                .filter(Objects::nonNull)
                .toList();
    }

    private UiHomeBootstrapDto.SignBookItem toHomeSignBookItem(SignBook signBook, String userEppn) {
        if (signBook == null || signBook.getSignRequests() == null || signBook.getSignRequests().isEmpty()) {
            return null;
        }

        SignRequest primarySignRequest = signBook.getSignRequests().get(0);
        String listTitle = signBook.getSubject();
        if (signBook.getSignRequests().size() > 1
                && primarySignRequest.getOriginalDocuments() != null
                && !primarySignRequest.getOriginalDocuments().isEmpty()) {
            listTitle = primarySignRequest.getOriginalDocuments().get(0).getFileName() + ", ...";
        }

        return new UiHomeBootstrapDto.SignBookItem(
                signBook.getId(),
                primarySignRequest.getId(),
                signBook.getDescription(),
                signBook.getSubject(),
                signBook.getWorkflowName(),
                formatHomeDate(signBook.getCreateDate()),
                listTitle,
                isViewedByUser(primarySignRequest, userEppn),
                primarySignRequest.getAttachments() != null && !primarySignRequest.getAttachments().isEmpty(),
                toHomePostitItems(signBook.getPostits()),
                toHomeSignRequestItems(signBook.getSignRequests())
        );
    }

    private List<UiHomeBootstrapDto.PostitItem> toHomePostitItems(List<Comment> postits) {
        if (postits == null || postits.isEmpty()) {
            return List.of();
        }
        return postits.stream()
                .filter(Objects::nonNull)
                .map(postit -> new UiHomeBootstrapDto.PostitItem(
                        toDisplayName(postit.getCreateBy()),
                        postit.getText()
                ))
                .toList();
    }

    private List<UiHomeBootstrapDto.SignRequestItem> toHomeSignRequestItems(List<SignRequest> signRequests) {
        if (signRequests == null || signRequests.isEmpty()) {
            return List.of();
        }
        return signRequests.stream()
                .filter(Objects::nonNull)
                .map(signRequest -> new UiHomeBootstrapDto.SignRequestItem(
                        signRequest.getId(),
                        signRequest.getTitle(),
                        signRequest.getStatus() != null ? signRequest.getStatus().name() : null
                ))
                .toList();
    }

    private boolean isViewedByUser(SignRequest signRequest, String userEppn) {
        if (signRequest == null || userEppn == null || signRequest.getViewedBy() == null || signRequest.getViewedBy().isEmpty()) {
            return false;
        }
        return signRequest.getViewedBy().stream()
                .filter(Objects::nonNull)
                .anyMatch(user -> userEppn.equals(user.getEppn()));
    }

    private String formatHomeDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }

    public AdminUiStatusDto buildAdminUiStatus() {
        Boolean dssStatus = null;
        try {
            if (dssService != null) {
                dssStatus = dssService.refreshIsNeeded();
            }
        } catch (IOException e) {
            logger.debug("enable to get dss status");
        }
        return uiFetchMapper.toAdminUiStatusDto(sessionRepositoryCustom.findAllSessionIds().size(), dssStatus);
    }

    public UiMeDto buildUiMe(String userEppn, String authUserEppn, HttpSession httpSession) {
        if (userEppn == null) {
            return uiFetchMapper.toUiMeDto(null, Collections.emptySet(), null, Collections.emptySet(), Collections.emptyList(), Collections.emptyList(), null, Collections.emptyMap(), null);
        }
        User user = userService.getFullUserByEppn(userEppn);
        User authUser = authUserEppn != null ? userService.getByEppn(authUserEppn) : null;
        Set<String> userRoles = user != null ? userService.getRoles(userEppn) : Collections.emptySet();
        Set<String> authUserRoles = authUser != null ? userService.getRoles(authUserEppn) : Collections.emptySet();
        Map<org.esupportail.esupsignature.entity.enums.UiParams, String> uiParams = authUserEppn != null
                ? userService.getUiParams(authUserEppn)
                : Collections.emptyMap();
        return uiFetchMapper.toUiMeDto(
                user,
                userRoles,
                authUser,
                authUserRoles,
                authUserEppn != null ? userShareService.getSuUsers(authUserEppn) : Collections.emptyList(),
                user != null && user.getSignImagesIds() != null ? user.getSignImagesIds() : Collections.emptyList(),
                user != null ? user.getKeystoreFileName() : null,
                uiParams,
                httpSession != null ? httpSession.getAttribute("securityServiceName") : null
        );
    }

    public UiCountersDto buildUiCounters(String userEppn, String authUserEppn) {
        if (userEppn == null) {
            return uiFetchMapper.toUiCountersDto(0L, 0L, 0L, 0, 0, false, false, false, false);
        }
        Integer reportNumber = authUserEppn != null ? reportService.countByUser(authUserEppn) : 0;
        Integer managedWorkflowsSize = authUserEppn != null ? workflowService.getWorkflowByManagersContains(authUserEppn).size() : 0;
        Boolean isRoleManager = authUserEppn != null && preAuthorizeService.isManager(authUserEppn);
        Boolean isOneSignShare = authUserEppn != null && userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.sign);
        Boolean isOneReadShare = authUserEppn != null && userShareService.isOneShareByType(userEppn, authUserEppn, ShareType.read);
        Boolean certificatProblem = userService.getByEppn(userEppn) != null && certificatService.checkCertificatProblem(userService.getRoles(userEppn));
        return uiFetchMapper.toUiCountersDto(
                signRequestService.getNbPendingSignRequests(userEppn),
                signBookService.nbToSignSignBooks(userEppn),
                signBookService.nbDeleted(userEppn),
                reportNumber,
                managedWorkflowsSize,
                isRoleManager,
                isOneSignShare,
                isOneReadShare,
                certificatProblem
        );
    }

    public UiConfigDto buildUiConfig(String userEppn, Integer maxInactiveInterval) {
        FrontendGlobalProperties frontendGlobalProperties = userEppn != null ? buildFrontendGlobalProperties(userEppn) : null;
        String profile = null;
        if (environment.getActiveProfiles().length > 0 && "dev".equals(environment.getActiveProfiles()[0])) {
            profile = environment.getActiveProfiles()[0];
        }
        String versionApp = buildProperties != null ? buildProperties.getVersion() : "dev";
        return uiFetchMapper.toUiConfigDto(
                frontendGlobalProperties,
                smsProperties.getEnableSms(),
                validationService != null,
                globalProperties.getApplicationEmail(),
                maxInactiveInterval,
                globalProperties.getHoursBeforeRefreshNotif(),
                globalProperties.getInfiniteScrolling(),
                versionApp,
                profile
        );
    }

    public FrontendGlobalProperties buildFrontendGlobalProperties(String userEppn) {
        GlobalProperties myGlobalProperties = new GlobalProperties();
        BeanUtils.copyProperties(globalProperties, myGlobalProperties);
        userService.parseRoles(userEppn, myGlobalProperties);
        myGlobalProperties.newVersion = globalProperties.newVersion;
        return FrontendGlobalProperties.fromGlobalProperties(myGlobalProperties);
    }

    public Map<String, String> getUiPreferences(String authUserEppn) {
        if (authUserEppn == null) {
            return Collections.emptyMap();
        }
        return uiFetchMapper.toUiParamsMap(userService.getUiParams(authUserEppn));
    }

    public void setUiPreference(String authUserEppn, String key, String value) {
        userService.setUiParams(authUserEppn, UiParams.valueOf(key), value);
    }

    public boolean setUiDataValue(String authUserEppn, String object, String key, String value) {
        if ("preferences".equals(object)) {
            setUiPreference(authUserEppn, key, value);
            return true;
        }
        return false;
    }

    public List<User> checkTempUsers(List<String> recipientEmails) {
        return new ArrayList<>(userService.checkTempUsers(
                recipientService
                        .convertRecipientEmailsToStep(recipientEmails)
                        .stream()
                        .map(WorkflowStepDto::getRecipients)
                        .flatMap(List::stream)
                        .toList()
        ));
    }

    public Set<User> getFavoriteUsers(String authUserEppn) {
        return userPropertieService.getFavoritesEmails(authUserEppn);
    }

    public List<String> getFavoriteFieldValues(String authUserEppn, Long fieldId) {
        return fieldPropertieService.getFavoritesValues(authUserEppn, fieldId);
    }

    public Map<String, Object> getSignatureDocument(String userEppn, Long id) throws IOException {
        return userService.getSignatureByUserAndId(userEppn, id);
    }

    public InputStream getDefaultImage(String authUserEppn) throws IOException {
        return userService.getDefaultImage(authUserEppn);
    }

    public InputStream getDefaultParaphe(String authUserEppn) throws IOException {
        return userService.getDefaultParaphe(authUserEppn);
    }

    public Map<String, Object> getKeystore(String authUserEppn) throws IOException {
        return userService.getKeystoreByUser(authUserEppn);
    }

    public UserSignatureStateDto updateProfile(String userEppn,
                                               String authUserEppn,
                                               String signImageBase64,
                                               String name,
                                               String firstname,
                                               Long signRequestId,
                                               EmailAlertFrequency emailAlertFrequency,
                                               Integer emailAlertHour,
                                               DayOfWeek emailAlertDay,
                                               MultipartFile multipartKeystore,
                                               HttpSession httpSession) throws Exception {
        userService.updateUser(authUserEppn, name, firstname, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, null, false);
        return buildUserSignatureState(userEppn, authUserEppn, signRequestId, httpSession);
    }

    public UserSignatureStateDto deleteSignature(String userEppn,
                                                 String authUserEppn,
                                                 long signatureId,
                                                 Long signRequestId,
                                                 HttpSession httpSession) {
        userService.deleteSign(authUserEppn, signatureId);
        return buildUserSignatureState(userEppn, authUserEppn, signRequestId, httpSession);
    }

    public void markWarningsRead(String authUserEppn) {
        signRequestService.warningReaded(authUserEppn);
    }

    @Transactional(readOnly = true)
    public ShowSignRequestContext buildShowSignRequestContext(Long id,
                                                              String userEppn,
                                                              String authUserEppn,
                                                              HttpSession httpSession,
                                                              boolean isOtpView) throws IOException {
        SignRequest signRequest = signRequestService.getByIdWithShowContext(id);
        SignBook signBook = signRequest.getParentSignBook();
        LiveWorkflow liveWorkflow = signBook.getLiveWorkflow();
        LiveWorkflowStep currentStep = liveWorkflow.getCurrentStep();
        Workflow workflow = liveWorkflow.getWorkflow();

        initializeShowSignRequestGraph(signRequest, signBook, liveWorkflow, currentStep, workflow);

        boolean signable = signBookService.checkSignRequestSignable(id, userEppn, authUserEppn);
        boolean editable = signRequestService.isEditable(id, userEppn);
        boolean manager = signBookService.checkUserManageRights(signBook.getId(), userEppn);
        boolean attachmentAlert = signRequestService.isAttachmentAlert(signRequest);
        boolean attachmentRequire = signRequestService.isAttachmentRequire(signRequest);
        SignType currentSignType = signRequest.getCurrentSignType();
        Integer currentStepNumber = liveWorkflow.getCurrentStepNumber();
        boolean currentStepMultiSign = true;
        boolean currentStepSingleSignWithAnnotation = true;
        Long currentStepId = null;
        SignLevel currentStepMinSignLevel = SignLevel.simple;
        SignLevel currentStepMaxSignLevel = SignLevel.qualified;
        Boolean stepRepeatable = null;

        if (currentStep != null) {
            currentStepMinSignLevel = currentStep.getMinSignLevel();
            currentStepMaxSignLevel = currentStep.getMaxSignLevel();
            currentStepMultiSign = currentStep.getMultiSign();
            currentStepSingleSignWithAnnotation = currentStep.getSingleSignWithAnnotation();
            stepRepeatable = currentStep.getRepeatable();
            if (currentStep.getWorkflowStep() != null) {
                currentStepId = currentStep.getWorkflowStep().getId();
            }
        }

        int nbSignRequestInSignBookParent = signBook.getSignRequests().size();
        boolean lastStep = !liveWorkflow.getLiveWorkflowSteps().isEmpty() && currentStepNumber >= liveWorkflow.getLiveWorkflowSteps().size();
        List<Document> toSignDocuments = signRequestService.getToSignDocuments(signRequest.getId());
        Document toSignDocument = toSignDocuments.size() == 1 ? toSignDocuments.get(0) : null;
        boolean pdf = toSignDocument != null && "application/pdf".equals(toSignDocument.getContentType());
        if (toSignDocuments.stream().anyMatch(document -> !document.isPdf()) && currentStepMinSignLevel.getValue() < 3) {
            currentStepMinSignLevel = SignLevel.advanced;
        }

        Reports reports = signService.validate(id);
        List<String> signatureIds = new ArrayList<>();
        boolean signatureIssue = false;
        if (reports != null) {
            signatureIds = reports.getSimpleReport().getSignatureIdList();
            for (String signatureId : signatureIds) {
                if (!reports.getSimpleReport().isValid(signatureId)) {
                    signatureIssue = true;
                    break;
                }
            }
            if (!signatureIds.isEmpty() && currentStepMinSignLevel.getValue() < 3) {
                currentStepMinSignLevel = SignLevel.advanced;
            }
        }

        List<Field> fields = signRequestService.prefillSignRequestFields(id, userEppn);
        List<SignRequestParams> currentSignRequestParamses = signRequestService.getToUseSignRequestParams(id, userEppn);
        List<Comment> comments = signRequestService.getComments(id);
        List<SignRequestParams> spots = signRequestService.getSpots(id);
        List<String> signImages = new ArrayList<>();
        String signImagesWarningMessage = null;
        try {
            signImages = fetchSignImagesForRequest(id, userEppn, authUserEppn, httpSession);
        } catch (EsupSignatureUserException e) {
            signImagesWarningMessage = e.getMessage();
        }

        User frontUser = userService.getFullUserByEppn(userEppn);
        User frontAuthUser = userService.getByEppn(authUserEppn);
        String action = null;
        Set<String> supervisors = null;
        if (signRequest.getData() != null && signRequest.getData().getForm() != null && signRequest.getData().getForm().getWorkflow() != null) {
            action = signRequest.getData().getForm().getAction();
            supervisors = signRequest.getData().getForm().getWorkflow().getManagers();
        }

        boolean notSigned = !signRequestService.isSigned(signRequest, reports);

        return new ShowSignRequestContext(
                userEppn,
                authUserEppn,
                isOtpView,
                signRequest,
                signBook,
                liveWorkflow,
                currentStep,
                workflow,
                signable,
                editable,
                manager,
                attachmentAlert,
                attachmentRequire,
                currentSignType,
                currentStepNumber,
                currentStepId,
                currentStepMultiSign,
                currentStepSingleSignWithAnnotation,
                currentStepMinSignLevel,
                currentStepMaxSignLevel,
                stepRepeatable,
                nbSignRequestInSignBookParent,
                lastStep,
                toSignDocuments,
                toSignDocument,
                pdf,
                reports,
                signatureIds,
                signatureIssue,
                fields,
                currentSignRequestParamses,
                comments,
                spots,
                signImages,
                signImagesWarningMessage,
                frontUser,
                frontAuthUser,
                action,
                supervisors,
                notSigned
        );
    }

    private void initializeShowSignRequestGraph(SignRequest signRequest,
                                                SignBook signBook,
                                                LiveWorkflow liveWorkflow,
                                                LiveWorkflowStep currentStep,
                                                Workflow workflow) {
        initializeUser(signRequest.getCreateBy());
        signRequest.getComments().size();
        signRequest.getOriginalDocuments().size();
        signRequest.getSignedDocuments().size();
        signRequest.getAttachments().size();
        signRequest.getSignRequestParams().size();
        signRequest.getViewedBy().forEach(this::initializeUser);
        signBook.getSignRequests().forEach(signRequestItem -> signRequestItem.getId());
        signBook.getViewers().forEach(this::initializeUser);

        if (workflow != null) {
            workflow.getWorkflowSteps().forEach(workflowStep -> {
                workflowStep.getId();
                workflowStep.getUsers().forEach(this::initializeUser);
            });
        }

        if (currentStep != null) {
            currentStep.getRecipients().forEach(recipient -> {
                if (recipient.getUser() != null) {
                    initializeUser(recipient.getUser());
                }
            });
            if (currentStep.getWorkflowStep() != null) {
                currentStep.getWorkflowStep().getId();
            }
        }

        liveWorkflow.getLiveWorkflowSteps().forEach(step -> {
            step.getRecipients().forEach(recipient -> {
                if (recipient.getUser() != null) {
                    initializeUser(recipient.getUser());
                }
            });
            if (step.getWorkflowStep() != null) {
                step.getWorkflowStep().getId();
            }
        });

        liveWorkflow.getTargets().forEach(target -> {
            target.getId();
            target.getTargetUri();
            target.getProtectedTargetUri();
            target.getTargetOk();
        });

        if (signRequest.getData() != null && signRequest.getData().getForm() != null) {
            signRequest.getData().getForm().getId();
            if (signRequest.getData().getForm().getWorkflow() != null) {
                signRequest.getData().getForm().getWorkflow().getWorkflowSteps().forEach(workflowStep -> {
                    workflowStep.getId();
                    workflowStep.getUsers().forEach(this::initializeUser);
                });
            }
        }
    }

    private void initializeUser(User user) {
        if (user == null) {
            return;
        }
        user.getEppn();
        user.getName();
        user.getFirstname();
        user.getEmail();
        user.getPhone();
        user.getUserType();
    }

    public ShowSignRequestBackDto buildShowSignRequestBackDto(ShowSignRequestContext context,
                                                              Boolean frameMode,
                                                              String annotation) {
        SignRequestUiCommonDto common = buildCommonDto(context);
        SignRequest signRequest = context.signRequest();
        SignBook signBook = context.signBook();
        String userEppn = context.userEppn();
        String authUserEppn = context.authUserEppn();

        boolean displayNotif = !context.isOtpView() && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean tempUsers = !context.isOtpView() && signBookService.isTempUsers(signBook.getId());
        List<Comment> postits = signRequestService.getPostits(signRequest.getId());
        List<Document> attachments = signRequestService.getAttachments(signRequest.getId());
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        SignRequest nextSignRequest = signBookService.getNextSignRequest(signRequest.getId(), nextSignBook.getId());
        List<SignWith> signWiths = new ArrayList<>();
        if (context.reports() != null) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, !context.signatureIds().isEmpty());
        } else if (context.signable()) {
            signWiths = signWithService.getAuthorizedSignWiths(userEppn, signRequest, false);
        }

        AuditTrail auditTrail = null;
        String size = null;
        if (!signRequest.getStatus().equals(SignRequestStatus.draft)
                && !signRequest.getStatus().equals(SignRequestStatus.pending)
                && !signRequest.getStatus().equals(SignRequestStatus.refused)
                && !signRequest.getDeleted()) {
            auditTrail = auditTrailService.getAuditTrailByToken(signRequest.getToken());
            if (auditTrail != null && auditTrail.getDocumentSize() != null) {
                size = FileUtils.byteCountToDisplaySize(auditTrail.getDocumentSize());
            }
        }

        boolean sealCertOK = signWithService.checkSealCertificat(userEppn, true);
        List<SealCertificatProperties> sealCertificatPropertieses = certificatService.getCheckedSealCertificates();
        SignWith[] allSignWiths = SignWith.values();
        List<Certificat> certificats = certificatService.getCertificatByUser(userEppn);
        List<LiveWorkflowStep> steps = signRequest.getStatus().equals(SignRequestStatus.draft)
                ? context.liveWorkflow().getLiveWorkflowSteps()
                : new ArrayList<>();
        List<Log> refuseLogs = logService.getRefuseLogs(signRequest.getId());
        boolean viewRight = preAuthorizeService.checkUserViewRights(signRequest, userEppn, authUserEppn);
        Data data = dataService.getBySignBook(signBook);
        Form form = data != null ? data.getForm() : null;
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        String pdfaCheck = !context.toSignDocuments().isEmpty() ? context.toSignDocuments().get(0).getPdfaCheck() : null;
        boolean auditTrailChecked = signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported);
        List<RecipientWsDto> externalsRecipients = auditTrailChecked
                ? signRequestService.getExternalRecipients(signRequest.getId())
                : new ArrayList<>();

        return new ShowSignRequestBackDto(
                signRequest,
                signBook,
                context.workflow(),
                common.signRequestId(),
                signBook.getId(),
                common.dataId(),
                common.formId(),
                context.isOtpView() ? "otp" : "user",
                displayNotif,
                tempUsers,
                common.signable(),
                common.editable(),
                common.manager(),
                common.status(),
                common.currentSignType(),
                common.currentStepNumber(),
                context.currentStepId(),
                common.currentStepMultiSign(),
                common.currentStepSingleSignWithAnnotation(),
                common.currentStepMinSignLevel(),
                context.currentStepMaxSignLevel(),
                common.stepRepeatable(),
                context.lastStep(),
                common.pdf(),
                common.attachmentAlert(),
                common.attachmentRequire(),
                common.notSigned(),
                signRequestService.isCurrentUserAsSigned(signRequest, userEppn),
                context.signatureIds(),
                context.signatureIssue(),
                common.nbSignRequests(),
                common.action(),
                context.supervisors(),
                context.toSignDocument(),
                postits,
                common.comments(),
                common.spots(),
                attachments,
                nextSignBook,
                nextSignRequest,
                common.fields(),
                common.signRequestParams(),
                common.signImages(),
                signWiths,
                auditTrail,
                size,
                sealCertOK,
                sealCertificatPropertieses,
                allSignWiths,
                certificats,
                annotation,
                steps,
                refuseLogs,
                viewRight,
                frameMode,
                form,
                logs,
                pdfaCheck,
                auditTrailChecked,
                externalsRecipients
        );
    }

    public SignUiFrontDto buildSignUiFrontDto(ShowSignRequestContext context) {
        SignRequestUiCommonDto common = buildCommonDto(context);
        User frontUser = context.frontUser();
        User frontAuthUser = context.frontAuthUser();

        return new SignUiFrontDto(
                common.signRequestId(),
                common.dataId(),
                common.formId(),
                toSignRequestParamsFrontDtos(common.signRequestParams()),
                frontUser != null ? frontUser.getDefaultSignImageNumber() : null,
                common.currentSignType(),
                common.signable(),
                common.editable(),
                toCommentFrontDtos(common.comments()),
                toSignRequestParamsFrontDtos(common.spots()),
                common.pdf(),
                common.currentStepNumber(),
                common.currentStepMultiSign(),
                common.currentStepSingleSignWithAnnotation(),
                common.currentStepMinSignLevel(),
                context.workflow() != null,
                common.signImages(),
                toDisplayName(frontUser),
                toDisplayName(frontAuthUser),
                toFieldFrontDtos(common.fields(), context.workflow()),
                common.stepRepeatable(),
                common.status(),
                common.action(),
                common.nbSignRequests(),
                common.notSigned(),
                common.attachmentAlert(),
                common.attachmentRequire(),
                context.isOtpView(),
                frontUser == null || frontUser.getFavoriteSignRequestParams() == null,
                frontUser != null ? frontUser.getPhone() : null,
                frontUser != null ? frontUser.getReturnToHomeAfterSign() : null,
                common.manager()
        );
    }

    private UserSignatureStateDto buildUserSignatureState(String userEppn, String authUserEppn, Long signRequestId, HttpSession httpSession) {
        User user = userService.getFullUserByEppn(authUserEppn);
        return new UserSignatureStateDto(user.getFirstname(), user.getName(), user.getEmail(), user.getSignImagesIds(), getSignImages(signRequestId, userEppn, authUserEppn, httpSession));
    }

    private List<String> getSignImages(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession) {
        if (signRequestId == null) {
            return Collections.emptyList();
        }
        try {
            return fetchSignImagesForRequest(signRequestId, userEppn, authUserEppn, httpSession);
        } catch (EsupSignatureUserException | IOException e) {
            return Collections.emptyList();
        }
    }

    private List<String> fetchSignImagesForRequest(Long signRequestId, String userEppn, String authUserEppn, HttpSession httpSession) throws IOException, EsupSignatureUserException {
        Long userShareId = getUserShareId(httpSession);
        return signBookService.getSignImagesForSignRequest(signRequestId, userEppn, authUserEppn, userShareId);
    }

    private Long getUserShareId(HttpSession httpSession) {
        Object userShareString = httpSession.getAttribute("userShareId");
        if (userShareString == null) {
            return null;
        }
        return Long.valueOf(userShareString.toString());
    }

    private List<CommentFrontDto> toCommentFrontDtos(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        return comments.stream()
                .map(comment -> new CommentFrontDto(
                        comment.getId(),
                        comment.getPageNumber(),
                        comment.getStepNumber(),
                        comment.getPosX(),
                        comment.getPosY()
                ))
                .toList();
    }

    private List<SignRequestParamsFrontDto> toSignRequestParamsFrontDtos(List<SignRequestParams> signRequestParamses) {
        if (signRequestParamses == null || signRequestParamses.isEmpty()) {
            return List.of();
        }
        return signRequestParamses.stream()
                .map(signRequestParams -> new SignRequestParamsFrontDto(
                        signRequestParams.getId(),
                        signRequestParams.getPdSignatureFieldName(),
                        signRequestParams.getStepNumber(),
                        signRequestParams.getSignImageNumber(),
                        signRequestParams.getSignPageNumber(),
                        signRequestParams.getSignDocumentNumber(),
                        signRequestParams.getSignWidth(),
                        signRequestParams.getSignHeight(),
                        signRequestParams.getxPos(),
                        signRequestParams.getyPos(),
                        signRequestParams.getExtraText(),
                        signRequestParams.getIsExtraText(),
                        signRequestParams.getAddWatermark(),
                        signRequestParams.getAllPages(),
                        signRequestParams.getAddImage(),
                        signRequestParams.getAddExtra(),
                        signRequestParams.getExtraType(),
                        signRequestParams.getExtraName(),
                        signRequestParams.getExtraDate(),
                        signRequestParams.getExtraOnTop(),
                        signRequestParams.getTextPart(),
                        signRequestParams.getSignScale(),
                        signRequestParams.getRed(),
                        signRequestParams.getGreen(),
                        signRequestParams.getBlue(),
                        signRequestParams.getFontSize()
                ))
                .toList();
    }

    private List<FieldFrontDto> toFieldFrontDtos(List<Field> fields, Workflow workflow) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .map(field -> new FieldFrontDto(
                        field.getId(),
                        field.getName(),
                        field.getDescription(),
                        field.getPage(),
                        field.getRequired(),
                        field.getReadOnly(),
                        field.getEditable(),
                        toWorkflowStepNumbers(field, workflow),
                        field.getDefaultValue(),
                        field.getSearchServiceName(),
                        field.getSearchType(),
                        field.getSearchReturn(),
                        field.getType() != null ? field.getType().name().toLowerCase() : null,
                        field.getFavorisable()
                ))
                .toList();
    }

    private List<Integer> toWorkflowStepNumbers(Field field, Workflow workflow) {
        if (field == null || workflow == null || field.getWorkflowSteps() == null || field.getWorkflowSteps().isEmpty()) {
            return List.of();
        }
        List<WorkflowStep> workflowSteps = workflow.getWorkflowSteps();
        if (workflowSteps == null || workflowSteps.isEmpty()) {
            return List.of();
        }

        List<Integer> stepNumbers = new ArrayList<>();
        for (WorkflowStep fieldWorkflowStep : field.getWorkflowSteps()) {
            Long workflowStepId = fieldWorkflowStep != null ? fieldWorkflowStep.getId() : null;
            for (int i = 0; i < workflowSteps.size(); i++) {
                WorkflowStep workflowStep = workflowSteps.get(i);
                if (workflowStepId != null && workflowStep != null && workflowStepId.equals(workflowStep.getId())) {
                    stepNumbers.add(i + 1);
                    break;
                }
            }
        }
        return stepNumbers;
    }

    private SignRequestUiCommonDto buildCommonDto(ShowSignRequestContext context) {
        SignRequest signRequest = context.signRequest();
        return new SignRequestUiCommonDto(
                signRequest.getId(),
                signRequest.getData() != null ? signRequest.getData().getId() : null,
                signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getId() : null,
                context.currentSignRequestParamses(),
                context.currentSignType(),
                context.signable(),
                context.editable(),
                context.comments(),
                context.spots(),
                context.pdf(),
                context.currentStepNumber(),
                context.currentStepMultiSign(),
                context.currentStepSingleSignWithAnnotation(),
                context.currentStepMinSignLevel(),
                context.signImages(),
                context.fields(),
                context.stepRepeatable(),
                signRequest.getStatus(),
                context.action(),
                context.nbSignRequestInSignBookParent(),
                context.notSigned(),
                context.attachmentAlert(),
                context.attachmentRequire(),
                context.manager()
        );
    }

    private String toDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getFirstname() + " " + user.getName();
    }

    public record ShowSignRequestContext(
            String userEppn,
            String authUserEppn,
            boolean isOtpView,
            SignRequest signRequest,
            SignBook signBook,
            LiveWorkflow liveWorkflow,
            LiveWorkflowStep currentStep,
            Workflow workflow,
            boolean signable,
            boolean editable,
            boolean manager,
            boolean attachmentAlert,
            boolean attachmentRequire,
            SignType currentSignType,
            Integer currentStepNumber,
            Long currentStepId,
            boolean currentStepMultiSign,
            boolean currentStepSingleSignWithAnnotation,
            SignLevel currentStepMinSignLevel,
            SignLevel currentStepMaxSignLevel,
            Boolean stepRepeatable,
            int nbSignRequestInSignBookParent,
            boolean lastStep,
            List<Document> toSignDocuments,
            Document toSignDocument,
            boolean pdf,
            Reports reports,
            List<String> signatureIds,
            boolean signatureIssue,
            List<Field> fields,
            List<SignRequestParams> currentSignRequestParamses,
            List<Comment> comments,
            List<SignRequestParams> spots,
            List<String> signImages,
            String signImagesWarningMessage,
            User frontUser,
            User frontAuthUser,
            String action,
            Set<String> supervisors,
            boolean notSigned
    ) {}
}


