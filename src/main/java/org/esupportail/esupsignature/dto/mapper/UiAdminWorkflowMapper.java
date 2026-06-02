package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowListViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowUpdateViewDto;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class UiAdminWorkflowMapper {

    public AdminWorkflowListViewDto toAdminWorkflowListViewDto(String workflowRole, String displayWorkflowType, List<Long> selectedTagIds, List<AdminWorkflowListViewDto.TagDto> allTags, List<String> roles, List<AdminWorkflowListViewDto.RowDto> workflows) {
        AdminWorkflowListViewDto dto = new AdminWorkflowListViewDto();
        dto.setWorkflowRole(workflowRole);
        dto.setDisplayWorkflowType(displayWorkflowType);
        dto.setSelectedTagIds(selectedTagIds);
        dto.setAllTags(allTags);
        dto.setRoles(roles);
        dto.setWorkflows(workflows);
        return dto;
    }

    public AdminWorkflowListViewDto.RowDto toAdminWorkflowRowDto(Workflow workflow) {
        AdminWorkflowListViewDto.RowDto dto = new AdminWorkflowListViewDto.RowDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        dto.setTags(workflow.getTags() == null ? List.of() : workflow.getTags().stream().map(this::toAdminWorkflowTagDto).toList());
        dto.setFeatured(Boolean.TRUE.equals(workflow.getIsFeatured()));
        dto.setPublicUsage(Boolean.TRUE.equals(workflow.getPublicUsage()));
        dto.setRoles(workflow.getRoles() == null ? List.of() : workflow.getRoles().stream().sorted().toList());
        dto.setCreateByEppn(workflow.getCreateBy() != null ? workflow.getCreateBy().getEppn() : null);
        dto.setWorkflowSteps(workflow.getWorkflowSteps() == null ? List.of() : mapWorkflowSteps(workflow.getWorkflowSteps()));
        dto.setDocumentsSourceUriPresent(workflow.getDocumentsSourceUri() != null);
        dto.setFromCode(Boolean.TRUE.equals(workflow.getFromCode()));
        return dto;
    }

    public AdminWorkflowListViewDto.StepDto toAdminWorkflowStepDto(WorkflowStep workflowStep, int index) {
        AdminWorkflowListViewDto.StepDto dto = new AdminWorkflowListViewDto.StepDto();
        dto.setIndex(index);
        dto.setUsers(workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminWorkflowUserDto).toList());
        dto.setChangeable(Boolean.TRUE.equals(workflowStep.getChangeable()));
        dto.setAutoSign(Boolean.TRUE.equals(workflowStep.getAutoSign()));
        return dto;
    }

    public AdminWorkflowListViewDto.UserDto toAdminWorkflowUserDto(User user) {
        AdminWorkflowListViewDto.UserDto dto = new AdminWorkflowListViewDto.UserDto();
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    public AdminWorkflowListViewDto.TagDto toAdminWorkflowTagDto(Tag tag) {
        AdminWorkflowListViewDto.TagDto dto = new AdminWorkflowListViewDto.TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
    }

    public AdminWorkflowUpdateViewDto toAdminWorkflowUpdateViewDto(String workflowRole, Workflow workflow, Long nbWorkflowSignRequests, List<String> roles, List<AdminWorkflowUpdateViewDto.TagDto> allTags, List<Long> selectedTagIds) {
        AdminWorkflowUpdateViewDto dto = new AdminWorkflowUpdateViewDto();
        dto.setWorkflowRole(workflowRole);
        dto.setWorkflow(toAdminWorkflowUpdateWorkflowDto(workflow));
        dto.setNbWorkflowSignRequests(nbWorkflowSignRequests);
        dto.setRoles(roles);
        dto.setAllTags(allTags);
        dto.setSelectedTagIds(selectedTagIds);
        return dto;
    }

    public AdminWorkflowUpdateViewDto.WorkflowDto toAdminWorkflowUpdateWorkflowDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.WorkflowDto dto = new AdminWorkflowUpdateViewDto.WorkflowDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        dto.setToken(workflow.getToken());
        dto.setMailFrom(workflow.getMailFrom());
        dto.setNamingTemplate(workflow.getNamingTemplate());
        dto.setIsFeatured(workflow.getIsFeatured());
        dto.setPublicUsage(workflow.getPublicUsage());
        dto.setRoles(workflow.getRoles() == null ? List.of() : workflow.getRoles().stream().sorted().toList());
        dto.setManagers(workflow.getManagers() == null ? List.of() : workflow.getManagers().stream().sorted().toList());
        dto.setDashboardRoles(workflow.getDashboardRoles() == null ? List.of() : workflow.getDashboardRoles().stream().sorted().toList());
        dto.setViewers(workflow.getViewers() == null ? List.of() : workflow.getViewers().stream().map(this::toAdminWorkflowViewerDto).toList());
        dto.setExternalAuths(workflow.getExternalAuths() == null ? List.of() : workflow.getExternalAuths().stream().sorted(Comparator.comparing(Enum::name)).toList());
        dto.setAuthorizedShareTypes(workflow.getAuthorizedShareTypes() == null ? List.of() : workflow.getAuthorizedShareTypes().stream().sorted(Comparator.comparing(Enum::name)).toList());
        dto.setSealAtEnd(workflow.getSealAtEnd());
        dto.setSendAlertToAllRecipients(workflow.getSendAlertToAllRecipients());
        dto.setOwnerSystem(workflow.getOwnerSystem());
        dto.setDisableDeleteByCreator(workflow.getDisableDeleteByCreator());
        dto.setDisableUpdateByCreator(workflow.getDisableUpdateByCreator());
        dto.setDisableEmailAlerts(workflow.getDisableEmailAlerts());
        dto.setForbidDownloadsBeforeEnd(workflow.getForbidDownloadsBeforeEnd());
        dto.setAuthorizeClone(workflow.getAuthorizeClone());
        dto.setExternalCanReaderAnnotations(workflow.getExternalCanReaderAnnotations());
        dto.setExternalCanEdit(workflow.getExternalCanEdit());
        dto.setExternalCanReaderAttachments(workflow.getExternalCanReaderAttachments());
        dto.setExternalCanEditAttachments(workflow.getExternalCanEditAttachments());
        dto.setDisableSidebarForExternal(workflow.getDisableSidebarForExternal());
        dto.setSignRequestParamsDetectionPattern(workflow.getSignRequestParamsDetectionPattern());
        dto.setScanPdfMetadatas(workflow.getScanPdfMetadatas());
        dto.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        dto.setUnzip(workflow.getUnzip());
        dto.setTargetNamingTemplate(workflow.getTargetNamingTemplate());
        dto.setStartArchiveDate(workflow.getStartArchiveDate());
        dto.setArchiveTarget(workflow.getArchiveTarget());
        dto.setTags(workflow.getTags() == null ? List.of() : workflow.getTags().stream().map(this::toAdminWorkflowUpdateTagDto).toList());
        dto.setTargetsOrdered(workflow.getTargets() == null ? List.of() : workflow.getTargets().stream()
                .sorted(Comparator.comparing(Target::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAdminWorkflowTargetDto)
                .toList());
        dto.setWorkflowSteps(workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toAdminWorkflowStepDetailDto).toList());
        dto.setFromCode(workflow.getFromCode());
        dto.setMessage(workflow.getMessage());
        return dto;
    }

    public AdminWorkflowUpdateViewDto.TargetDto toAdminWorkflowTargetDto(Target target) {
        if (target == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.TargetDto dto = new AdminWorkflowUpdateViewDto.TargetDto();
        dto.setId(target.getId());
        dto.setProtectedTargetUri(target.getProtectedTargetUri());
        dto.setSendDocument(target.getSendDocument());
        dto.setSendReport(target.getSendReport());
        dto.setSendAttachment(target.getSendAttachment());
        dto.setSendZip(target.getSendZip());
        return dto;
    }

    public AdminWorkflowUpdateViewDto.WorkflowStepDto toAdminWorkflowStepDetailDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.WorkflowStepDto dto = new AdminWorkflowUpdateViewDto.WorkflowStepDto();
        dto.setId(workflowStep.getId());
        dto.setDescription(workflowStep.getDescription());
        dto.setAutoSign(workflowStep.getAutoSign());
        dto.setSealCertificatName(workflowStep.getSealCertificatName());
        dto.setSignType(workflowStep.getSignType());
        dto.setMinSignLevel(workflowStep.getMinSignLevel());
        dto.setMaxSignLevel(workflowStep.getMaxSignLevel());
        dto.setSealVisa(workflowStep.getSealVisa());
        dto.setMaxRecipients(workflowStep.getMaxRecipients());
        dto.setChangeable(workflowStep.getChangeable());
        dto.setRepeatable(workflowStep.getRepeatable());
        dto.setMultiSign(workflowStep.getMultiSign());
        dto.setSingleSignWithAnnotation(workflowStep.getSingleSignWithAnnotation());
        dto.setAllSignToComplete(workflowStep.getAllSignToComplete());
        dto.setAttachmentAlert(workflowStep.getAttachmentAlert());
        dto.setAttachmentRequire(workflowStep.getAttachmentRequire());
        dto.setUsers(workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminWorkflowStepUserDto).toList());
        dto.setCertificat(toAdminWorkflowStepCertificatDto(workflowStep.getCertificat()));
        return dto;
    }

    public AdminWorkflowUpdateViewDto.CertificatDto toAdminWorkflowStepCertificatDto(Certificat certificat) {
        if (certificat == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.CertificatDto dto = new AdminWorkflowUpdateViewDto.CertificatDto();
        dto.setId(certificat.getId());
        return dto;
    }

    public AdminWorkflowUpdateViewDto.UserDto toAdminWorkflowStepUserDto(User user) {
        if (user == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.UserDto dto = new AdminWorkflowUpdateViewDto.UserDto();
        dto.setEppn(user.getEppn());
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    public AdminWorkflowUpdateViewDto.ViewerDto toAdminWorkflowViewerDto(User user) {
        if (user == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.ViewerDto dto = new AdminWorkflowUpdateViewDto.ViewerDto();
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    public AdminWorkflowUpdateViewDto.TagDto toAdminWorkflowUpdateTagDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        AdminWorkflowUpdateViewDto.TagDto dto = new AdminWorkflowUpdateViewDto.TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
    }

    private List<AdminWorkflowListViewDto.StepDto> mapWorkflowSteps(List<WorkflowStep> workflowSteps) {
        List<AdminWorkflowListViewDto.StepDto> steps = new java.util.ArrayList<>();
        for (int i = 0; i < workflowSteps.size(); i++) {
            steps.add(toAdminWorkflowStepDto(workflowSteps.get(i), i + 1));
        }
        return steps;
    }
}

