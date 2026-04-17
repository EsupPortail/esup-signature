package org.esupportail.esupsignature.dto.ui.global;

import java.util.List;

public record UiHomeDto(
        Long startFormId,
        Long startWorkflowId,
        String warningReadUrl,
        String searchUrl,
        String searchTitlesUrl,
        List<SignBookItem> toSignSignBooks,
        List<SignBookItem> pendingSignBooks
) {

    public record SignBookItem(
            Long id,
            Long primarySignRequestId,
            String description,
            String subject,
            String workflowName,
            String createDateLabel,
            String listTitle,
            boolean viewedByCurrentUser,
            boolean hasAttachments,
            List<PostitItem> postits,
            List<SignRequestItem> signRequests
    ) {
    }

    public record SignRequestItem(
            Long id,
            String title,
            String status
    ) {
    }

    public record PostitItem(
            String author,
            String text
    ) {
    }
}


