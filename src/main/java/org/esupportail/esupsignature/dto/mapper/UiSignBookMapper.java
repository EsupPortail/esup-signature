package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.user.signbook.SignBookFullDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepRecipientProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowTargetProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookViewerProjectionDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.Tag;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.service.TagService;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class UiSignBookMapper {

    private final UiFetchSignRequestMapper uiFetchSignRequestMapper;

    public UiSignBookMapper(UiFetchSignRequestMapper uiFetchSignRequestMapper) {
        this.uiFetchSignRequestMapper = uiFetchSignRequestMapper;
    }

    public SignBookFullDto toSignBookListItemDto(SignBook signBook, String userEppn) {
        Integer currentStepNumber = signBook.getLiveWorkflow() != null ? signBook.getLiveWorkflow().getCurrentStepNumber() : null;
        return toSignBookFullDto(signBook, userEppn, false, currentStepNumber, List.of(), List.of(), List.of());
    }

    public SignBookFullDto toSignBookUpdateViewDto(SignBook signBook,
                                                   String userEppn,
                                                   List<ShowSignRequestDto.SignBookViewerDto> viewers,
                                                   List<ShowSignRequestDto.StepDto> liveWorkflowSteps,
                                                   List<ShowSignRequestDto.TargetDto> liveWorkflowTargets) {
        boolean editable = signBook.getLiveWorkflow() != null && signBook.isEditable();
        Integer currentStepNumber = signBook.getLiveWorkflow() != null ? signBook.getLiveWorkflow().getCurrentStepNumber() : null;
        return toSignBookFullDto(signBook, userEppn, editable, currentStepNumber, viewers, liveWorkflowSteps, liveWorkflowTargets);
    }

    public List<ShowSignRequestDto.SignBookViewerDto> toSignBookViewerDtos(List<SignBookViewerProjectionDto> viewers) {
        if (viewers == null || viewers.isEmpty()) {
            return List.of();
        }
        return viewers.stream().map(uiFetchSignRequestMapper::toSignBookViewerDto).toList();
    }

    public List<ShowSignRequestDto.StepDto> toLiveWorkflowStepDtos(List<LiveWorkflowStepProjectionDto> steps,
                                                                   List<LiveWorkflowStepRecipientProjectionDto> recipients) {
        return uiFetchSignRequestMapper.toStepDtos(steps, recipients);
    }

    public List<ShowSignRequestDto.TargetDto> toLiveWorkflowTargetDtos(List<LiveWorkflowTargetProjectionDto> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        return targets.stream().map(uiFetchSignRequestMapper::toTargetDto).toList();
    }

    private SignBookFullDto toSignBookFullDto(SignBook signBook,
                                              String userEppn,
                                              boolean editable,
                                              Integer liveWorkflowCurrentStepNumber,
                                              List<ShowSignRequestDto.SignBookViewerDto> viewers,
                                              List<ShowSignRequestDto.StepDto> liveWorkflowSteps,
                                              List<ShowSignRequestDto.TargetDto> liveWorkflowTargets) {
        SignRequest primarySignRequest = signBook.getSignRequests().isEmpty() ? null : signBook.getSignRequests().get(0);
        String currentSignType = null;
        if (signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getSignType() != null) {
            currentSignType = signBook.getLiveWorkflow().getCurrentStep().getSignType().name();
        }

        String refusedCommentTitle = null;
        if (primarySignRequest != null && primarySignRequest.getComments() != null && !primarySignRequest.getComments().isEmpty()) {
            refusedCommentTitle = primarySignRequest.getComments().get(primarySignRequest.getComments().size() - 1).getText();
        }

        return new SignBookFullDto(
                signBook.getId(),
                signBook.getSubject(),
                signBook.getDescription(),
                signBook.getWorkflowName(),
                uiFetchSignRequestMapper.toDisplayName(signBook.getCreateBy()),
                signBook.getCreateBy() != null ? signBook.getCreateBy().getEppn() : null,
                signBook.getStatus() != null ? signBook.getStatus().name() : null,
                Boolean.TRUE.equals(signBook.getDeleted()),
                signBook.getArchiveStatus() != null ? signBook.getArchiveStatus().name() : null,
                Boolean.TRUE.equals(signBook.getDeleteableByCurrentUser()),
                Boolean.TRUE.equals(signBook.getDisplayNotif()),
                isHiddenByCurrentUser(signBook, userEppn),
                currentSignType,
                toParticipantSteps(signBook, primarySignRequest),
                formatDate(signBook.getEndDate()),
                Boolean.TRUE.equals(signBook.getDeleted()) ? "Supprimé le : " + formatDate(signBook.getUpdateDate()) : null,
                toLastSignedDocumentDateLabel(signBook, primarySignRequest),
                refusedCommentTitle,
                toPrimarySignRequestDto(signBook, primarySignRequest, userEppn),
                toSignRequestDocumentDtos(signBook.getSignRequests()),
                toPostitDtos(signBook.getPostits()),
                editable,
                liveWorkflowCurrentStepNumber,
                viewers,
                liveWorkflowSteps,
                liveWorkflowTargets,
                toTagDtos(signBook)
        );
    }

    private SignBookFullDto.PrimarySignRequestDto toPrimarySignRequestDto(SignBook signBook, SignRequest signRequest, String userEppn) {
        if (signRequest == null) {
            return null;
        }
        return new SignBookFullDto.PrimarySignRequestDto(
                signRequest.getId(),
                signRequest.getTitle(),
                signRequest.getStatus() != null ? signRequest.getStatus().name() : null,
                formatDate(signRequest.getCreateDate()),
                isViewedByUser(signRequest, userEppn),
                signRequest.getAttachments() != null && !signRequest.getAttachments().isEmpty(),
                Boolean.TRUE.equals(signRequest.getDeleted()),
                buildPrimaryRowTitle(signBook, signRequest),
                canDownloadSingle(signBook, signRequest),
                canDownloadAll(signBook, signRequest),
                signRequest.getPaperlessSignedDocumentId()
        );
    }

    private List<SignBookFullDto.SignRequestDocumentDto> toSignRequestDocumentDtos(List<SignRequest> signRequests) {
        if (signRequests == null || signRequests.isEmpty()) {
            return List.of();
        }
        return signRequests.stream().map(signRequest -> new SignBookFullDto.SignRequestDocumentDto(
                signRequest.getId(),
                signRequest.getTitle(),
                signRequest.getStatus() != null ? signRequest.getStatus().name() : null,
                getFirstOriginalFileName(signRequest),
                formatDate(signRequest.getCreateDate()),
                uiFetchSignRequestMapper.toDisplayName(signRequest.getCreateBy()),
                signRequest.getCreateBy() != null ? signRequest.getCreateBy().getEppn() : null
        )).toList();
    }

    private List<SignBookFullDto.PostitDto> toPostitDtos(List<Comment> postits) {
        if (postits == null || postits.isEmpty()) {
            return List.of();
        }
        return postits.stream()
                .filter(Objects::nonNull)
                .map(postit -> new SignBookFullDto.PostitDto(uiFetchSignRequestMapper.toDisplayName(postit.getCreateBy()), postit.getText()))
                .toList();
    }

    private List<SignBookFullDto.ParticipantStepDto> toParticipantSteps(SignBook signBook, SignRequest primarySignRequest) {
        if (signBook.getLiveWorkflow() == null
                || signBook.getLiveWorkflow().getLiveWorkflowSteps() == null
                || signBook.getLiveWorkflow().getLiveWorkflowSteps().isEmpty()
                || signBook.getLiveWorkflow().getCurrentStepNumber() == null
                || signBook.getLiveWorkflow().getCurrentStepNumber() <= 0) {
            return List.of();
        }
        List<SignBookFullDto.ParticipantStepDto> participantSteps = new ArrayList<>();
        for (int i = 0; i < signBook.getLiveWorkflow().getLiveWorkflowSteps().size(); i++) {
            LiveWorkflowStep liveWorkflowStep = signBook.getLiveWorkflow().getLiveWorkflowSteps().get(i);
            List<SignBookFullDto.ParticipantDto> recipients = liveWorkflowStep.getRecipients().stream()
                    .map(recipient -> toParticipantDto(recipient, primarySignRequest, signBook.getSignRequests().size() == 1))
                    .toList();
            participantSteps.add(new SignBookFullDto.ParticipantStepDto(i + 1, recipients));
        }
        return participantSteps;
    }

    private SignBookFullDto.ParticipantDto toParticipantDto(Recipient recipient, SignRequest primarySignRequest, boolean singleDocument) {
        String statusKey = null;
        if (singleDocument && primarySignRequest != null) {
            if (Boolean.TRUE.equals(recipient.getSigned())) {
                if (primarySignRequest.getStatus() == SignRequestStatus.refused) {
                    statusKey = "refused";
                } else {
                    statusKey = "signed";
                }
            } else if (primarySignRequest.getStatus() == SignRequestStatus.pending) {
                statusKey = "pending";
            } else {
                statusKey = "notSigned";
            }
        }
        return new SignBookFullDto.ParticipantDto(
                recipient.getUser() != null ? recipient.getUser().getEmail() : null,
                toDisplayNameOrEmail(recipient.getUser()),
                statusKey
        );
    }

    private boolean isViewedByUser(SignRequest signRequest, String userEppn) {
        if (userEppn == null || signRequest.getViewedBy() == null || signRequest.getViewedBy().isEmpty()) {
            return false;
        }
        return signRequest.getViewedBy().stream().anyMatch(user -> user != null && userEppn.equals(user.getEppn()));
    }

    private boolean isHiddenByCurrentUser(SignBook signBook, String userEppn) {
        if (userEppn == null || signBook.getHidedBy() == null || signBook.getHidedBy().isEmpty()) {
            return false;
        }
        return signBook.getHidedBy().stream().anyMatch(user -> user != null && userEppn.equals(user.getEppn()));
    }

    private String buildPrimaryRowTitle(SignBook signBook, SignRequest signRequest) {
        if (signBook.getSignRequests().size() <= 1) {
            return signBook.getSubject();
        }
        String firstOriginalFileName = getFirstOriginalFileName(signRequest);
        if (firstOriginalFileName == null) {
            return signBook.getSubject();
        }
        if (Objects.equals(signBook.getSubject(), firstOriginalFileName)) {
            return firstOriginalFileName + ", ...";
        }
        return signBook.getSubject();
    }

    private String getFirstOriginalFileName(SignRequest signRequest) {
        if (signRequest.getOriginalDocuments() == null || signRequest.getOriginalDocuments().isEmpty()) {
            return null;
        }
        return signRequest.getOriginalDocuments().get(0).getFileName();
    }

    private String toLastSignedDocumentDateLabel(SignBook signBook, SignRequest primarySignRequest) {
        if (primarySignRequest == null) {
            return null;
        }
        if (!(!(Boolean.TRUE.equals(signBook.getDeleted()) || signBook.getEndDate() != null) || signBook.getSignRequests().size() != 1)) {
            return null;
        }
        Document lastSignedDocument = primarySignRequest.getLastSignedDocument();
        if (lastSignedDocument == null || lastSignedDocument.getCreateDate() == null) {
            return null;
        }
        return formatDate(lastSignedDocument.getCreateDate());
    }

    private boolean canDownloadAll(SignBook signBook, SignRequest signRequest) {
        if (signBook.getSignRequests().size() <= 1 || signRequest.getStatus() == null) {
            return false;
        }
        return signRequest.getStatus() == SignRequestStatus.completed
                || signRequest.getStatus() == SignRequestStatus.exported
                || signRequest.getStatus() == SignRequestStatus.archived;
    }

    private boolean canDownloadSingle(SignBook signBook, SignRequest signRequest) {
        if (signRequest.getStatus() == null) {
            return false;
        }
        if (signRequest.getStatus() == SignRequestStatus.completed
                || signRequest.getStatus() == SignRequestStatus.exported
                || signRequest.getStatus() == SignRequestStatus.archived) {
            return true;
        }
        boolean forbidDownloadsBeforeEnd = signBook.getLiveWorkflow() != null
                && signBook.getLiveWorkflow().getWorkflow() != null
                && Boolean.TRUE.equals(signBook.getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd());
        return !forbidDownloadsBeforeEnd && signRequest.getStatus() == SignRequestStatus.pending;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(date);
    }

    private String toDisplayNameOrEmail(User user) {
        String displayName = uiFetchSignRequestMapper.toDisplayName(user);
        if (displayName != null) {
            return displayName;
        }
        return user != null ? user.getEmail() : null;
    }

    private List<SignBookFullDto.TagDto> toTagDtos(SignBook signBook) {
        if (signBook.getTags() == null || signBook.getTags().isEmpty()) {
            return List.of();
        }
        List<SignBookFullDto.TagDto> dtos = new ArrayList<>();
        for (Tag tag : signBook.getTags()) {
            String parentName = null;
            String rootName = null;
            String groupColor = null;
            if (tag.getParentTag() != null) {
                parentName = tag.getParentTag().getName();
                String rawGroupColor = tag.getParentTag().getColor();
                groupColor = rawGroupColor != null ? TagService.darkenOrLighten(rawGroupColor, 1) : null;
                Tag root = tag;
                while (root.getParentTag() != null) {
                    root = root.getParentTag();
                }
                rootName = root.getName();
            } else {
                rootName = tag.getName();
            }
            dtos.add(new SignBookFullDto.TagDto(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                groupColor,
                parentName,
                rootName
            ));
        }
        return dtos;
    }
}

