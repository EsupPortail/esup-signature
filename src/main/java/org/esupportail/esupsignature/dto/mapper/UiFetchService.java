package org.esupportail.esupsignature.dto.mapper;

import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.io.FileUtils;
import org.esupportail.esupsignature.dto.page.admin.AdminFormListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminFormDetailViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowUpdateViewDto;
import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
import org.esupportail.esupsignature.dto.page.user.wiz.StartFormViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;
import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.dto.ui.global.UserSignatureStateDto;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.CommentFrontDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.FieldFrontDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestFullDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignUiFrontDto;
import org.esupportail.esupsignature.dto.ui.global.UiCountersDto;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiHomeDto;
import org.esupportail.esupsignature.dto.ui.global.UiCurrentUserDto;
import org.esupportail.esupsignature.dto.ui.global.UiUserLookupDto;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.*;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.AuditTrailService;
import org.esupportail.esupsignature.service.DataService;
import org.esupportail.esupsignature.service.LogService;
import org.esupportail.esupsignature.service.CertificatService;
import org.esupportail.esupsignature.service.FieldPropertieService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.ReportService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.SignWithService;
import org.esupportail.esupsignature.service.TagService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserShareService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
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
import java.util.LinkedHashMap;
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
    private final FormService formService;
    private final UserShareService userShareService;
    private final UserService userService;
    private final UserPropertieService userPropertieService;
    private final FieldPropertieService fieldPropertieService;
    private final RecipientService recipientService;
    private final TagService tagService;
    private final PreFillService preFillService;
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
                          FormService formService,
                          UserShareService userShareService,
                          UserService userService,
                          UserPropertieService userPropertieService,
                          FieldPropertieService fieldPropertieService,
                          RecipientService recipientService,
                          TagService tagService,
                          PreFillService preFillService,
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
        this.formService = formService;
        this.userShareService = userShareService;
        this.userService = userService;
        this.userPropertieService = userPropertieService;
        this.fieldPropertieService = fieldPropertieService;
        this.recipientService = recipientService;
        this.tagService = tagService;
        this.preFillService = preFillService;
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
        UiDataDto.UiConfigDto config = buildUiConfig(userEppn, httpSession != null ? httpSession.getMaxInactiveInterval() : null);
        UiCountersDto counters = buildUiCounters(userEppn, authUserEppn);
        UiCurrentUserDto currentUser = buildUiMe(userEppn, authUserEppn, httpSession);
        Map<String, String> preferences = getUiPreferences(authUserEppn);
        AdminUiStatusDto adminStatus = authUserEppn != null && userService.getRoles(authUserEppn).contains("ROLE_ADMIN")
                ? buildAdminUiStatus()
                : null;
        return uiFetchMapper.toUiDataDto(config, counters, currentUser, preferences, adminStatus);
    }

    @Transactional(readOnly = true)
    public UiHomeDto buildUiHomeBootstrap(String userEppn, String authUserEppn, Long startFormId, Long startWorkflowId) {
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

    @Transactional(readOnly = true)
    public AdminWorkflowListViewDto buildAdminWorkflowListView(String authUserEppn,
                                                               String workflowRole,
                                                               DisplayWorkflowType displayWorkflowType,
                                                               List<Tag> selectedTags) {
        DisplayWorkflowType effectiveDisplayWorkflowType = displayWorkflowType != null ? displayWorkflowType : DisplayWorkflowType.system;
        List<Tag> effectiveSelectedTags = selectedTags != null ? selectedTags : List.of();
        List<Workflow> workflows = "admin".equals(workflowRole)
                ? workflowService.getWorkflowsByDisplayWorkflowTypeAndSelectedTags(effectiveDisplayWorkflowType, effectiveSelectedTags)
                : workflowService.getManagerWorkflows(authUserEppn);
        List<String> roles = "manager".equals(workflowRole)
                ? userService.getManagersRoles(authUserEppn)
                : List.of();
        return uiFetchMapper.toAdminWorkflowListViewDto(
                workflowRole,
                effectiveDisplayWorkflowType.name(),
                effectiveSelectedTags.stream().map(Tag::getId).filter(Objects::nonNull).toList(),
                tagService.getAllTags(Pageable.unpaged()).getContent().stream().map(uiFetchMapper::toAdminWorkflowTagDto).toList(),
                roles,
                workflows.stream().map(uiFetchMapper::toAdminWorkflowRowDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminFormListViewDto buildAdminFormListView(String authUserEppn,
                                                       String workflowRole,
                                                       List<Tag> selectedTags,
                                                       Boolean activeVersion) {
        List<Tag> effectiveSelectedTags = selectedTags != null ? selectedTags : List.of();
        List<Form> forms = ("admin".equals(workflowRole)
                ? formService.getAllForms(effectiveSelectedTags, activeVersion)
                : formService.getManagerForms(effectiveSelectedTags, activeVersion, authUserEppn))
                .stream()
                .sorted(java.util.Comparator.comparing(form -> form.getTitle() != null ? form.getTitle().toLowerCase() : ""))
                .toList();
        List<String> roles = "admin".equals(workflowRole)
                ? userService.getAllRoles()
                : userService.getManagersRoles(authUserEppn);
        List<Workflow> workflowTypes = "admin".equals(workflowRole)
                ? workflowService.getSystemWorkflows()
                : workflowService.getManagerWorkflows(authUserEppn);
        List<PreFill> preFillTypes = preFillService.getPreFillValues();
        return uiFetchMapper.toAdminFormListViewDto(
                workflowRole,
                activeVersion,
                effectiveSelectedTags.stream().map(Tag::getId).filter(Objects::nonNull).toList(),
                tagService.getAllTags(Pageable.unpaged()).getContent().stream().map(uiFetchMapper::toAdminFormTagDto).toList(),
                roles,
                workflowTypes.stream().map(uiFetchMapper::toAdminFormWorkflowOptionDto).toList(),
                preFillTypes.stream().map(uiFetchMapper::toAdminFormPreFillOptionDto).toList(),
                forms.stream().map(uiFetchMapper::toAdminFormRowDto).toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminWorkflowUpdateViewDto buildAdminWorkflowUpdateView(String authUserEppn,
                                                                  String workflowRole,
                                                                  Long workflowId) {
        Workflow workflow = workflowService.getById(workflowId);
        List<String> roles = "admin".equals(workflowRole)
                ? userService.getAllRoles()
                : userService.getManagersRoles(authUserEppn);
        List<AdminWorkflowUpdateViewDto.TagDto> allTags = tagService.getAllTags(Pageable.unpaged())
                .getContent()
                .stream()
                .map(uiFetchMapper::toAdminWorkflowUpdateTagDto)
                .toList();
        List<Long> selectedTagIds = workflow.getTags() == null
                ? List.of()
                : workflow.getTags().stream().map(Tag::getId).filter(Objects::nonNull).toList();
        return uiFetchMapper.toAdminWorkflowUpdateViewDto(
                workflowRole,
                workflow,
                (long) signBookService.countSignBooksByWorkflow(workflowId),
                roles,
                allTags,
                selectedTagIds
        );
    }

    @Transactional(readOnly = true)
    public AdminWorkflowUpdateViewDto.WorkflowDto buildAdminWorkflowTargetsWorkflowView(Long workflowId) {
        return uiFetchMapper.toAdminWorkflowUpdateWorkflowDto(workflowService.getById(workflowId));
    }

    @Transactional(readOnly = true)
    public WorkflowViewDto buildWorkflowView(Long workflowId) {
        return uiFetchMapper.toWorkflowViewDto(workflowService.getById(workflowId), null);
    }

    @Transactional(readOnly = true)
    public WorkflowViewDto buildWorkflowWizardView(Long workflowId, String userEppn) {
        Workflow workflow = workflowService.getById(workflowId);
        return uiFetchMapper.toWorkflowViewDto(workflow, workflowService.getHelpMessage(userEppn, workflow));
    }

    @Transactional(readOnly = true)
    public StartFormViewDto buildStartFormWizardView(Long formId, String userEppn) {
        Form form = formService.getById(formId);
        return uiFetchMapper.toStartFormViewDto(form, formService.getHelpMessage(userEppn, form));
    }

    @Transactional(readOnly = true)
    public AdminFormDetailViewDto buildAdminFormUpdateView(String authUserEppn,
                                                           String workflowRole,
                                                           Long formId) {
        Form form = formService.getById(formId);
        List<String> roles = "admin".equals(workflowRole)
                ? userService.getAllRoles()
                : userService.getManagersRoles(authUserEppn);
        List<Workflow> availableWorkflows = "admin".equals(workflowRole)
                ? mergeWorkflowOptions(workflowService.getSystemWorkflows(), form.getWorkflow())
                : workflowService.getManagerWorkflows(authUserEppn);
        List<PreFill> preFillTypes = preFillService.getPreFillValues();
        return uiFetchMapper.toAdminFormDetailViewDto(
                workflowRole,
                form,
                roles,
                availableWorkflows.stream().map(uiFetchMapper::toAdminFormDetailWorkflowOptionDto).toList(),
                preFillTypes.stream().map(uiFetchMapper::toAdminFormDetailPreFillOptionDto).toList(),
                tagService.getAllTags(Pageable.unpaged()).getContent().stream().map(uiFetchMapper::toAdminFormDetailTagDto).toList(),
                form.getTags() == null ? List.of() : form.getTags().stream().map(Tag::getId).filter(Objects::nonNull).toList(),
                Collections.emptyMap(),
                List.of(),
                Collections.emptyMap(),
                null
        );
    }

    @Transactional(readOnly = true)
    public AdminFormDetailViewDto buildAdminFormFieldsView(String authUserEppn,
                                                           String workflowRole,
                                                           Long formId) {
        Form form = formService.getById(formId);
        PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
        Map<String, List<String>> preFillTypeOptions = preFill != null ? preFill.getTypes() : Collections.emptyMap();
        return uiFetchMapper.toAdminFormDetailViewDto(
                workflowRole,
                form,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                preFillTypeOptions,
                List.of(),
                Collections.emptyMap(),
                null
        );
    }

    @Transactional(readOnly = true)
    public AdminFormDetailViewDto buildAdminFormSignsView(String authUserEppn,
                                                          String workflowRole,
                                                          Long formId) {
        Form form = formService.getById(formId);
        List<SignRequestParamsFrontDto> spots = form.getWorkflow() != null
                ? uiFetchMapper.toSignRequestParamsFrontDtos(formService.getSpots(formId))
                : List.of();
        Map<Integer, Long> srpMap = form.getWorkflow() != null
                ? formService.getSrpMap(form)
                : Collections.emptyMap();
        Integer defaultSignImageNumber = userService.getFullUserByEppn(authUserEppn) != null
                ? userService.getFullUserByEppn(authUserEppn).getDefaultSignImageNumber()
                : null;
        return uiFetchMapper.toAdminFormDetailViewDto(
                workflowRole,
                form,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Collections.emptyMap(),
                spots,
                srpMap,
                defaultSignImageNumber
        );
    }

    private List<Workflow> mergeWorkflowOptions(List<Workflow> workflows, Workflow currentWorkflow) {
        Map<Long, Workflow> workflowMap = new LinkedHashMap<>();
        if (workflows != null) {
            workflows.stream()
                    .filter(Objects::nonNull)
                    .filter(workflow -> workflow.getId() != null)
                    .forEach(workflow -> workflowMap.put(workflow.getId(), workflow));
        }
        if (currentWorkflow != null && currentWorkflow.getId() != null) {
            workflowMap.put(currentWorkflow.getId(), currentWorkflow);
        }
        return new ArrayList<>(workflowMap.values());
    }

    private List<UiHomeDto.SignBookItem> buildHomeSignBookItems(String userEppn, String authUserEppn, String statusFilter) {
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

    private UiHomeDto.SignBookItem toHomeSignBookItem(SignBook signBook, String userEppn) {
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

        return new UiHomeDto.SignBookItem(
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

    private List<UiHomeDto.PostitItem> toHomePostitItems(List<Comment> postits) {
        if (postits == null || postits.isEmpty()) {
            return List.of();
        }
        return postits.stream()
                .filter(Objects::nonNull)
                .map(postit -> new UiHomeDto.PostitItem(
                        toDisplayName(postit.getCreateBy()),
                        postit.getText()
                ))
                .toList();
    }

    private List<UiHomeDto.SignRequestItem> toHomeSignRequestItems(List<SignRequest> signRequests) {
        if (signRequests == null || signRequests.isEmpty()) {
            return List.of();
        }
        return signRequests.stream()
                .filter(Objects::nonNull)
                .map(signRequest -> new UiHomeDto.SignRequestItem(
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

    public UiCurrentUserDto buildUiMe(String userEppn, String authUserEppn, HttpSession httpSession) {
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
        Boolean certificatProblem = certificatService.checkCertificatProblem(userService.getRoles(userEppn));
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

    public UiDataDto.UiConfigDto buildUiConfig(String userEppn, Integer maxInactiveInterval) {
        UiGlobalPropertiesDto uiGlobalProperties = userEppn != null ? buildUiGlobalProperties(userEppn) : null;
        String profile = null;
        if (environment.getActiveProfiles().length > 0 && "dev".equals(environment.getActiveProfiles()[0])) {
            profile = environment.getActiveProfiles()[0];
        }
        String versionApp = buildProperties != null ? buildProperties.getVersion() : "dev";
        return uiFetchMapper.toUiConfigDto(
                uiGlobalProperties,
                smsProperties.getEnableSms() ? smsProperties.getServiceName() : null,
                globalProperties.getSmsRequired(),
                validationService != null,
                globalProperties.getApplicationEmail(),
                maxInactiveInterval,
                globalProperties.getHoursBeforeRefreshNotif(),
                globalProperties.getInfiniteScrolling(),
                versionApp,
                profile
        );
    }

    public UiGlobalPropertiesDto buildUiGlobalProperties(String userEppn) {
        GlobalProperties myGlobalProperties = new GlobalProperties();
        BeanUtils.copyProperties(globalProperties, myGlobalProperties);
        userService.parseRoles(userEppn, myGlobalProperties);
        myGlobalProperties.newVersion = globalProperties.newVersion;
        return UiGlobalPropertiesDto.fromGlobalProperties(myGlobalProperties);
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

    public List<UiUserLookupDto> checkTempUsers(List<String> recipientEmails) {
        List<User> tempUsers = userService.checkTempUsers(
                recipientService
                        .convertRecipientEmailsToStep(recipientEmails)
                        .stream()
                        .map(WorkflowStepDto::getRecipients)
                        .flatMap(List::stream)
                        .toList()
        );
        return uiFetchMapper.toUiUserLookupDtos(tempUsers == null ? List.of() : tempUsers);
    }

    public List<UiUserLookupDto> getFavoriteUsers(String authUserEppn) {
        return uiFetchMapper.toUiUserLookupDtos(userPropertieService.getFavoritesEmails(authUserEppn));
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

    public ShowSignRequestDto buildShowSignRequestBackDto(ShowSignRequestContext context) {
        SignRequestFullDto common = buildCommonDto(context);
        SignRequest signRequest = context.signRequest();
        SignBook signBook = context.signBook();
        String userEppn = context.userEppn();
        String authUserEppn = context.authUserEppn();
        ShowSignRequestDto.SignRequestLightDto request = new ShowSignRequestDto.SignRequestLightDto(
                signRequest.getId(),
                signRequest.getStatus(),
                signRequest.getDeleted(),
                signRequest.getToken(),
                signRequest.getCreateBy() != null
                        ? new ShowSignRequestDto.SignRequestUserDto(
                        signRequest.getCreateBy().getId(),
                        signRequest.getCreateBy().getEppn(),
                        signRequest.getCreateBy().getFirstname(),
                        signRequest.getCreateBy().getName()
                )
                        : null,
                signRequest.getLinks() != null ? new ArrayList<>(signRequest.getLinks()) : new ArrayList<>()
        );
        Workflow workflow = context.workflow();
        SignBookLightDto signBookLight = new SignBookLightDto(
                signBook.getId(),
                signBook.getWorkflowName(),
                signBook.getSubject(),
                signBook.getDescription(),
                signBook.getStatus(),
                signBook.getDeleted(),
                signBook.isEditable(),
                signBook.getArchiveStatus(),
                signBook.getCreateDate(),
                signBook.getViewers() != null
                        ? signBook.getViewers().stream()
                        .map(viewer -> new ShowSignRequestDto.SignBookViewerDto(
                                viewer.getId(),
                                viewer.getFirstname(),
                                viewer.getName(),
                                viewer.getEmail()
                        ))
                        .toList()
                        : List.of()
        );
        ShowSignRequestDto.WorkflowMetaDto workflowMeta = new ShowSignRequestDto.WorkflowMetaDto(
                workflow != null,
                workflow == null || Boolean.TRUE.equals(workflow.getExternalCanReaderAnnotations()),
                workflow != null && Boolean.TRUE.equals(workflow.getDisableSidebarForExternal()),
                workflow == null || Boolean.TRUE.equals(workflow.getExternalCanReaderAttachments()),
                workflow == null || Boolean.TRUE.equals(workflow.getExternalCanEdit()),
                workflow != null && Boolean.TRUE.equals(workflow.getExternalCanEditAttachments()),
                workflow == null || Boolean.TRUE.equals(workflow.getAuthorizeClone()),
                workflow != null && Boolean.TRUE.equals(workflow.getForbidDownloadsBeforeEnd()),
                workflow != null && Boolean.TRUE.equals(workflow.getSendAlertToAllRecipients()),
                workflow != null && workflow.getWorkflowSteps() != null ? workflow.getWorkflowSteps().size() : 0,
                workflow != null && workflow.getMailFrom() != null ? workflow.getMailFrom() : ""
        );

        boolean displayNotif = !context.isOtpView() && signRequestService.isDisplayNotif(signRequest, userEppn);
        boolean tempUsers = !context.isOtpView() && signBookService.isTempUsers(signBook.getId());
        String toSignDocumentContentType = context.toSignDocument() != null ? context.toSignDocument().getContentType() : null;
        List<Comment> postits = signRequestService.getPostits(signRequest.getId());
        List<ShowSignRequestDto.AttachmentDto> attachments = signRequestService.getAttachments(signRequest.getId()).stream()
                .map(attachment -> new ShowSignRequestDto.AttachmentDto(
                        attachment.getId(),
                        attachment.getFileName(),
                        attachment.getCreateBy() != null
                                ? new ShowSignRequestDto.AttachmentUserDto(
                                attachment.getCreateBy().getEppn(),
                                attachment.getCreateBy().getFirstname(),
                                attachment.getCreateBy().getName()
                        )
                                : null
                ))
                .toList();
        List<ShowSignRequestDto.DocumentDto> originalDocuments = signRequest.getOriginalDocuments().stream()
                .map(this::toDocumentDto)
                .toList();
        List<ShowSignRequestDto.DocumentDto> signedDocuments = signRequest.getSignedDocuments().stream()
                .map(this::toDocumentDto)
                .toList();
        String exportedDocumentURI = signRequest.getExportedDocumentURI();
        String lastSignedDocumentContentType = signRequest.getLastSignedDocument() != null
                ? signRequest.getLastSignedDocument().getContentType()
                : null;
        SignBook nextSignBook = signBookService.getNextSignBook(signRequest.getId(), userEppn, authUserEppn);
        SignRequest nextSignRequest = nextSignBook != null
                ? signBookService.getNextSignRequest(signRequest.getId(), nextSignBook.getId())
                : null;
        boolean hasNextSignBook = nextSignBook != null;
        Long nextSignRequestId = nextSignRequest != null ? nextSignRequest.getId() : null;
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
        List<ShowSignRequestDto.StepDto> steps = context.liveWorkflow() != null
                ? context.liveWorkflow().getLiveWorkflowSteps().stream()
                .map(step -> new ShowSignRequestDto.StepDto(
                        step.getId(),
                        step.getDescription(),
                        step.getWorkflowStep() != null ? step.getWorkflowStep().getChangeable() : false,
                        step.getSignType(),
                        step.getAutoSign(),
                        step.getAllSignToComplete(),
                        step.getRepeatable(),
                        step.getUsers().stream()
                                .map(this::toStepUserDto)
                                .toList(),
                        step.getRecipients().stream()
                                .map(recipient -> new ShowSignRequestDto.StepRecipientDto(
                                        recipient.getId(),
                                        recipient.getUser() != null ? toStepUserDto(recipient.getUser()) : null,
                                        recipient.getSigned()
                                ))
                                .toList()
                ))
                .toList()
                : new ArrayList<>();
        List<ShowSignRequestDto.TargetDto> targets = context.liveWorkflow() != null
                ? context.liveWorkflow().getTargets().stream()
                .map(target -> new ShowSignRequestDto.TargetDto(
                        target.getTargetUri(),
                        target.getProtectedTargetUri(),
                        target.getTargetOk()
                ))
                .toList()
                : new ArrayList<>();
        Map<Long, ShowSignRequestDto.RecipientActionDto> recipientActions = new LinkedHashMap<>();
        if (signRequest.getRecipientHasSigned() != null) {
            signRequest.getRecipientHasSigned().forEach((recipient, action) -> {
                if (recipient != null && recipient.getId() != null && action != null) {
                    recipientActions.put(
                            recipient.getId(),
                            new ShowSignRequestDto.RecipientActionDto(
                                    action.getActionType(),
                                    action.getDate()
                            )
                    );
                }
            });
        }
        List<ShowSignRequestDto.SignRequestTabDto> signRequestTabs = signBook.getSignRequests().stream()
                .map(signRequestTab -> new ShowSignRequestDto.SignRequestTabDto(
                        signRequestTab.getId(),
                        signRequestTab.getTitle(),
                        signRequestTab.getStatus(),
                        signRequestTab.getDeleted()
                ))
                .toList();
        boolean viewedByCurrentUser = isViewedByUser(signRequest, userEppn);
        boolean viewRight = preAuthorizeService.checkUserViewRights(signRequest.getId(), userEppn, authUserEppn);
        List<Log> logs = logService.getFullBySignRequest(signRequest.getId());
        String pdfaCheck = !context.toSignDocuments().isEmpty() ? context.toSignDocuments().get(0).getPdfaCheck() : null;
        boolean auditTrailChecked = signBook.getStatus().equals(SignRequestStatus.completed) || signBook.getStatus().equals(SignRequestStatus.exported);
        List<RecipientWsDto> externalsRecipients = auditTrailChecked
                ? signRequestService.getExternalRecipients(signRequest.getId())
                : new ArrayList<>();

        return new ShowSignRequestDto(
                signBookLight,
                request,
                common,
                workflowMeta,
                context.isOtpView() ? "otp" : "user",
                displayNotif,
                tempUsers,
                context.lastStep(),
                signRequestService.isCurrentUserAsSigned(signRequest, userEppn),
                context.signatureIds(),
                context.signatureIssue(),
                context.supervisors(),
                toSignDocumentContentType,
                postits,
                attachments,
                originalDocuments,
                signedDocuments,
                exportedDocumentURI,
                lastSignedDocumentContentType,
                hasNextSignBook,
                nextSignRequestId,
                signWiths,
                auditTrail,
                size,
                sealCertOK,
                sealCertificatPropertieses,
                steps,
                targets,
                recipientActions,
                signRequestTabs,
                steps.size(),
                viewedByCurrentUser,
                viewRight,
                logs,
                pdfaCheck,
                auditTrailChecked,
                externalsRecipients
        );
    }

    private ShowSignRequestDto.StepUserDto toStepUserDto(User user) {
        return new ShowSignRequestDto.StepUserDto(
                user.getId(),
                user.getFirstname(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getHidedPhone(),
                user.getUserType()
        );
    }

    private ShowSignRequestDto.DocumentDto toDocumentDto(Document document) {
        return new ShowSignRequestDto.DocumentDto(
                document.getId(),
                document.getFileName(),
                document.getSize(),
                document.getContentType()
        );
    }

    public SignUiFrontDto buildSignUiFrontDto(ShowSignRequestContext context) {
        SignRequestFullDto common = buildCommonDto(context);
        User frontUser = context.frontUser();
        User frontAuthUser = context.frontAuthUser();

        return new SignUiFrontDto(
                common.signRequestId(),
                common.dataId(),
                common.formId(),
                context.liveWorkflow() != null
                        ? context.liveWorkflow().getLiveWorkflowSteps().stream()
                        .map(step -> new ShowSignRequestDto.StepDto(
                                step.getId(),
                                step.getDescription(),
                                step.getWorkflowStep() != null ? step.getWorkflowStep().getChangeable() : false,
                                step.getSignType(),
                                step.getAutoSign(),
                                step.getAllSignToComplete(),
                                step.getRepeatable(),
                                step.getUsers().stream()
                                        .map(this::toStepUserDto)
                                        .toList(),
                                step.getRecipients().stream()
                                        .map(recipient -> new ShowSignRequestDto.StepRecipientDto(
                                                recipient.getId(),
                                                recipient.getUser() != null ? toStepUserDto(recipient.getUser()) : null,
                                                recipient.getSigned()
                                        ))
                                        .toList()
                        ))
                        .toList()
                        : List.of(),
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
                        signRequestParams.getFontSize(),
                        signRequestParams.getRecipient() != null ? signRequestParams.getRecipient().getId() : null
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

    private SignRequestFullDto buildCommonDto(ShowSignRequestContext context) {
        SignRequest signRequest = context.signRequest();
        return new SignRequestFullDto(
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
                context.currentStep() != null ? context.currentStep().getRepeatableSignType() : null,
                signRequest.getStatus(),
                context.action(),
                context.nbSignRequestInSignBookParent(),
                context.notSigned(),
                context.attachmentAlert(),
                context.attachmentRequire(),
                context.manager(),
                context.manager() && (signRequest.getStatus() == SignRequestStatus.draft || signRequest.getStatus() == SignRequestStatus.pending),
                signRequest.getDocumentsHistory() != null
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


