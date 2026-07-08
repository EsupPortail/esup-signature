package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.admin.AdminSignRequestShowViewDto;
import org.esupportail.esupsignature.dto.projection.jpa.AttachmentProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.AdminCommentProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.AdminLogProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.DocumentProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepRecipientProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowTargetProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookViewerProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignRequestLightProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignRequestTabProjectionDto;
import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.CommentFrontDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.FieldFrontDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestContextDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestFullDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Log;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class UiFetchSignRequestMapper {

    public AdminSignRequestShowViewDto.CommentDto toAdminCommentDto(AdminCommentProjectionDto comment) {
        AdminSignRequestShowViewDto.CommentDto dto = new AdminSignRequestShowViewDto.CommentDto();
        dto.setId(comment.getId());
        dto.setCreateDate(comment.getCreateDate());
        dto.setText(comment.getText());
        AdminSignRequestShowViewDto.UserDto createBy = new AdminSignRequestShowViewDto.UserDto();
        createBy.setId(comment.getCreateById());
        createBy.setEppn(comment.getCreateByEppn());
        createBy.setFirstname(comment.getCreateByFirstname());
        createBy.setName(comment.getCreateByName());
        createBy.setEmail(comment.getCreateByEmail());
        createBy.setPhone(comment.getCreateByPhone());
        createBy.setUserType(comment.getCreateByUserType() != null ? comment.getCreateByUserType().name() : null);
        dto.setCreateBy(createBy);
        return dto;
    }

    public AdminSignRequestShowViewDto.LogDto toAdminLogDto(AdminLogProjectionDto log) {
        AdminSignRequestShowViewDto.LogDto dto = new AdminSignRequestShowViewDto.LogDto();
        dto.setLogDate(log.getLogDate());
        dto.setEppn(log.getEppn());
        dto.setAction(log.getAction());
        dto.setInitialStatus(log.getInitialStatus());
        dto.setFinalStatus(log.getFinalStatus());
        dto.setComment(log.getComment());
        return dto;
    }

    public ShowSignRequestDto.SignRequestLightDto toSignRequestLightDto(SignRequest signRequest) {
        ShowSignRequestDto.SignRequestLightDto dto = new ShowSignRequestDto.SignRequestLightDto();
        dto.setId(signRequest.getId());
        dto.setClonedFromId(signRequest.getClonedFrom() != null ? signRequest.getClonedFrom().getId() : null);
        dto.setStatus(signRequest.getStatus());
        dto.setDeleted(signRequest.getDeleted());
        dto.setToken(signRequest.getToken());
        dto.setCreateBy(signRequest.getCreateBy() != null ? toSignRequestUserDto(signRequest.getCreateBy()) : null);
        dto.setLinks(signRequest.getLinks() != null ? new ArrayList<>(signRequest.getLinks()) : new ArrayList<>());
        return dto;
    }

    public ShowSignRequestDto.SignRequestLightDto toSignRequestLightDto(SignRequestLightProjectionDto signRequest) {
        ShowSignRequestDto.SignRequestLightDto dto = new ShowSignRequestDto.SignRequestLightDto();
        dto.setId(signRequest.getId());
        dto.setClonedFromId(signRequest.getClonedFromId());
        dto.setStatus(signRequest.getStatus());
        dto.setDeleted(signRequest.getDeleted());
        dto.setToken(signRequest.getToken());
        if (signRequest.getCreateById() != null || signRequest.getCreateByEppn() != null
                || signRequest.getCreateByFirstname() != null || signRequest.getCreateByName() != null) {
            ShowSignRequestDto.SignRequestUserDto createBy = new ShowSignRequestDto.SignRequestUserDto();
            createBy.setId(signRequest.getCreateById());
            createBy.setEppn(signRequest.getCreateByEppn());
            createBy.setFirstname(signRequest.getCreateByFirstname());
            createBy.setName(signRequest.getCreateByName());
            dto.setCreateBy(createBy);
        }
        dto.setLinks(List.of());
        return dto;
    }

    public SignBookLightDto toSignBookLightDto(SignBook signBook) {
        return toSignBookLightDto(signBook, signBook.getViewers() != null
                ? signBook.getViewers().stream().map(this::toSignBookViewerDto).toList()
                : List.of());
    }

    public SignBookLightDto toSignBookLightDto(SignBook signBook, List<ShowSignRequestDto.SignBookViewerDto> viewers) {
        SignBookLightDto dto = new SignBookLightDto();
        dto.setId(signBook.getId());
        dto.setWorkflowName(signBook.getWorkflowName());
        dto.setSubject(signBook.getSubject());
        dto.setDescription(signBook.getDescription());
        dto.setStatus(signBook.getStatus());
        dto.setDeleted(signBook.getDeleted());
        dto.setEditable(signBook.isEditable());
        dto.setArchiveStatus(signBook.getArchiveStatus());
        dto.setCreateDate(signBook.getCreateDate());
        dto.setViewers(viewers == null ? List.of() : viewers);
        return dto;
    }

    public ShowSignRequestDto.WorkflowMetaDto toWorkflowMetaDto(Workflow workflow) {
        ShowSignRequestDto.WorkflowMetaDto dto = new ShowSignRequestDto.WorkflowMetaDto();
        dto.setHasWorkflow(workflow != null);
        dto.setExternalCanReaderAnnotations(workflow == null || Boolean.TRUE.equals(workflow.getExternalCanReaderAnnotations()));
        dto.setDisableSidebarForExternal(workflow != null && Boolean.TRUE.equals(workflow.getDisableSidebarForExternal()));
        dto.setExternalCanReaderAttachments(workflow == null || Boolean.TRUE.equals(workflow.getExternalCanReaderAttachments()));
        dto.setExternalCanEdit(workflow == null || Boolean.TRUE.equals(workflow.getExternalCanEdit()));
        dto.setExternalCanEditAttachments(workflow != null && Boolean.TRUE.equals(workflow.getExternalCanEditAttachments()));
        dto.setAuthorizeClone(workflow == null || Boolean.TRUE.equals(workflow.getAuthorizeClone()));
        dto.setForbidDownloadsBeforeEnd(workflow != null && Boolean.TRUE.equals(workflow.getForbidDownloadsBeforeEnd()));
        dto.setSendAlertToAllRecipients(workflow != null && Boolean.TRUE.equals(workflow.getSendAlertToAllRecipients()));
        dto.setWorkflowStepCount(workflow != null && workflow.getWorkflowSteps() != null ? workflow.getWorkflowSteps().size() : 0);
        dto.setMailFrom(workflow != null && workflow.getMailFrom() != null ? workflow.getMailFrom() : "");
        return dto;
    }

    public SignRequestFullDto toAdminSignRequestFullDto(SignRequest signRequest,
                                                        SignBook signBook,
                                                        LiveWorkflow liveWorkflow,
                                                        LiveWorkflowStep currentStep) {
        SignRequestFullDto dto = new SignRequestFullDto();
        dto.setSignRequestId(signRequest.getId());
        dto.setDataId(signRequest.getData() != null ? signRequest.getData().getId() : null);
        dto.setFormId(signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getId() : null);
        dto.setSignRequestParams(List.of());
        dto.setCurrentSignType(signRequest.getCurrentSignType());
        dto.setSignable(false);
        dto.setEditable(false);
        dto.setComments(List.of());
        dto.setSpots(List.of());
        dto.setPdf(false);
        dto.setCurrentStepNumber(liveWorkflow != null ? liveWorkflow.getCurrentStepNumber() : null);
        dto.setCurrentStepMultiSign(currentStep != null && Boolean.TRUE.equals(currentStep.getMultiSign()));
        dto.setCurrentStepSingleSignWithAnnotation(currentStep != null && Boolean.TRUE.equals(currentStep.getSingleSignWithAnnotation()));
        dto.setCurrentStepMinSignLevel(currentStep != null ? currentStep.getMinSignLevel() : null);
        dto.setSignImages(List.of());
        dto.setFields(List.of());
        dto.setStepRepeatable(currentStep != null ? currentStep.getRepeatable() : null);
        dto.setCurrentStepRepeatableSignType(currentStep != null ? currentStep.getRepeatableSignType() : null);
        dto.setStatus(signRequest.getStatus());
        dto.setAction(signRequest.getData() != null && signRequest.getData().getForm() != null ? signRequest.getData().getForm().getAction() : null);
        dto.setNbSignRequests(signBook != null && signBook.getSignRequests() != null ? signBook.getSignRequests().size() : 0);
        dto.setNbPendingSignRequests(signBook != null && signBook.getSignRequests() != null
                ? (int) signBook.getSignRequests().stream()
                    .filter(sr -> SignRequestStatus.pending.equals(sr.getStatus()) && !Boolean.TRUE.equals(sr.getDeleted()))
                    .count()
                : 0);
        dto.setNotSigned(false);
        dto.setAttachmentAlert(false);
        dto.setAttachmentRequire(false);
        dto.setManager(true);
        dto.setUpdateAllowed(false);
        dto.setCommentDeleteAllowed(false);
        dto.setHasDocumentsHistory(signRequest.getDocumentsHistory() != null);
        return dto;
    }

    public ShowSignRequestDto.StepDto toStepDto(LiveWorkflowStep step) {
        ShowSignRequestDto.StepDto dto = new ShowSignRequestDto.StepDto();
        dto.setId(step.getId());
        dto.setDescription(step.getDescription());
        dto.setChangeable(step.getWorkflowStep() != null ? step.getWorkflowStep().getChangeable() : false);
        dto.setSignType(step.getSignType());
        dto.setMinSignLevel(step.getMinSignLevel() != null ? step.getMinSignLevel() : SignLevel.simple);
        dto.setAutoSign(step.getAutoSign());
        dto.setAllSignToComplete(step.getAllSignToComplete());
        dto.setRepeatable(step.getRepeatable());
        dto.setSealVisa(step.getSealVisa());
        dto.setUsers(step.getUsers().stream().map(this::toStepUserDto).toList());
        dto.setRecipients(step.getRecipients().stream().map(this::toStepRecipientDto).toList());
        return dto;
    }

    public ShowSignRequestDto.TargetDto toTargetDto(Target target) {
        ShowSignRequestDto.TargetDto dto = new ShowSignRequestDto.TargetDto();
        dto.setTargetUri(target.getTargetUri());
        dto.setProtectedTargetUri(target.getProtectedTargetUri());
        dto.setTargetOk(target.getTargetOk());
        return dto;
    }

    public ShowSignRequestDto.TargetDto toTargetDto(LiveWorkflowTargetProjectionDto target) {
        ShowSignRequestDto.TargetDto dto = new ShowSignRequestDto.TargetDto();
        dto.setTargetUri(target.getTargetUri());
        dto.setProtectedTargetUri(toProtectedTargetUri(target.getTargetUri()));
        dto.setTargetOk(target.getTargetOk());
        return dto;
    }

    public ShowSignRequestDto.StepUserDto toStepUserDto(User user) {
        ShowSignRequestDto.StepUserDto dto = new ShowSignRequestDto.StepUserDto();
        dto.setId(user.getId());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setHidedPhone(user.getHidedPhone());
        dto.setUserType(user.getUserType());
        return dto;
    }

    public List<ShowSignRequestDto.StepDto> toStepDtos(List<LiveWorkflowStepProjectionDto> steps,
                                                       List<LiveWorkflowStepRecipientProjectionDto> recipients) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        Map<Long, List<LiveWorkflowStepRecipientProjectionDto>> recipientsByStepId = recipients == null
                ? Map.of()
                : recipients.stream().collect(Collectors.groupingBy(
                        LiveWorkflowStepRecipientProjectionDto::getStepId,
                        LinkedHashMap::new,
                        Collectors.toList()));
        return steps.stream().map(step -> {
            List<LiveWorkflowStepRecipientProjectionDto> stepRecipients = recipientsByStepId.getOrDefault(step.getId(), List.of());
            ShowSignRequestDto.StepDto dto = new ShowSignRequestDto.StepDto();
            dto.setId(step.getId());
            dto.setDescription(step.getDescription());
            dto.setChangeable(Boolean.TRUE.equals(step.getChangeable()));
            dto.setSignType(step.getSignType());
            dto.setMinSignLevel(step.getMinSignLevel() != null ? step.getMinSignLevel() : SignLevel.simple);
            dto.setAutoSign(Boolean.TRUE.equals(step.getAutoSign()));
            dto.setAllSignToComplete(Boolean.TRUE.equals(step.getAllSignToComplete()));
            dto.setRepeatable(Boolean.TRUE.equals(step.getRepeatable()));
            dto.setSealVisa(Boolean.TRUE.equals(step.getSealVisa()));
            dto.setUsers(stepRecipients.stream().map(this::toStepUserDto).filter(Objects::nonNull).toList());
            dto.setRecipients(stepRecipients.stream().map(this::toStepRecipientDto).filter(Objects::nonNull).toList());
            return dto;
        }).toList();
    }

    public ShowSignRequestDto.DocumentDto toDocumentDto(DocumentProjectionDto document) {
        ShowSignRequestDto.DocumentDto dto = new ShowSignRequestDto.DocumentDto();
        dto.setId(document.getId());
        dto.setFileName(document.getFileName());
        dto.setSize(document.getSize());
        dto.setContentType(document.getContentType());
        return dto;
    }

    public ShowSignRequestDto.AttachmentDto toAttachmentDto(AttachmentProjectionDto attachment) {
        ShowSignRequestDto.AttachmentDto dto = new ShowSignRequestDto.AttachmentDto();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        ShowSignRequestDto.AttachmentUserDto createBy = null;
        if (attachment.getCreateByEppn() != null || attachment.getCreateByFirstname() != null || attachment.getCreateByName() != null) {
            createBy = new ShowSignRequestDto.AttachmentUserDto();
            createBy.setEppn(attachment.getCreateByEppn());
            createBy.setFirstname(attachment.getCreateByFirstname());
            createBy.setName(attachment.getCreateByName());
        }
        dto.setCreateBy(createBy);
        return dto;
    }

    public ShowSignRequestDto.SignRequestTabDto toSignRequestTabDto(SignRequestTabProjectionDto signRequest) {
        ShowSignRequestDto.SignRequestTabDto dto = new ShowSignRequestDto.SignRequestTabDto();
        dto.setId(signRequest.getId());
        dto.setTitle(signRequest.getTitle());
        dto.setStatus(signRequest.getStatus());
        dto.setDeleted(signRequest.getDeleted());
        dto.setViewedByCurrentUser(Boolean.TRUE.equals(signRequest.getViewedByCurrentUser()));
        return dto;
    }

    public SignRequestFullDto toCommonDto(ShowSignRequestContextDto context, boolean updateAllowed) {
        SignRequestFullDto dto = new SignRequestFullDto();
        dto.setSignRequestId(context.getSignRequestId());
        dto.setDataId(context.getDataId());
        dto.setFormId(context.getFormId());
        dto.setSignRequestParams(context.getCurrentSignRequestParamses());
        dto.setCurrentSignType(context.getCurrentSignType());
        dto.setSignable(context.isSignable());
        dto.setEditable(context.isEditable());
        dto.setComments(context.getComments());
        dto.setSpots(context.getSpots());
        dto.setPdf(context.isPdf());
        dto.setCurrentStepNumber(context.getCurrentStepNumber());
        dto.setCurrentStepMultiSign(context.isCurrentStepMultiSign());
        dto.setCurrentStepSingleSignWithAnnotation(context.isCurrentStepSingleSignWithAnnotation());
        dto.setCurrentStepMinSignLevel(context.getCurrentStepMinSignLevel());
        dto.setSignImages(context.getSignImages());
        dto.setFields(null); // champs portes en FieldFrontDto via context.getFieldFrontDtos()
        dto.setStepRepeatable(context.getStepRepeatable());
        dto.setCurrentStepRepeatableSignType(context.getCurrentStepRepeatableSignType());
        dto.setStatus(context.getSignRequestStatus());
        dto.setAction(context.getAction());
        dto.setNbSignRequests(context.getNbSignRequestInSignBookParent());
        dto.setNbPendingSignRequests(context.getNbPendingSignRequestInSignBookParent());
        dto.setNotSigned(context.isNotSigned());
        dto.setAttachmentAlert(context.isAttachmentAlert());
        dto.setAttachmentRequire(context.isAttachmentRequire());
        dto.setManager(context.isManager());
        dto.setUpdateAllowed(updateAllowed);
        dto.setCommentDeleteAllowed(context.isManager() && isCommentDeleteStatusAllowed(context));
        dto.setHasDocumentsHistory(context.isHasDocumentsHistory());
        return dto;
    }

    public List<CommentFrontDto> toCommentFrontDtos(List<Comment> comments) {
        return toCommentFrontDtos(comments, null);
    }

    public List<CommentFrontDto> toCommentFrontDtos(List<Comment> comments, ShowSignRequestContextDto context) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        return comments.stream()
                .map(comment -> {
                    CommentFrontDto dto = new CommentFrontDto();
                    dto.setId(comment.getId());
                    dto.setPageNumber(comment.getPageNumber());
                    dto.setStepNumber(comment.getStepNumber());
                    dto.setPosX(comment.getPosX());
                    dto.setPosY(comment.getPosY());
                    dto.setDeleteAllowed(isCommentDeleteAllowed(comment, context));
                    return dto;
                })
                .toList();
    }

    private boolean isCommentDeleteAllowed(Comment comment, ShowSignRequestContextDto context) {
        if (comment == null || context == null || !isCommentDeleteStatusAllowed(context)) {
            return false;
        }
        boolean managerAllowed = context.isManager();
        boolean creatorAllowed = comment.getCreateBy() != null
                && Objects.equals(comment.getCreateBy().getEppn(), context.getAuthUserEppn());
        return managerAllowed || creatorAllowed;
    }

    private boolean isCommentDeleteStatusAllowed(ShowSignRequestContextDto context) {
        return context != null
                && (context.getSignRequestStatus() == SignRequestStatus.draft
                    || context.getSignRequestStatus() == SignRequestStatus.pending);
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

    public List<FieldFrontDto> toFieldFrontDtos(List<Field> fields, Workflow workflow) {
        if (fields == null || fields.isEmpty()) {
            return List.of();
        }
        return fields.stream()
                .map(field -> {
                    FieldFrontDto dto = new FieldFrontDto();
                    dto.setId(field.getId());
                    dto.setName(field.getName());
                    dto.setDescription(field.getDescription());
                    dto.setPage(field.getPage());
                    dto.setRequired(field.getRequired());
                    dto.setReadOnly(field.getReadOnly());
                    dto.setEditable(field.getEditable());
                    dto.setWorkflowSteps(toWorkflowStepNumbers(field, workflow));
                    dto.setDefaultValue(field.getDefaultValue());
                    dto.setSearchServiceName(field.getSearchServiceName());
                    dto.setSearchType(field.getSearchType());
                    dto.setSearchReturn(field.getSearchReturn());
                    dto.setType(field.getType() != null ? field.getType().name().toLowerCase() : null);
                    dto.setFavorisable(field.getFavorisable());
                    return dto;
                })
                .toList();
    }

    private ShowSignRequestDto.SignRequestUserDto toSignRequestUserDto(User user) {
        ShowSignRequestDto.SignRequestUserDto dto = new ShowSignRequestDto.SignRequestUserDto();
        dto.setId(user.getId());
        dto.setEppn(user.getEppn());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    private ShowSignRequestDto.SignBookViewerDto toSignBookViewerDto(User viewer) {
        ShowSignRequestDto.SignBookViewerDto dto = new ShowSignRequestDto.SignBookViewerDto();
        dto.setId(viewer.getId());
        dto.setFirstname(viewer.getFirstname());
        dto.setName(viewer.getName());
        dto.setEmail(viewer.getEmail());
        return dto;
    }

    public ShowSignRequestDto.SignBookViewerDto toSignBookViewerDto(SignBookViewerProjectionDto viewer) {
        ShowSignRequestDto.SignBookViewerDto dto = new ShowSignRequestDto.SignBookViewerDto();
        dto.setId(viewer.getId());
        dto.setFirstname(viewer.getFirstname());
        dto.setName(viewer.getName());
        dto.setEmail(viewer.getEmail());
        return dto;
    }

    private ShowSignRequestDto.StepRecipientDto toStepRecipientDto(org.esupportail.esupsignature.entity.Recipient recipient) {
        ShowSignRequestDto.StepRecipientDto dto = new ShowSignRequestDto.StepRecipientDto();
        dto.setId(recipient.getId());
        dto.setUser(recipient.getUser() != null ? toStepUserDto(recipient.getUser()) : null);
        dto.setSigned(recipient.getSigned());
        return dto;
    }

    private ShowSignRequestDto.StepUserDto toStepUserDto(LiveWorkflowStepRecipientProjectionDto recipient) {
        if (recipient == null || recipient.getUserId() == null) {
            return null;
        }
        return new ShowSignRequestDto.StepUserDto(
                recipient.getUserId(),
                recipient.getUserFirstname(),
                recipient.getUserName(),
                recipient.getUserEmail(),
                recipient.getUserPhone(),
                toHidedPhone(recipient.getUserPhone()),
                recipient.getUserUserType()
        );
    }

    private ShowSignRequestDto.StepRecipientDto toStepRecipientDto(LiveWorkflowStepRecipientProjectionDto recipient) {
        if (recipient == null || recipient.getRecipientId() == null) {
            return null;
        }
        ShowSignRequestDto.StepRecipientDto dto = new ShowSignRequestDto.StepRecipientDto();
        dto.setId(recipient.getRecipientId());
        dto.setUser(toStepUserDto(recipient));
        dto.setSigned(recipient.getSigned());
        return dto;
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

    public String toDisplayName(User user) {
        if (user == null) {
            return null;
        }
        String firstname = user.getFirstname() != null ? user.getFirstname().trim() : "";
        String name = user.getName() != null ? user.getName().trim() : "";
        String displayName = (firstname + " " + name).trim();
        return displayName.isEmpty() ? null : displayName;
    }

    private String toProtectedTargetUri(String targetUri) {
        if (targetUri == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("[^@]*:\\/\\/[^:]*:([^@]*)@.*?$");
        Matcher matcher = pattern.matcher(targetUri);
        StringBuffer stringBuffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, matcher.group(0).replaceFirst(Pattern.quote(matcher.group(1)), "********"));
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString().replaceAll("\\?.*", "?...");
    }

    private String toHidedPhone(String phone) {
        if (phone == null || phone.length() <= 4) {
            return "";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
