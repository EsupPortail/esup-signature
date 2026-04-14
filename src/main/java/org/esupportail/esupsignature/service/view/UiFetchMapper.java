package org.esupportail.esupsignature.service.view;

import org.esupportail.esupsignature.dto.page.admin.AdminFormListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminFormDetailViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowUpdateViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.StartFormViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminUiStatusDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.dto.ui.global.UiCountersDto;
import org.esupportail.esupsignature.dto.ui.global.UiDataDto;
import org.esupportail.esupsignature.dto.ui.global.UiGlobalPropertiesDto;
import org.esupportail.esupsignature.dto.ui.global.UiHomeBootstrapDto;
import org.esupportail.esupsignature.dto.ui.global.UiCurrentUserDto;
import org.esupportail.esupsignature.dto.ui.global.UiUserLookupDto;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UiFetchMapper {

    public AdminUiStatusDto toAdminUiStatusDto(Integer nbSessions, Boolean dssStatus) {
        return new AdminUiStatusDto(nbSessions, dssStatus);
    }

    public UiDataDto toUiDataDto(UiDataDto.UiConfigDto config,
                                 UiCountersDto counters,
                                 UiCurrentUserDto currentUser,
                                 Map<String, String> preferences,
                                 AdminUiStatusDto adminStatus) {
        return new UiDataDto(config, counters, currentUser, preferences, adminStatus);
    }

    public UiHomeBootstrapDto toUiHomeBootstrapDto(Long startFormId,
                                                   Long startWorkflowId,
                                                   String warningReadUrl,
                                                   String searchUrl,
                                                   String searchTitlesUrl,
                                                   List<UiHomeBootstrapDto.SignBookItem> toSignSignBooks,
                                                   List<UiHomeBootstrapDto.SignBookItem> pendingSignBooks) {
        return new UiHomeBootstrapDto(startFormId, startWorkflowId, warningReadUrl, searchUrl, searchTitlesUrl, toSignSignBooks, pendingSignBooks);
    }

    public UiCurrentUserDto toUiMeDto(User user,
                                      Set<String> userRoles,
                                      User authUser,
                                      Set<String> authUserRoles,
                                      List<User> suUsers,
                                      List<Long> userImagesIds,
                                      String keystoreFileName,
                                      Map<UiParams, String> uiParams,
                                      Object securityServiceName) {
        return new UiCurrentUserDto(
                toUiUserDto(user, userRoles),
                toUiUserDto(authUser, authUserRoles),
                suUsers == null ? List.of() : suUsers.stream().map(this::toSuUserDto).toList(),
                userImagesIds == null ? List.of() : List.copyOf(userImagesIds),
                keystoreFileName,
                toUiParamsMap(uiParams),
                securityServiceName != null ? securityServiceName.toString() : null
        );
    }

    public UiCurrentUserDto.UiUserDto toUiUserDto(User user, Set<String> roles) {
        if (user == null) {
            return null;
        }
        List<String> sortedRoles = roles == null
                ? List.of()
                : roles.stream().sorted().toList();
        return new UiCurrentUserDto.UiUserDto(
                user.getId(),
                user.getEppn(),
                user.getFirstname(),
                user.getName(),
                user.getEmail(),
                user.getUserType() != null ? user.getUserType().name() : null,
                user.getDefaultSignImageNumber(),
                sortedRoles
        );
    }

    public UiCurrentUserDto.SuUserDto toSuUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new UiCurrentUserDto.SuUserDto(
                user.getEppn(),
                user.getFirstname(),
                user.getName(),
                user.getUserShareId()
        );
    }

    public UiUserLookupDto toUiUserLookupDto(User user) {
        if (user == null) {
            return null;
        }
        return new UiUserLookupDto(
                user.getEmail(),
                user.getFirstname(),
                user.getName(),
                user.getHidedPhone()
        );
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(this::toUiUserLookupDto)
                .toList();
    }

    public List<UiUserLookupDto> toUiUserLookupDtos(Set<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
                .map(this::toUiUserLookupDto)
                .toList();
    }

    public UiCountersDto toUiCountersDto(Long nbSignRequests,
                                         Long nbToSign,
                                         Long nbDeleted,
                                         Integer reportNumber,
                                         Integer managedWorkflowsSize,
                                         Boolean isRoleManager,
                                         Boolean isOneSignShare,
                                         Boolean isOneReadShare,
                                         Boolean certificatProblem) {
        return new UiCountersDto(
                nbSignRequests,
                nbToSign,
                nbDeleted,
                reportNumber,
                managedWorkflowsSize,
                isRoleManager,
                isOneSignShare,
                isOneReadShare,
                certificatProblem
        );
    }

    public UiDataDto.UiConfigDto toUiConfigDto(UiGlobalPropertiesDto globalProperties,
                                               Boolean enableSms,
                                               Boolean validationToolsEnabled,
                                               String applicationEmail,
                                               Integer maxInactiveInterval,
                                               Integer hoursBeforeRefreshNotif,
                                               Boolean infiniteScrolling,
                                               String versionApp,
                                               String profile) {
        return new UiDataDto.UiConfigDto(
                globalProperties,
                enableSms,
                validationToolsEnabled,
                applicationEmail,
                maxInactiveInterval,
                hoursBeforeRefreshNotif,
                infiniteScrolling,
                versionApp,
                profile
        );
    }


    public Map<String, String> toUiParamsMap(Map<UiParams, String> uiParams) {
        if (uiParams == null || uiParams.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> mappedUiParams = new LinkedHashMap<>();
        uiParams.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .forEach(entry -> mappedUiParams.put(entry.getKey().name(), entry.getValue()));
        return mappedUiParams;
    }

    public AdminWorkflowListViewDto toAdminWorkflowListViewDto(String workflowRole,
                                                               String displayWorkflowType,
                                                               List<Long> selectedTagIds,
                                                               List<AdminWorkflowListViewDto.TagDto> allTags,
                                                               List<String> roles,
                                                               List<AdminWorkflowListViewDto.RowDto> workflows) {
        return new AdminWorkflowListViewDto(workflowRole, displayWorkflowType, selectedTagIds, allTags, roles, workflows);
    }

    public AdminWorkflowListViewDto.RowDto toAdminWorkflowRowDto(Workflow workflow) {
        return new AdminWorkflowListViewDto.RowDto(
                workflow.getId(),
                workflow.getDescription(),
                workflow.getTags() == null ? List.of() : workflow.getTags().stream().map(this::toAdminWorkflowTagDto).toList(),
                workflow.getIsFeatured(),
                workflow.getPublicUsage(),
                workflow.getRoles() == null ? List.of() : workflow.getRoles().stream().sorted().toList(),
                workflow.getCreateBy() != null ? workflow.getCreateBy().getEppn() : null,
                workflow.getWorkflowSteps() == null ? List.of() : mapWorkflowSteps(workflow.getWorkflowSteps()),
                workflow.getDocumentsSourceUri() != null,
                workflow.getFromCode()
        );
    }

    private List<AdminWorkflowListViewDto.StepDto> mapWorkflowSteps(List<WorkflowStep> workflowSteps) {
        List<AdminWorkflowListViewDto.StepDto> steps = new java.util.ArrayList<>();
        for (int i = 0; i < workflowSteps.size(); i++) {
            steps.add(toAdminWorkflowStepDto(workflowSteps.get(i), i + 1));
        }
        return steps;
    }

    public AdminWorkflowListViewDto.StepDto toAdminWorkflowStepDto(WorkflowStep workflowStep, int index) {
        return new AdminWorkflowListViewDto.StepDto(
                index,
                workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminWorkflowUserDto).toList(),
                workflowStep.getChangeable(),
                workflowStep.getAutoSign()
        );
    }

    public AdminWorkflowListViewDto.UserDto toAdminWorkflowUserDto(User user) {
        return new AdminWorkflowListViewDto.UserDto(user.getFirstname(), user.getName());
    }

    public AdminWorkflowListViewDto.TagDto toAdminWorkflowTagDto(Tag tag) {
        return new AdminWorkflowListViewDto.TagDto(tag.getId(), tag.getName(), tag.getColor());
    }

    public AdminWorkflowUpdateViewDto toAdminWorkflowUpdateViewDto(String workflowRole,
                                                                   Workflow workflow,
                                                                   Long nbWorkflowSignRequests,
                                                                   List<String> roles,
                                                                   List<AdminWorkflowUpdateViewDto.TagDto> allTags,
                                                                   List<Long> selectedTagIds) {
        return new AdminWorkflowUpdateViewDto(
                workflowRole,
                toAdminWorkflowUpdateWorkflowDto(workflow),
                nbWorkflowSignRequests,
                roles,
                allTags,
                selectedTagIds
        );
    }

    public AdminWorkflowUpdateViewDto.WorkflowDto toAdminWorkflowUpdateWorkflowDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.WorkflowDto(
                workflow.getId(),
                workflow.getDescription(),
                workflow.getToken(),
                workflow.getMailFrom(),
                workflow.getNamingTemplate(),
                workflow.getIsFeatured(),
                workflow.getPublicUsage(),
                workflow.getRoles() == null ? List.of() : workflow.getRoles().stream().sorted().toList(),
                workflow.getManagers() == null ? List.of() : workflow.getManagers().stream().sorted().toList(),
                workflow.getDashboardRoles() == null ? List.of() : workflow.getDashboardRoles().stream().sorted().toList(),
                workflow.getViewers() == null ? List.of() : workflow.getViewers().stream().map(this::toAdminWorkflowViewerDto).toList(),
                workflow.getExternalAuths() == null ? List.of() : workflow.getExternalAuths().stream().sorted(Comparator.comparing(Enum::name)).toList(),
                workflow.getAuthorizedShareTypes() == null ? List.of() : workflow.getAuthorizedShareTypes().stream().sorted(Comparator.comparing(Enum::name)).toList(),
                workflow.getSealAtEnd(),
                workflow.getSendAlertToAllRecipients(),
                workflow.getOwnerSystem(),
                workflow.getDisableDeleteByCreator(),
                workflow.getDisableEmailAlerts(),
                workflow.getForbidDownloadsBeforeEnd(),
                workflow.getAuthorizeClone(),
                workflow.getExternalCanReaderAnnotations(),
                workflow.getExternalCanEdit(),
                workflow.getExternalCanReaderAttachments(),
                workflow.getExternalCanEditAttachments(),
                workflow.getDisableSidebarForExternal(),
                workflow.getSignRequestParamsDetectionPattern(),
                workflow.getScanPdfMetadatas(),
                workflow.getDocumentsSourceUri(),
                workflow.getTargetNamingTemplate(),
                workflow.getStartArchiveDate(),
                workflow.getArchiveTarget(),
                workflow.getTags() == null ? List.of() : workflow.getTags().stream().map(this::toAdminWorkflowUpdateTagDto).toList(),
                workflow.getTargets() == null ? List.of() : workflow.getTargets().stream()
                        .sorted(Comparator.comparing(Target::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toAdminWorkflowTargetDto)
                        .toList(),
                workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toAdminWorkflowStepDetailDto).toList(),
                workflow.getFromCode(),
                workflow.getMessage()
        );
    }

    public AdminWorkflowUpdateViewDto.TargetDto toAdminWorkflowTargetDto(Target target) {
        if (target == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.TargetDto(
                target.getId(),
                target.getProtectedTargetUri(),
                target.getSendDocument(),
                target.getSendReport(),
                target.getSendAttachment(),
                target.getSendZip()
        );
    }

    public AdminWorkflowUpdateViewDto.WorkflowStepDto toAdminWorkflowStepDetailDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.WorkflowStepDto(
                workflowStep.getId(),
                workflowStep.getDescription(),
                workflowStep.getAutoSign(),
                workflowStep.getSignType(),
                workflowStep.getMinSignLevel(),
                workflowStep.getMaxSignLevel(),
                workflowStep.getSealVisa(),
                workflowStep.getMaxRecipients(),
                workflowStep.getChangeable(),
                workflowStep.getRepeatable(),
                workflowStep.getMultiSign(),
                workflowStep.getSingleSignWithAnnotation(),
                workflowStep.getAllSignToComplete(),
                workflowStep.getAttachmentAlert(),
                workflowStep.getAttachmentRequire(),
                workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminWorkflowStepUserDto).toList(),
                toAdminWorkflowStepCertificatDto(workflowStep.getCertificat())
        );
    }

    public AdminWorkflowUpdateViewDto.CertificatDto toAdminWorkflowStepCertificatDto(Certificat certificat) {
        if (certificat == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.CertificatDto(certificat.getId());
    }

    public AdminWorkflowUpdateViewDto.UserDto toAdminWorkflowStepUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.UserDto(
                user.getEppn(),
                user.getEmail(),
                user.getFirstname(),
                user.getName()
        );
    }

    public AdminWorkflowUpdateViewDto.ViewerDto toAdminWorkflowViewerDto(User user) {
        if (user == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.ViewerDto(user.getEmail(), user.getFirstname(), user.getName());
    }

    public WorkflowViewDto toWorkflowViewDto(Workflow workflow, String messageToDisplay) {
        if (workflow == null) {
            return null;
        }
        return new WorkflowViewDto(
                workflow.getId(),
                workflow.getDescription(),
                workflow.getDocumentsSourceUri(),
                workflow.getSendAlertToAllRecipients(),
                workflow.getFromCode(),
                messageToDisplay,
                workflow.getTargets() == null ? List.of() : workflow.getTargets().stream().map(this::toWorkflowTargetViewDto).toList(),
                workflow.getViewers() == null ? List.of() : workflow.getViewers().stream().map(this::toWorkflowViewerDto).toList(),
                workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toWorkflowStepViewDto).toList()
        );
    }

    public StartFormViewDto toStartFormViewDto(Form form, String messageToDisplay) {
        if (form == null) {
            return null;
        }
        return new StartFormViewDto(
                form.getId(),
                form.getTitle(),
                messageToDisplay,
                toWorkflowViewDto(form.getWorkflow(), null)
        );
    }

    public WorkflowViewDto.TargetDto toWorkflowTargetViewDto(Target target) {
        if (target == null) {
            return null;
        }
        return new WorkflowViewDto.TargetDto(target.getId(), target.getTargetUri());
    }

    public WorkflowViewDto.ViewerDto toWorkflowViewerDto(User user) {
        if (user == null) {
            return null;
        }
        return new WorkflowViewDto.ViewerDto(user.getEmail(), user.getFirstname(), user.getName());
    }

    public WorkflowViewDto.WorkflowStepDto toWorkflowStepViewDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        return new WorkflowViewDto.WorkflowStepDto(
                workflowStep.getId(),
                workflowStep.getDescription(),
                workflowStep.getAutoSign(),
                workflowStep.getSignType(),
                workflowStep.getMinSignLevel(),
                workflowStep.getMaxSignLevel(),
                workflowStep.getSealVisa(),
                workflowStep.getMaxRecipients(),
                workflowStep.getChangeable(),
                workflowStep.getRepeatable(),
                workflowStep.getMultiSign(),
                workflowStep.getSingleSignWithAnnotation(),
                workflowStep.getAllSignToComplete(),
                workflowStep.getAttachmentAlert(),
                workflowStep.getAttachmentRequire(),
                workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toWorkflowStepUserViewDto).toList(),
                toWorkflowStepCertificatViewDto(workflowStep.getCertificat())
        );
    }

    public WorkflowViewDto.UserDto toWorkflowStepUserViewDto(User user) {
        if (user == null) {
            return null;
        }
        return new WorkflowViewDto.UserDto(
                user.getEppn(),
                user.getEmail(),
                user.getFirstname(),
                user.getName(),
                user.getHidedPhone(),
                user.getUserType(),
                toWorkflowReplacementUserViewDto(user.getCurrentReplaceByUser())
        );
    }

    public WorkflowViewDto.UserDto toWorkflowReplacementUserViewDto(User user) {
        if (user == null) {
            return null;
        }
        return new WorkflowViewDto.UserDto(
                user.getEppn(),
                user.getEmail(),
                user.getFirstname(),
                user.getName(),
                user.getHidedPhone(),
                user.getUserType(),
                null
        );
    }

    public WorkflowViewDto.CertificatDto toWorkflowStepCertificatViewDto(Certificat certificat) {
        if (certificat == null) {
            return null;
        }
        return new WorkflowViewDto.CertificatDto(certificat.getId());
    }

    public AdminWorkflowUpdateViewDto.TagDto toAdminWorkflowUpdateTagDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new AdminWorkflowUpdateViewDto.TagDto(tag.getId(), tag.getName(), tag.getColor());
    }

    public AdminFormListViewDto toAdminFormListViewDto(String workflowRole,
                                                       Boolean activeVersion,
                                                       List<Long> selectedTagIds,
                                                       List<AdminFormListViewDto.TagDto> allTags,
                                                       List<String> roles,
                                                       List<AdminFormListViewDto.WorkflowOptionDto> workflowTypes,
                                                       List<AdminFormListViewDto.PreFillOptionDto> preFillTypes,
                                                       List<AdminFormListViewDto.RowDto> forms) {
        return new AdminFormListViewDto(workflowRole, activeVersion, selectedTagIds, allTags, roles, workflowTypes, preFillTypes, forms);
    }

    public AdminFormListViewDto.RowDto toAdminFormRowDto(Form form) {
        return new AdminFormListViewDto.RowDto(
                form.getId(),
                form.getName(),
                form.getTitle(),
                form.getTags() == null ? List.of() : form.getTags().stream().map(this::toAdminFormTagDto).toList(),
                form.getIsFeatured(),
                form.getWorkflow() != null ? toAdminFormWorkflowOptionDto(form.getWorkflow()) : null,
                form.getActiveVersion(),
                form.getHideButton(),
                form.getDeleted(),
                form.getPublicUsage(),
                form.getRoles() == null ? List.of() : form.getRoles().stream().sorted().toList()
        );
    }

    public AdminFormListViewDto.WorkflowOptionDto toAdminFormWorkflowOptionDto(Workflow workflow) {
        return new AdminFormListViewDto.WorkflowOptionDto(workflow.getId(), workflow.getDescription());
    }

    public AdminFormListViewDto.PreFillOptionDto toAdminFormPreFillOptionDto(PreFill preFill) {
        return new AdminFormListViewDto.PreFillOptionDto(preFill.getName(), preFill.getDescription());
    }

    public AdminFormListViewDto.TagDto toAdminFormTagDto(Tag tag) {
        return new AdminFormListViewDto.TagDto(tag.getId(), tag.getName(), tag.getColor());
    }

    public AdminFormDetailViewDto toAdminFormDetailViewDto(String workflowRole,
                                                           Form form,
                                                           List<String> roles,
                                                           List<AdminFormDetailViewDto.WorkflowOptionDto> workflowTypes,
                                                           List<AdminFormDetailViewDto.PreFillOptionDto> preFillTypes,
                                                           List<AdminFormDetailViewDto.TagDto> allTags,
                                                           List<Long> selectedTagIds,
                                                           Map<String, List<String>> preFillTypeOptions,
                                                           List<SignRequestParamsFrontDto> spots,
                                                           Map<Integer, Long> srpMap,
                                                           Integer defaultSignImageNumber) {
        return new AdminFormDetailViewDto(
                workflowRole,
                toAdminFormDto(form),
                form != null ? toAdminFormWorkflowSummaryDto(form.getWorkflow()) : null,
                roles,
                workflowTypes,
                preFillTypes,
                allTags,
                selectedTagIds,
                form != null ? toAdminFormDocumentDto(form.getDocument()) : null,
                preFillTypeOptions,
                spots,
                srpMap,
                defaultSignImageNumber
        );
    }

    public AdminFormDetailViewDto.FormDto toAdminFormDto(Form form) {
        if (form == null) {
            return null;
        }
        return new AdminFormDetailViewDto.FormDto(
                form.getId(),
                form.getTitle(),
                form.getName(),
                form.getDescription(),
                form.getIsFeatured(),
                form.getActiveVersion(),
                form.getAuthorizedShareTypes() == null ? List.of() : form.getAuthorizedShareTypes().stream().sorted(Comparator.comparing(Enum::name)).toList(),
                form.getPublicUsage(),
                form.getRoles() == null ? List.of() : form.getRoles().stream().sorted().toList(),
                form.getHideButton(),
                form.getPreFillType(),
                form.getPdfDisplay(),
                form.getAction(),
                form.getMessage(),
                toAdminFormWorkflowSummaryDto(form.getWorkflow()),
                toAdminFormDocumentDto(form.getDocument()),
                form.getFields() == null ? List.of() : form.getFields().stream().map(field -> toAdminFormFieldDto(field, form.getWorkflow())).toList(),
                form.getWorkflow() == null ? List.of() : toSignRequestParamsFrontDtos(form.getSignRequestParams()),
                form.getDeleted()
        );
    }

    public AdminFormDetailViewDto.WorkflowSummaryDto toAdminFormWorkflowSummaryDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        return new AdminFormDetailViewDto.WorkflowSummaryDto(
                workflow.getId(),
                workflow.getDescription(),
                workflow.getMailFrom(),
                workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toAdminFormWorkflowStepDto).toList()
        );
    }

    public AdminFormDetailViewDto.WorkflowStepDto toAdminFormWorkflowStepDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        return new AdminFormDetailViewDto.WorkflowStepDto(
                workflowStep.getId(),
                workflowStep.getSignType(),
                workflowStep.getAllSignToComplete(),
                workflowStep.getName(),
                workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminFormUserDto).toList()
        );
    }

    public AdminFormDetailViewDto.UserDto toAdminFormUserDto(User user) {
        if (user == null) {
            return null;
        }
        return new AdminFormDetailViewDto.UserDto(user.getFirstname(), user.getName());
    }

    public AdminFormDetailViewDto.WorkflowOptionDto toAdminFormDetailWorkflowOptionDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        return new AdminFormDetailViewDto.WorkflowOptionDto(workflow.getId(), workflow.getDescription());
    }

    public AdminFormDetailViewDto.PreFillOptionDto toAdminFormDetailPreFillOptionDto(PreFill preFill) {
        if (preFill == null) {
            return null;
        }
        return new AdminFormDetailViewDto.PreFillOptionDto(preFill.getName(), preFill.getDescription());
    }

    public AdminFormDetailViewDto.TagDto toAdminFormDetailTagDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new AdminFormDetailViewDto.TagDto(tag.getId(), tag.getName(), tag.getColor());
    }

    public AdminFormDetailViewDto.DocumentDto toAdminFormDocumentDto(Document document) {
        if (document == null || document.getId() == null) {
            return null;
        }
        return new AdminFormDetailViewDto.DocumentDto(document.getFileName(), document.getContentType());
    }

    public AdminFormDetailViewDto.FieldDto toAdminFormFieldDto(Field field, Workflow workflow) {
        if (field == null) {
            return null;
        }
        return new AdminFormDetailViewDto.FieldDto(
                field.getId(),
                field.getName(),
                field.getDescription(),
                field.getType(),
                field.getFavorisable(),
                field.getRequired(),
                field.getReadOnly(),
                field.getExtValueServiceName(),
                field.getSearchServiceName(),
                field.getExtValueType(),
                field.getSearchType(),
                field.getExtValueReturn(),
                field.getSearchReturn(),
                toAdminFormWorkflowStepIds(field, workflow)
        );
    }

    private List<Long> toAdminFormWorkflowStepIds(Field field, Workflow workflow) {
        if (field == null || workflow == null || field.getWorkflowSteps() == null) {
            return List.of();
        }
        Set<Long> workflowStepIds = workflow.getWorkflowSteps() == null
                ? Set.of()
                : workflow.getWorkflowSteps().stream().map(WorkflowStep::getId).collect(Collectors.toSet());
        return field.getWorkflowSteps().stream()
                .map(WorkflowStep::getId)
                .filter(workflowStepIds::contains)
                .toList();
    }

    public List<SignRequestParamsFrontDto> toSignRequestParamsFrontDtos(List<SignRequestParams> signRequestParamses) {
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
}


