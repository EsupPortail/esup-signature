package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.admin.AdminFormDetailViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminFormListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowUpdateViewDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.page.user.wiz.StartFormViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.dto.ui.global.UiCountersDto;
import org.esupportail.esupsignature.dto.ui.global.UiCurrentUserDto;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiHomeDto;
import org.esupportail.esupsignature.dto.ui.global.UiUserLookupDto;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class UiFetchMapper {

    private final UiGlobalMapper uiGlobalMapper;
    private final UiAdminWorkflowMapper uiAdminWorkflowMapper;
    private final UiAdminFormMapper uiAdminFormMapper;
    private final UiWorkflowMapper uiWorkflowMapper;

    public UiFetchMapper(UiGlobalMapper uiGlobalMapper, UiAdminWorkflowMapper uiAdminWorkflowMapper, UiAdminFormMapper uiAdminFormMapper, UiWorkflowMapper uiWorkflowMapper) {
        this.uiGlobalMapper = uiGlobalMapper;
        this.uiAdminWorkflowMapper = uiAdminWorkflowMapper;
        this.uiAdminFormMapper = uiAdminFormMapper;
        this.uiWorkflowMapper = uiWorkflowMapper;
    }

    public AdminUiStatusDto toAdminUiStatusDto(Integer nbSessions, Boolean dssStatus) {
        return uiGlobalMapper.toAdminUiStatusDto(nbSessions, dssStatus);
    }

    public UiDataDto toUiDataDto(UiDataDto.UiConfigDto config, UiCountersDto counters, UiCurrentUserDto currentUser, Map<String, String> preferences, AdminUiStatusDto adminStatus) {
        return uiGlobalMapper.toUiDataDto(config, counters, currentUser, preferences, adminStatus);
    }

    public UiHomeDto toUiHomeBootstrapDto(Long startFormId, Long startWorkflowId, String warningReadUrl, String searchUrl, String searchTitlesUrl, List<UiHomeDto.SignBookItem> toSignSignBooks, List<UiHomeDto.SignBookItem> pendingSignBooks) {
        return uiGlobalMapper.toUiHomeBootstrapDto(startFormId, startWorkflowId, warningReadUrl, searchUrl, searchTitlesUrl, toSignSignBooks, pendingSignBooks);
    }

    public UiCurrentUserDto toUiMeDto(User user, Set<String> userRoles, User authUser, Set<String> authUserRoles, List<User> suUsers, List<Long> userImagesIds, String keystoreFileName, Map<UiParams, String> uiParams, Object securityServiceName) {
        return uiGlobalMapper.toUiMeDto(user, userRoles, authUser, authUserRoles, suUsers, userImagesIds, keystoreFileName, uiParams, securityServiceName);
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(List<User> users) {
        return uiGlobalMapper.toUiUserLookupDtos(users);
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(Set<User> users) {
        return uiGlobalMapper.toUiUserLookupDtos(users);
    }

    public UiCountersDto toUiCountersDto(Long nbSignRequests, Long nbToSign, Long nbDeleted, Integer reportNumber, Integer managedWorkflowsSize, Boolean isRoleManager, Boolean isOneSignShare, Boolean isOneReadShare, Boolean certificatProblem) {
        return uiGlobalMapper.toUiCountersDto(nbSignRequests, nbToSign, nbDeleted, reportNumber, managedWorkflowsSize, isRoleManager, isOneSignShare, isOneReadShare, certificatProblem);
    }

    public UiDataDto.UiConfigDto toUiConfigDto(UiGlobalPropertiesDto globalProperties, String enableSms, Boolean smsRequired, Boolean validationToolsEnabled, String applicationEmail, Integer maxInactiveInterval, Integer hoursBeforeRefreshNotif, Boolean infiniteScrolling, String versionApp, String profile) {
        return uiGlobalMapper.toUiConfigDto(globalProperties, enableSms, smsRequired, validationToolsEnabled, applicationEmail, maxInactiveInterval, hoursBeforeRefreshNotif, infiniteScrolling, versionApp, profile);
    }

    public Map<String, String> toUiParamsMap(Map<UiParams, String> uiParams) {
        return uiGlobalMapper.toUiParamsMap(uiParams);
    }

    public AdminWorkflowListViewDto toAdminWorkflowListViewDto(String workflowRole, String displayWorkflowType, List<Long> selectedTagIds, List<AdminWorkflowListViewDto.TagDto> allTags, List<String> roles, List<AdminWorkflowListViewDto.RowDto> workflows) {
        return uiAdminWorkflowMapper.toAdminWorkflowListViewDto(workflowRole, displayWorkflowType, selectedTagIds, allTags, roles, workflows);
    }

    public AdminWorkflowListViewDto.RowDto toAdminWorkflowRowDto(Workflow workflow) {
        return uiAdminWorkflowMapper.toAdminWorkflowRowDto(workflow);
    }

    public AdminWorkflowListViewDto.TagDto toAdminWorkflowTagDto(Tag tag) {
        return uiAdminWorkflowMapper.toAdminWorkflowTagDto(tag);
    }

    public AdminWorkflowUpdateViewDto toAdminWorkflowUpdateViewDto(String workflowRole, Workflow workflow, Long nbWorkflowSignRequests, List<String> roles, List<AdminWorkflowUpdateViewDto.TagDto> allTags, List<Long> selectedTagIds) {
        return uiAdminWorkflowMapper.toAdminWorkflowUpdateViewDto(workflowRole, workflow, nbWorkflowSignRequests, roles, allTags, selectedTagIds);
    }

    public AdminWorkflowUpdateViewDto.WorkflowDto toAdminWorkflowUpdateWorkflowDto(Workflow workflow) {
        return uiAdminWorkflowMapper.toAdminWorkflowUpdateWorkflowDto(workflow);
    }

    public AdminWorkflowUpdateViewDto.TagDto toAdminWorkflowUpdateTagDto(Tag tag) {
        return uiAdminWorkflowMapper.toAdminWorkflowUpdateTagDto(tag);
    }

    public WorkflowViewDto toWorkflowViewDto(Workflow workflow, String messageToDisplay, String userEppn) {
        return uiWorkflowMapper.toWorkflowViewDto(workflow, messageToDisplay, userEppn);
    }

    public StartFormViewDto toStartFormViewDto(Form form, String messageToDisplay, String userEppn) {
        return uiWorkflowMapper.toStartFormViewDto(form, messageToDisplay, userEppn);
    }

    public AdminFormListViewDto toAdminFormListViewDto(String workflowRole, Boolean activeVersion, List<Long> selectedTagIds, List<AdminFormListViewDto.TagDto> allTags, List<String> roles, List<AdminFormListViewDto.WorkflowOptionDto> workflowTypes, List<AdminFormListViewDto.PreFillOptionDto> preFillTypes, List<AdminFormListViewDto.RowDto> forms) {
        return uiAdminFormMapper.toAdminFormListViewDto(workflowRole, activeVersion, selectedTagIds, allTags, roles, workflowTypes, preFillTypes, forms);
    }

    public AdminFormListViewDto.RowDto toAdminFormRowDto(Form form) {
        return uiAdminFormMapper.toAdminFormRowDto(form);
    }

    public AdminFormListViewDto.WorkflowOptionDto toAdminFormWorkflowOptionDto(Workflow workflow) {
        return uiAdminFormMapper.toAdminFormWorkflowOptionDto(workflow);
    }

    public AdminFormListViewDto.PreFillOptionDto toAdminFormPreFillOptionDto(PreFill preFill) {
        return uiAdminFormMapper.toAdminFormPreFillOptionDto(preFill);
    }

    public AdminFormListViewDto.TagDto toAdminFormTagDto(Tag tag) {
        return uiAdminFormMapper.toAdminFormTagDto(tag);
    }

    public AdminFormDetailViewDto.WorkflowOptionDto toAdminFormDetailWorkflowOptionDto(Workflow workflow) {
        return uiAdminFormMapper.toAdminFormDetailWorkflowOptionDto(workflow);
    }

    public AdminFormDetailViewDto.PreFillOptionDto toAdminFormDetailPreFillOptionDto(PreFill preFill) {
        return uiAdminFormMapper.toAdminFormDetailPreFillOptionDto(preFill);
    }

    public AdminFormDetailViewDto.TagDto toAdminFormDetailTagDto(Tag tag) {
        return uiAdminFormMapper.toAdminFormDetailTagDto(tag);
    }

    public List<SignRequestParamsFrontDto> toSignRequestParamsFrontDtos(List<SignRequestParams> signRequestParamses) {
        return uiAdminFormMapper.toSignRequestParamsFrontDtos(signRequestParamses);
    }
}
