package org.esupportail.esupsignature.service.ui;

import jakarta.servlet.http.HttpSession;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.dto.mapper.UiAdminFormMapper;
import org.esupportail.esupsignature.dto.mapper.UiFetchMapper;
import org.esupportail.esupsignature.dto.page.admin.*;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.page.user.wiz.StartFormViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.dto.ui.global.*;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DisplayWorkflowType;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.custom.SessionRepositoryCustom;
import org.esupportail.esupsignature.service.*;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.security.PreAuthorizeService;
import org.esupportail.esupsignature.service.utils.sign.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.*;

@Service
public class UiFetchService {

    private static final Logger logger = LoggerFactory.getLogger(UiFetchService.class);

    private final GlobalProperties globalProperties;
    private final SmsProperties smsProperties;
    private final SignRequestService signRequestService;
    private final SignBookService signBookService;
    private final WorkflowService workflowService;
    private final FormService formService;
    private final UserShareService userShareService;
    private final UserService userService;
    private final UserPropertieService userPropertieService;
    private final FieldPropertieService fieldPropertieService;
    private final RecipientService recipientService;
    private final TagService tagService;
    private final PreFillService preFillService;
    private final ReportService reportService;
    private final PreAuthorizeService preAuthorizeService;
    private final Environment environment;
    private final BuildProperties buildProperties;
    private final ValidationService validationService;
    private final CertificatService certificatService;
    private final DSSService dssService;
    private final SessionRepositoryCustom sessionRepositoryCustom;
    private final UiFetchMapper uiFetchMapper;
    private final UiAdminFormMapper uiAdminFormMapper;
    private final MessageSource messageSource;

