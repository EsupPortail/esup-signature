package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.user.signbook.SignBookFullDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.projection.jpa.HomePostitItemProjection;
import org.esupportail.esupsignature.dto.projection.jpa.HomeSignRequestItemProjection;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowStepRecipientProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.LiveWorkflowTargetProjectionDto;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookListMetadataProjection;
import org.esupportail.esupsignature.dto.projection.jpa.SignBookViewerProjectionDto;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.LiveWorkflowStep;
import org.esupportail.esupsignature.entity.Recipient;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
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

    public SignBookFullDto toSignBookListItemDto(SignBook signBook, String userEppn, SignBookListMetadataProjection metadata) {
        Integer currentStepNumber = signBook.getLiveWorkflow() != null ? signBook.getLiveWorkflow().getCurrentStepNumber() : null;
        int signRequestCount = metadata != null && metadata.getSignRequestCount() != null ? metadata.getSignRequestCount().intValue() : 0;
        return toSignBookFullDto(signBook, userEppn, false, currentStepNumber, List.of(), List.of(), List.of(), toPrimarySignRequestData(metadata), signRequestCount, false);
    }

    public List<SignBookFullDto.SignRequestDocumentDto> toSignRequestDocumentDtosFromProjections(List<HomeSignRequestItemProjection> signRequests) {
        if (signRequests == null || signRequests.isEmpty()) {
            return List.of();
        }
        return signRequests.stream().map(signRequest -> new SignBookFullDto.SignRequestDocumentDto(
                signRequest.getSignRequestId(),
                signRequest.getTitle(),
                signRequest.getStatus(),
                signRequest.getFileName(),
                null,
                null,
                null
        )).toList();
    }

    public List<SignBookFullDto.PostitDto> toPostitDtosFromProjections(List<HomePostitItemProjection> postits) {
        if (postits == null || postits.isEmpty()) {
            return List.of();
        }
        return postits.stream()
                .filter(Objects::nonNull)
                .map(postit -> new SignBookFullDto.PostitDto(
                        toDisplayName(postit.getAuthorFirstname(), postit.getAuthorName()),
                        postit.getText()
                ))
                .toList();
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
        int signRequestCount = signBook.getSignRequests().size();
        return toSignBookFullDto(signBook, userEppn, editable, liveWorkflowCurrentStepNumber, viewers, liveWorkflowSteps, liveWorkflowTargets, toPrimarySignRequestData(primarySignRequest, userEppn), signRequestCount, true);
    }

    private SignBookFullDto toSignBookFullDto(SignBook signBook,
                                              String userEppn,
                                              boolean editable,
                                              Integer liveWorkflowCurrentStepNumber,
                                              List<ShowSignRequestDto.SignBookViewerDto> viewers,
                                              List<ShowSignRequestDto.StepDto> liveWorkflowSteps,
                                              List<ShowSignRequestDto.TargetDto> liveWorkflowTargets,
                                              PrimarySignRequestData primarySignRequest,
                                              int signRequestCount,
                                              boolean includeFullListDetails) {
        String currentSignType = null;
        if (signBook.getLiveWorkflow() != null && signBook.getLiveWorkflow().getCurrentStep() != null && signBook.getLiveWorkflow().getCurrentStep().getSignType() != null) {
            currentSignType = signBook.getLiveWorkflow().getCurrentStep().getSignType().name();
        }

        String refusedCommentTitle = null;
        if (primarySignRequest != null) {
            refusedCommentTitle = primarySignRequest.lastComment();
        }

        SignBookFullDto dto = new SignBookFullDto(
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
                toParticipantSteps(signBook, primarySignRequest, signRequestCount == 1),
                formatDate(signBook.getEndDate()),
                Boolean.TRUE.equals(signBook.getDeleted()) ? "Supprimé le : " + formatDate(signBook.getUpdateDate()) : null,
                toLastSignedDocumentDateLabel(signBook, primarySignRequest, signRequestCount),
                refusedCommentTitle,
                toPrimarySignRequestDto(signBook, primarySignRequest, userEppn, signRequestCount),
                includeFullListDetails ? toSignRequestDocumentDtos(signBook.getSignRequests()) : List.of(),
                includeFullListDetails ? toPostitDtos(signBook.getPostits()) : List.of(),
                editable,
                liveWorkflowCurrentStepNumber,
                viewers,
                liveWorkflowSteps,
                liveWorkflowTargets
        );
        dto.setSignRequestCount(signRequestCount);
        return dto;
    }

    private SignBookFullDto.PrimarySignRequestDto toPrimarySignRequestDto(SignBook signBook, PrimarySignRequestData signRequest, String userEppn, int signRequestCount) {
        if (signRequest == null) {
            return null;
        }
        return new SignBookFullDto.PrimarySignRequestDto(
                signRequest.id(),
                signRequest.title(),
                signRequest.status() != null ? signRequest.status().name() : null,
                formatDate(signRequest.createDate()),
                signRequest.viewedByCurrentUser(),
                signRequest.hasAttachments(),
                signRequest.deleted(),
                buildPrimaryRowTitle(signBook, signRequest, signRequestCount),
                canDownloadSingle(signBook, signRequest),
                canDownloadAll(signRequestCount, signRequest)
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

    private String toDisplayName(String firstname, String name) {
        return (Objects.requireNonNullElse(firstname, "") + " " + Objects.requireNonNullElse(name, "")).trim();
    }

    private List<SignBookFullDto.ParticipantStepDto> toParticipantSteps(SignBook signBook, PrimarySignRequestData primarySignRequest, boolean singleDocument) {
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
                    .map(recipient -> toParticipantDto(recipient, primarySignRequest, singleDocument))
                    .toList();
            participantSteps.add(new SignBookFullDto.ParticipantStepDto(i + 1, recipients));
        }
        return participantSteps;
    }

    private SignBookFullDto.ParticipantDto toParticipantDto(Recipient recipient, PrimarySignRequestData primarySignRequest, boolean singleDocument) {
        String statusKey = null;
        if (singleDocument && primarySignRequest != null) {
            if (Boolean.TRUE.equals(recipient.getSigned())) {
                if (primarySignRequest.status() == SignRequestStatus.refused) {
                    statusKey = "refused";
                } else {
                    statusKey = "signed";
                }
            } else if (primarySignRequest.status() == SignRequestStatus.pending) {
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

    private String buildPrimaryRowTitle(SignBook signBook, PrimarySignRequestData signRequest, int signRequestCount) {
        if (signRequestCount <= 1) {
            return signBook.getSubject();
        }
        String firstOriginalFileName = signRequest.firstOriginalFileName();
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

    private String toLastSignedDocumentDateLabel(SignBook signBook, PrimarySignRequestData primarySignRequest, int signRequestCount) {
        if (primarySignRequest == null) {
            return null;
        }
        if (!(!(Boolean.TRUE.equals(signBook.getDeleted()) || signBook.getEndDate() != null) || signRequestCount != 1)) {
            return null;
        }
        if (primarySignRequest.lastSignedDocumentDate() == null) {
            return null;
        }
        return formatDate(primarySignRequest.lastSignedDocumentDate());
    }

    private boolean canDownloadAll(int signRequestCount, PrimarySignRequestData signRequest) {
        if (signRequestCount <= 1 || signRequest.status() == null) {
            return false;
        }
        return signRequest.status() == SignRequestStatus.completed
                || signRequest.status() == SignRequestStatus.exported
                || signRequest.status() == SignRequestStatus.archived;
    }

    private boolean canDownloadSingle(SignBook signBook, PrimarySignRequestData signRequest) {
        if (signRequest.status() == null) {
            return false;
        }
        if (signRequest.status() == SignRequestStatus.completed
                || signRequest.status() == SignRequestStatus.exported
                || signRequest.status() == SignRequestStatus.archived) {
            return true;
        }
        boolean forbidDownloadsBeforeEnd = signBook.getLiveWorkflow() != null
                && signBook.getLiveWorkflow().getWorkflow() != null
                && Boolean.TRUE.equals(signBook.getLiveWorkflow().getWorkflow().getForbidDownloadsBeforeEnd());
        return !forbidDownloadsBeforeEnd && signRequest.status() == SignRequestStatus.pending;
    }

    private PrimarySignRequestData toPrimarySignRequestData(SignRequest signRequest, String userEppn) {
        if (signRequest == null) {
            return null;
        }
        String lastComment = signRequest.getComments() == null || signRequest.getComments().isEmpty()
                ? null
                : signRequest.getComments().get(signRequest.getComments().size() - 1).getText();
        Document lastSignedDocument = signRequest.getLastSignedDocument();
        return new PrimarySignRequestData(
                signRequest.getId(),
                signRequest.getTitle(),
                signRequest.getStatus(),
                signRequest.getCreateDate(),
                isViewedByUser(signRequest, userEppn),
                signRequest.getAttachments() != null && !signRequest.getAttachments().isEmpty(),
                Boolean.TRUE.equals(signRequest.getDeleted()),
                getFirstOriginalFileName(signRequest),
                lastSignedDocument != null ? lastSignedDocument.getCreateDate() : null,
                lastComment
        );
    }

    private PrimarySignRequestData toPrimarySignRequestData(SignBookListMetadataProjection metadata) {
        if (metadata == null || metadata.getPrimarySignRequestId() == null) {
            return null;
        }
        SignRequestStatus status = metadata.getPrimarySignRequestStatus() == null
                ? null
                : SignRequestStatus.valueOf(metadata.getPrimarySignRequestStatus());
        boolean deleted = Boolean.TRUE.equals(metadata.getPrimarySignRequestDeleted()) || status == SignRequestStatus.deleted;
        return new PrimarySignRequestData(
                metadata.getPrimarySignRequestId(),
                metadata.getPrimarySignRequestTitle(),
                status,
                metadata.getPrimarySignRequestCreateDate(),
                Boolean.TRUE.equals(metadata.getPrimarySignRequestViewedByCurrentUser()),
                Boolean.TRUE.equals(metadata.getPrimarySignRequestHasAttachments()),
                deleted,
                metadata.getPrimarySignRequestFirstOriginalFileName(),
                metadata.getPrimarySignRequestLastSignedDocumentDate(),
                metadata.getPrimarySignRequestLastComment()
        );
    }

    private record PrimarySignRequestData(Long id,
                                          String title,
                                          SignRequestStatus status,
                                          Date createDate,
                                          boolean viewedByCurrentUser,
                                          boolean hasAttachments,
                                          boolean deleted,
                                          String firstOriginalFileName,
                                          Date lastSignedDocumentDate,
                                          String lastComment) {
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
}
