package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.admin.AdminFormDetailViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminFormListViewDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFill;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UiAdminFormMapper {

    public AdminFormListViewDto toAdminFormListViewDto(String workflowRole,
                                                       Boolean activeVersion,
                                                       List<Long> selectedTagIds,
                                                       List<AdminFormListViewDto.TagDto> allTags,
                                                       List<String> roles,
                                                       List<AdminFormListViewDto.WorkflowOptionDto> workflowTypes,
                                                       List<AdminFormListViewDto.PreFillOptionDto> preFillTypes,
                                                       List<AdminFormListViewDto.RowDto> forms) {
        AdminFormListViewDto dto = new AdminFormListViewDto();
        dto.setWorkflowRole(workflowRole);
        dto.setActiveVersion(activeVersion);
        dto.setSelectedTagIds(selectedTagIds);
        dto.setAllTags(allTags);
        dto.setRoles(roles);
        dto.setWorkflowTypes(workflowTypes);
        dto.setPreFillTypes(preFillTypes);
        dto.setForms(forms);
        return dto;
    }

    public AdminFormListViewDto.RowDto toAdminFormRowDto(Form form) {
        AdminFormListViewDto.RowDto dto = new AdminFormListViewDto.RowDto();
        dto.setId(form.getId());
        dto.setName(form.getName());
        dto.setTitle(form.getTitle());
        dto.setTags(form.getTags() == null ? List.of() : form.getTags().stream().map(this::toAdminFormTagDto).toList());
        dto.setFeatured(Boolean.TRUE.equals(form.getIsFeatured()));
        dto.setWorkflow(form.getWorkflow() != null ? toAdminFormWorkflowOptionDto(form.getWorkflow()) : null);
        dto.setActiveVersion(Boolean.TRUE.equals(form.getActiveVersion()));
        dto.setHideButton(Boolean.TRUE.equals(form.getHideButton()));
        dto.setDeleted(Boolean.TRUE.equals(form.getDeleted()));
        dto.setPublicUsage(Boolean.TRUE.equals(form.getPublicUsage()));
        dto.setRoles(form.getRoles() == null ? List.of() : form.getRoles().stream().sorted().toList());
        return dto;
    }

    public AdminFormListViewDto.WorkflowOptionDto toAdminFormWorkflowOptionDto(Workflow workflow) {
        AdminFormListViewDto.WorkflowOptionDto dto = new AdminFormListViewDto.WorkflowOptionDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        return dto;
    }

    public AdminFormListViewDto.PreFillOptionDto toAdminFormPreFillOptionDto(PreFill preFill) {
        AdminFormListViewDto.PreFillOptionDto dto = new AdminFormListViewDto.PreFillOptionDto();
        dto.setName(preFill.getName());
        dto.setDescription(preFill.getDescription());
        return dto;
    }

    public AdminFormListViewDto.TagDto toAdminFormTagDto(Tag tag) {
        AdminFormListViewDto.TagDto dto = new AdminFormListViewDto.TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
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
        AdminFormDetailViewDto dto = new AdminFormDetailViewDto();
        dto.setWorkflowRole(workflowRole);
        dto.setForm(toAdminFormDto(form));
        dto.setWorkflow(form != null ? toAdminFormWorkflowSummaryDto(form.getWorkflow()) : null);
        dto.setRoles(roles);
        dto.setWorkflowTypes(workflowTypes);
        dto.setPreFillTypes(preFillTypes);
        dto.setAllTags(allTags);
        dto.setSelectedTagIds(selectedTagIds);
        dto.setDocument(form != null ? toAdminFormDocumentDto(form.getDocument()) : null);
        dto.setPreFillTypeOptions(preFillTypeOptions);
        dto.setSpots(spots);
        dto.setSrpMap(srpMap);
        dto.setDefaultSignImageNumber(defaultSignImageNumber);
        return dto;
    }

    public AdminFormDetailViewDto.FormDto toAdminFormDto(Form form) {
        if (form == null) {
            return null;
        }
        AdminFormDetailViewDto.FormDto dto = new AdminFormDetailViewDto.FormDto();
        dto.setId(form.getId());
        dto.setTitle(form.getTitle());
        dto.setName(form.getName());
        dto.setDescription(form.getDescription());
        dto.setIsFeatured(form.getIsFeatured());
        dto.setActiveVersion(form.getActiveVersion());
        dto.setAuthorizedShareTypes(form.getAuthorizedShareTypes() == null ? List.of() : form.getAuthorizedShareTypes().stream().sorted(Comparator.comparing(Enum::name)).toList());
        dto.setPublicUsage(form.getPublicUsage());
        dto.setRoles(form.getRoles() == null ? List.of() : form.getRoles().stream().sorted().toList());
        dto.setHideButton(form.getHideButton());
        dto.setPreFillType(form.getPreFillType());
        dto.setPdfDisplay(form.getPdfDisplay());
        dto.setAction(form.getAction());
        dto.setMessage(form.getMessage());
        dto.setWorkflow(toAdminFormWorkflowSummaryDto(form.getWorkflow()));
        dto.setDocument(toAdminFormDocumentDto(form.getDocument()));
        dto.setFields(form.getFields() == null ? List.of() : form.getFields().stream().map(field -> toAdminFormFieldDto(field, form.getWorkflow())).toList());
        dto.setSignRequestParams(form.getWorkflow() == null ? List.of() : toSignRequestParamsFrontDtos(form.getSignRequestParams()));
        dto.setDeleted(form.getDeleted());
        return dto;
    }

    public AdminFormDetailViewDto.WorkflowSummaryDto toAdminFormWorkflowSummaryDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        AdminFormDetailViewDto.WorkflowSummaryDto dto = new AdminFormDetailViewDto.WorkflowSummaryDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        dto.setMailFrom(workflow.getMailFrom());
        dto.setWorkflowSteps(workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toAdminFormWorkflowStepDto).toList());
        return dto;
    }

    public AdminFormDetailViewDto.WorkflowStepDto toAdminFormWorkflowStepDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        AdminFormDetailViewDto.WorkflowStepDto dto = new AdminFormDetailViewDto.WorkflowStepDto();
        dto.setId(workflowStep.getId());
        dto.setSignType(workflowStep.getSignType());
        dto.setAllSignToComplete(workflowStep.getAllSignToComplete());
        dto.setName(workflowStep.getName());
        dto.setUsers(workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toAdminFormUserDto).toList());
        return dto;
    }

    public AdminFormDetailViewDto.UserDto toAdminFormUserDto(User user) {
        if (user == null) {
            return null;
        }
        AdminFormDetailViewDto.UserDto dto = new AdminFormDetailViewDto.UserDto();
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    public AdminFormDetailViewDto.WorkflowOptionDto toAdminFormDetailWorkflowOptionDto(Workflow workflow) {
        if (workflow == null) {
            return null;
        }
        AdminFormDetailViewDto.WorkflowOptionDto dto = new AdminFormDetailViewDto.WorkflowOptionDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        return dto;
    }

    public AdminFormDetailViewDto.PreFillOptionDto toAdminFormDetailPreFillOptionDto(PreFill preFill) {
        if (preFill == null) {
            return null;
        }
        AdminFormDetailViewDto.PreFillOptionDto dto = new AdminFormDetailViewDto.PreFillOptionDto();
        dto.setName(preFill.getName());
        dto.setDescription(preFill.getDescription());
        return dto;
    }

    public AdminFormDetailViewDto.TagDto toAdminFormDetailTagDto(Tag tag) {
        if (tag == null) {
            return null;
        }
        AdminFormDetailViewDto.TagDto dto = new AdminFormDetailViewDto.TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColor(tag.getColor());
        return dto;
    }

    public AdminFormDetailViewDto.DocumentDto toAdminFormDocumentDto(Document document) {
        if (document == null || document.getId() == null) {
            return null;
        }
        AdminFormDetailViewDto.DocumentDto dto = new AdminFormDetailViewDto.DocumentDto();
        dto.setFileName(document.getFileName());
        dto.setContentType(document.getContentType());
        return dto;
    }

    public AdminFormDetailViewDto.FieldDto toAdminFormFieldDto(Field field, Workflow workflow) {
        if (field == null) {
            return null;
        }
        AdminFormDetailViewDto.FieldDto dto = new AdminFormDetailViewDto.FieldDto();
        dto.setId(field.getId());
        dto.setName(field.getName());
        dto.setDescription(field.getDescription());
        dto.setType(field.getType());
        dto.setFavorisable(field.getFavorisable());
        dto.setRequired(field.getRequired());
        dto.setReadOnly(field.getReadOnly());
        dto.setExtValueServiceName(field.getExtValueServiceName());
        dto.setSearchServiceName(field.getSearchServiceName());
        dto.setExtValueType(field.getExtValueType());
        dto.setSearchType(field.getSearchType());
        dto.setExtValueReturn(field.getExtValueReturn());
        dto.setSearchReturn(field.getSearchReturn());
        dto.setWorkflowStepIds(toAdminFormWorkflowStepIds(field, workflow));
        return dto;
    }

    public List<SignRequestParamsFrontDto> toSignRequestParamsFrontDtos(List<SignRequestParams> signRequestParamses) {
        if (signRequestParamses == null || signRequestParamses.isEmpty()) {
            return List.of();
        }
        return signRequestParamses.stream()
                .map(signRequestParams -> {
                    SignRequestParamsFrontDto dto = new SignRequestParamsFrontDto();
                    dto.setId(signRequestParams.getId());
                    dto.setPdSignatureFieldName(signRequestParams.getPdSignatureFieldName());
                    dto.setStepNumber(signRequestParams.getStepNumber());
                    dto.setSignImageNumber(signRequestParams.getSignImageNumber());
                    dto.setSignPageNumber(signRequestParams.getSignPageNumber());
                    dto.setSignDocumentNumber(signRequestParams.getSignDocumentNumber());
                    dto.setSignWidth(signRequestParams.getSignWidth());
                    dto.setSignHeight(signRequestParams.getSignHeight());
                    dto.setXPos(signRequestParams.getxPos());
                    dto.setYPos(signRequestParams.getyPos());
                    dto.setExtraText(signRequestParams.getExtraText());
                    dto.setIsExtraText(signRequestParams.getIsExtraText());
                    dto.setAddWatermark(signRequestParams.getAddWatermark());
                    dto.setAllPages(signRequestParams.getAllPages());
                    dto.setAddImage(signRequestParams.getAddImage());
                    dto.setAddExtra(signRequestParams.getAddExtra());
                    dto.setExtraType(signRequestParams.getExtraType());
                    dto.setExtraName(signRequestParams.getExtraName());
                    dto.setExtraDate(signRequestParams.getExtraDate());
                    dto.setExtraOnTop(signRequestParams.getExtraOnTop());
                    dto.setTextPart(signRequestParams.getTextPart());
                    dto.setSignScale(signRequestParams.getSignScale());
                    dto.setRed(signRequestParams.getRed());
                    dto.setGreen(signRequestParams.getGreen());
                    dto.setBlue(signRequestParams.getBlue());
                    dto.setFontSize(signRequestParams.getFontSize());
                    dto.setRecipientId(signRequestParams.getRecipient() != null ? signRequestParams.getRecipient().getId() : null);
                    return dto;
                })
                .toList();
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
}