    public UiFetchService(GlobalProperties globalProperties, SmsProperties smsProperties, SignRequestService signRequestService, SignBookService signBookService, WorkflowService workflowService, FormService formService, UserShareService userShareService, UserService userService, UserPropertieService userPropertieService, FieldPropertieService fieldPropertieService, RecipientService recipientService, TagService tagService, PreFillService preFillService, ReportService reportService, PreAuthorizeService preAuthorizeService, Environment environment, @Autowired(required = false) BuildProperties buildProperties, ValidationService validationService, CertificatService certificatService, @Autowired(required = false) DSSService dssService, SessionRepositoryCustom sessionRepositoryCustom, UiFetchMapper uiFetchMapper, UiAdminFormMapper uiAdminFormMapper, MessageSource messageSource) {
        this.globalProperties = globalProperties;
        this.smsProperties = smsProperties;
        this.signRequestService = signRequestService;
        this.signBookService = signBookService;
        this.workflowService = workflowService;
        this.formService = formService;
        this.userShareService = userShareService;
        this.userService = userService;
        this.userPropertieService = userPropertieService;
        this.fieldPropertieService = fieldPropertieService;
        this.recipientService = recipientService;
        this.tagService = tagService;
        this.preFillService = preFillService;
        this.reportService = reportService;
        this.preAuthorizeService = preAuthorizeService;
        this.environment = environment;
        this.buildProperties = buildProperties;
        this.validationService = validationService;
        this.certificatService = certificatService;
        this.dssService = dssService;
        this.sessionRepositoryCustom = sessionRepositoryCustom;
        this.uiFetchMapper = uiFetchMapper;
        this.uiAdminFormMapper = uiAdminFormMapper;
        this.messageSource = messageSource;
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
    public List<UiSearchResult> buildHomeSearchResults(String authUserEppn, List<UiSearchRequest> searchRequests) {
        if (searchRequests == null || searchRequests.isEmpty()) {
            return List.of();
        }

        List<String> words = new ArrayList<>();
        List<String> types = new ArrayList<>();
        Set<Long> tagIds = new HashSet<>();

        for (UiSearchRequest searchRequest : searchRequests) {
            String value = searchRequest.getValue();
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (value.startsWith("tag:")) {
                try {
                    tagIds.add(Long.valueOf(value.split(":")[1]));
                } catch (NumberFormatException e) {
                    logger.warn("Tag de recherche invalide: {}", value);
                }
            } else if (value.startsWith("type:")) {
                types.add(value.split(":")[1]);
            } else if (!value.contains(":")) {
                words.add(value);
            }
        }

        List<UiSearchResult> searchResults = new ArrayList<>();
        List<Workflow> workflows = List.of();
        List<Form> forms = List.of();

        if (types.isEmpty() || types.contains("workflow")) {
            workflows = workflowService.getWorkflowsByUser(authUserEppn, authUserEppn).stream()
                    .filter(workflow -> matchesTags(workflow.getTags(), tagIds) && matchesWords(workflow.getDescription(), words))
                    .toList();
            searchResults.addAll(workflows.stream().map(this::toWorkflowSearchResult).toList());
        }

        if (types.isEmpty() || types.contains("form")) {
            forms = formService.getFormsByUser(authUserEppn, authUserEppn).stream()
                    .filter(form -> matchesTags(form.getTags(), tagIds) && matchesWords(form.getDescription(), words))
                    .toList();
            searchResults.addAll(forms.stream().map(this::toFormSearchResult).toList());
        }

        if (types.isEmpty() || types.contains("signBookLight")) {
            searchResults.addAll(buildSignBookSearchResults(authUserEppn, words, tagIds, workflows, forms));
        }

        return searchResults;
    }

    @Transactional(readOnly = true)
    public AdminWorkflowListViewDto buildAdminWorkflowListView(String authUserEppn, String workflowRole, DisplayWorkflowType displayWorkflowType, List<Tag> selectedTags) {
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
    public AdminFormListViewDto buildAdminFormListView(String authUserEppn, String workflowRole, List<Tag> selectedTags, Boolean activeVersion) {
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
    public AdminWorkflowUpdateViewDto buildAdminWorkflowUpdateView(String authUserEppn, String workflowRole, Long workflowId) {
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
    public WorkflowViewDto buildWorkflowView(Long workflowId, String userEppn) {
        Workflow workflow = workflowService.getById(workflowId);
        if(workflow.getFromCode()) {
            workflow = workflowService.computeWorkflow(workflow, null, userEppn, true);
            return uiFetchMapper.toWorkflowViewDto(workflow, workflowService.getHelpMessage(userEppn, workflow), userEppn);
        }
        return uiFetchMapper.toWorkflowViewDto(workflowService.getById(workflowId), null, userEppn);
    }

    @Transactional(readOnly = true)
    public WorkflowViewDto buildWorkflowWizardView(Long workflowId, String userEppn) {
        Workflow workflow = workflowService.getById(workflowId);
        return uiFetchMapper.toWorkflowViewDto(workflow, workflowService.getHelpMessage(userEppn, workflow), userEppn);
    }

    @Transactional(readOnly = true)
    public StartFormViewDto buildStartFormWizardView(Long formId, String userEppn) {
        Form form = formService.getById(formId);
        return uiFetchMapper.toStartFormViewDto(form, formService.getHelpMessage(userEppn, form), userEppn);
    }

    @Transactional(readOnly = true)
    public AdminFormDetailViewDto buildAdminFormUpdateView(String authUserEppn, String workflowRole, Long formId) {
        Form form = formService.getById(formId);
        List<String> roles = "admin".equals(workflowRole)
                ? userService.getAllRoles()
                : userService.getManagersRoles(authUserEppn);
        List<Workflow> availableWorkflows = "admin".equals(workflowRole)
                ? mergeWorkflowOptions(workflowService.getSystemWorkflows(), form.getWorkflow())
                : workflowService.getManagerWorkflows(authUserEppn);
        List<PreFill> preFillTypes = preFillService.getPreFillValues();
        return uiAdminFormMapper.toAdminFormDetailViewDto(
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
    public AdminFormDetailViewDto buildAdminFormFieldsView(String workflowRole, Long formId) {
        Form form = formService.getById(formId);
        PreFill preFill = preFillService.getPreFillServiceByName(form.getPreFillType());
        Map<String, List<String>> preFillTypeOptions = preFill != null ? preFill.getTypes() : Collections.emptyMap();
        return uiAdminFormMapper.toAdminFormDetailViewDto(
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
    public AdminFormDetailViewDto buildAdminFormSignsView(String authUserEppn, String workflowRole, Long formId) {
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
        return uiAdminFormMapper.toAdminFormDetailViewDto(
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

        UiHomeDto.SignBookItem dto = new UiHomeDto.SignBookItem();
        dto.setId(signBook.getId());
        dto.setPrimarySignRequestId(primarySignRequest.getId());
        dto.setDescription(signBook.getDescription());
        dto.setSubject(signBook.getSubject());
        dto.setWorkflowName(signBook.getWorkflowName());
        dto.setCreateDateLabel(formatHomeDate(signBook.getCreateDate()));
        dto.setListTitle(listTitle);
        dto.setViewedByCurrentUser(isViewedByUser(primarySignRequest, userEppn));
        dto.setHasAttachments(primarySignRequest.getAttachments() != null && !primarySignRequest.getAttachments().isEmpty());
        dto.setPostits(toHomePostitItems(signBook.getPostits()));
        dto.setSignRequests(toHomeSignRequestItems(signBook.getSignRequests()));
        return dto;
    }

    private List<UiHomeDto.PostitItem> toHomePostitItems(List<Comment> postits) {
        if (postits == null || postits.isEmpty()) {
            return List.of();
        }
        return postits.stream()
                .filter(Objects::nonNull)
                .map(postit -> {
                    UiHomeDto.PostitItem dto = new UiHomeDto.PostitItem();
                    dto.setAuthor(toDisplayName(postit.getCreateBy()));
                    dto.setText(postit.getText());
                    return dto;
                })
                .toList();
    }

    private List<UiHomeDto.SignRequestItem> toHomeSignRequestItems(List<SignRequest> signRequests) {
        if (signRequests == null || signRequests.isEmpty()) {
            return List.of();
        }
        return signRequests.stream()
                .filter(Objects::nonNull)
                .map(signRequest -> {
                    UiHomeDto.SignRequestItem dto = new UiHomeDto.SignRequestItem();
                    dto.setId(signRequest.getId());
                    dto.setTitle(signRequest.getTitle());
                    dto.setStatus(signRequest.getStatus() != null ? signRequest.getStatus().name() : null);
                    return dto;
                })
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
                signBookService.nbToSignSignBooks(userEppn, authUserEppn),
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

    public UserSignatureStateDto updateProfile(String userEppn, String authUserEppn, String signImageBase64, String name, String firstname, Long signRequestId, EmailAlertFrequency emailAlertFrequency, Integer emailAlertHour, DayOfWeek emailAlertDay, MultipartFile multipartKeystore, HttpSession httpSession) throws Exception {
        userService.updateUser(authUserEppn, name, firstname, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, null, false);
        return buildUserSignatureState(userEppn, authUserEppn, signRequestId, httpSession);
    }

    public UserSignatureStateDto deleteSignature(String userEppn, String authUserEppn, long signatureId, Long signRequestId, HttpSession httpSession) {
        userService.deleteSign(authUserEppn, signatureId);
        return buildUserSignatureState(userEppn, authUserEppn, signRequestId, httpSession);
    }

    public void markWarningsRead(String authUserEppn) {
        signRequestService.warningReaded(authUserEppn);
    }

    private UserSignatureStateDto buildUserSignatureState(String userEppn, String authUserEppn, Long signRequestId, HttpSession httpSession) {
        User user = userService.getFullUserByEppn(authUserEppn);
        UserSignatureStateDto dto = new UserSignatureStateDto();
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setSignImageIds(user.getSignImagesIds());
        dto.setSignImages(getSignImages(signRequestId, userEppn, authUserEppn, httpSession));
        try {
            dto.setDefaultSignImage(userService.getDefaultImage64(authUserEppn));
        } catch (IOException e) {
            logger.warn("unable to get default sign image", e);
        }
        return dto;
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


    private String toDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getFirstname() + " " + user.getName();
    }

    private List<UiSearchResult> buildSignBookSearchResults(String authUserEppn, List<String> words, Set<Long> tagIds, List<Workflow> workflows, List<Form> forms) {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "createDate"));
        List<SignBook> allSignBooks = signBookService.getSignBooks(authUserEppn, authUserEppn, "all", null, null, null, null, null, pageable).getContent();
        Set<SignBook> signBooks = new LinkedHashSet<>();

        if (words.isEmpty() && workflows.isEmpty() && forms.isEmpty()) {
            signBooks.addAll(allSignBooks);
        } else {
            for (String word : words) {
                signBooks.addAll(signBookService.getSignBooks(authUserEppn, authUserEppn, "all", null, null, word, null, null, Pageable.unpaged()).getContent());
            }
            for (Workflow workflow : workflows) {
                signBooks.addAll(allSignBooks.stream()
                        .filter(signBook -> signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().equals(workflow))
                        .toList());
            }
            for (Form form : forms) {
                signBooks.addAll(allSignBooks.stream()
                        .filter(signBook -> signBook.getLiveWorkflow().getWorkflow() != null && signBook.getLiveWorkflow().getWorkflow().equals(form.getWorkflow()))
                        .toList());
            }
        }

        return signBooks.stream()
                .filter(signBook -> tagIds.isEmpty() || hasAllTags(getWorkflowTags(signBook), tagIds))
                .map(this::toSignBookSearchResult)
                .toList();
    }

    private UiSearchResult toWorkflowSearchResult(Workflow workflow) {
        UiSearchResult searchResult = new UiSearchResult();
        searchResult.setIcon("fi fi-rr-diagram-project project-diagram-color");
        searchResult.setTitle(workflow.getDescription());
        searchResult.setUrl("/user/start-workflow/" + workflow.getId());
        searchResult.setTags(renderTags(workflow.getTags()));
        return searchResult;
    }

    private UiSearchResult toFormSearchResult(Form form) {
        UiSearchResult searchResult = new UiSearchResult();
        searchResult.setIcon("fi fi-rr-poll-h file-alt-color");
        searchResult.setTitle(form.getTitle());
        searchResult.setUrl("/user/start-form/" + form.getId());
        searchResult.setTags(renderTags(form.getTags()));
        return searchResult;
    }

    private UiSearchResult toSignBookSearchResult(SignBook signBook) {
        UiSearchResult searchResult = new UiSearchResult();
        searchResult.setIcon("fi fi-rr-file");
        searchResult.setTitle(signBook.getSubject());
        searchResult.setUrl("/user/signbooks/" + signBook.getId());
        searchResult.setDate(signBook.getCreateDate());
        searchResult.setTags(renderTags(getWorkflowTags(signBook)));
        String status = messageSource.getMessage("signbook.status." + signBook.getStatus().name(), null, Locale.ROOT);
        String color = messageSource.getMessage("signbook.status.color." + signBook.getStatus().name(), null, Locale.ROOT);
        String icon = messageSource.getMessage("signbook.status.icon." + signBook.getStatus().name(), null, Locale.ROOT);
        String badge = "<div class='badge rounded-pill badge-status text-bg-" + color + "'><i class='fi " + icon + "'></i><span class='d-md-inline-flex'>" + status + "</span></div>";
        searchResult.setStatus(badge);
        return searchResult;
    }

    private List<Tag> getWorkflowTags(SignBook signBook) {
        if (signBook.getLiveWorkflow() == null || signBook.getLiveWorkflow().getWorkflow() == null) {
            return List.of();
        }
        return signBook.getLiveWorkflow().getWorkflow().getTags();
    }

    private boolean matchesWords(String source, List<String> words) {
        return words.isEmpty() || words.stream().anyMatch(word -> source != null && source.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT)));
    }

    private boolean matchesTags(Collection<Tag> sourceTags, Set<Long> expectedTagIds) {
        return expectedTagIds.isEmpty() || hasAllTags(sourceTags, expectedTagIds);
    }

    private boolean hasAllTags(Collection<Tag> sourceTags, Set<Long> expectedTagIds) {
        Set<Long> sourceTagIds = sourceTags.stream()
                .filter(Objects::nonNull)
                .map(Tag::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return sourceTagIds.containsAll(expectedTagIds);
    }

    private String renderTags(Collection<Tag> tags) {
        StringBuilder renderedTags = new StringBuilder();
        for (Tag tag : tags) {
            if (tag != null) {
                renderedTags.append("<span style=\"background-color: ")
                        .append(tag.getColor())
                        .append("\" class=\"badge\">")
                        .append(tag.getName())
                        .append("</span> ");
            }
        }
        return renderedTags.toString();
    }

}


