package org.esupportail.esupsignature.dto.page.user.signbook;

import java.util.List;

public record SignBookListItemDto(
        Long id,
        String subject,
        String description,
        String workflowName,
        String createByDisplayName,
        String createByEppn,
        String status,
        boolean deleted,
        String archiveStatus,
        boolean deleteableByCurrentUser,
        boolean displayNotif,
        boolean hiddenByCurrentUser,
        String currentSignType,
        List<ParticipantStepDto> participantSteps,
        String endDateLabel,
        String deletedDateLabel,
        String lastSignedDocumentDateLabel,
        String refusedCommentTitle,
        PrimarySignRequestDto primarySignRequest,
        List<SignRequestDocumentDto> signRequests,
        List<PostitDto> postits
) {

    public boolean isEmpty() {
        return primarySignRequest == null;
    }

    public int getSignRequestCount() {
        return signRequests == null ? 0 : signRequests.size();
    }

    public boolean isMultiSignRequest() {
        return getSignRequestCount() > 1;
    }

    public boolean hasParticipants() {
        return participantSteps != null && !participantSteps.isEmpty();
    }

    public record PrimarySignRequestDto(
            Long id,
            String title,
            String status,
            String createDateLabel,
            boolean viewedByCurrentUser,
            boolean hasAttachments,
            boolean deleted,
            String rowTitle,
            boolean canDownload,
            boolean canDownloadAll
    ) {
    }

    public record SignRequestDocumentDto(
            Long id,
            String title,
            String status,
            String fileName
    ) {
    }

    public record PostitDto(
            String author,
            String text
    ) {
    }

    public record ParticipantStepDto(
            Integer stepNumber,
            List<ParticipantDto> recipients
    ) {
    }

    public record ParticipantDto(
            String email,
            String displayName,
            String statusIconClass,
            String statusTitle
    ) {
    }
}
