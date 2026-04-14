package org.esupportail.esupsignature.dto.view.signrequest;

import org.esupportail.esupsignature.config.certificat.SealCertificatProperties;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.SignWith;
import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ShowSignRequestBackDto(
        SignBookMetaDto signBook,
        RequestMetaDto request,
        SignRequestUiCommonDto common,
        WorkflowMetaDto workflow,
        String urlProfil,
        Boolean displayNotif,
        Boolean tempUsers,
        Boolean lastStep,
        Boolean currentUserAsSigned,
        List<String> signatureIds,
        Boolean signatureIssue,
        Set<String> supervisors,
        String toSignDocumentContentType,
        List<Comment> postits,
        List<AttachmentDto> attachments,
        List<DocumentDto> originalDocuments,
        List<DocumentDto> signedDocuments,
        String exportedDocumentURI,
        String lastSignedDocumentContentType,
        Boolean hasNextSignBook,
        Long nextSignRequestId,
        List<SignWith> signWiths,
        AuditTrail auditTrail,
        String size,
        Boolean sealCertOK,
        List<SealCertificatProperties> sealCertificatPropertieses,
        String annotation,
        List<StepDto> steps,
        List<TargetDto> targets,
        Map<Long, RecipientActionDto> recipientActions,
        List<SignRequestTabDto> signRequestTabs,
        Integer liveWorkflowStepCount,
        Boolean viewedByCurrentUser,
        Boolean viewRight,
        Boolean frameMode,
        List<Log> logs,
        String pdfaCheck,
        Boolean auditTrailChecked,
        List<RecipientWsDto> externalsRecipients
) {
        public record SignBookMetaDto(
                Long id,
                String workflowName,
                String description,
                SignRequestStatus status,
                Boolean deleted,
                ArchiveStatus archiveStatus,
                Date createDate,
                List<SignBookViewerDto> viewers
        ) {}

        public record SignBookViewerDto(
                Long id,
                String firstname,
                String name,
                String email
        ) {}

        public record WorkflowMetaDto(
                Boolean hasWorkflow,
                Boolean externalCanReaderAnnotations,
                Boolean disableSidebarForExternal,
                Boolean externalCanReaderAttachments,
                Boolean externalCanEdit,
                Boolean externalCanEditAttachments,
                Boolean authorizeClone,
                Boolean forbidDownloadsBeforeEnd,
                Boolean sendAlertToAllRecipients,
                Integer workflowStepCount,
                String mailFrom
        ) {}

        public record RequestMetaDto(
                Long id,
                SignRequestStatus status,
                Boolean deleted,
                String token,
                RequestUserDto createBy,
                List<String> links
        ) {}

        public record RequestUserDto(
                Long id,
                String eppn,
                String firstname,
                String name
        ) {}

        public record SignRequestTabDto(
                Long id,
                String title,
                SignRequestStatus status,
                Boolean deleted
        ) {}

        public record AttachmentDto(
                Long id,
                String fileName,
                AttachmentUserDto createBy
        ) {}

        public record AttachmentUserDto(
                String eppn,
                String firstname,
                String name
        ) {}

        public record DocumentDto(
                Long id,
                String fileName,
                Long size,
                String contentType
        ) {}

        public record StepDto(
                Long id,
                String description,
                Boolean changeable,
                SignType signType,
                Boolean autoSign,
                Boolean allSignToComplete,
                Boolean repeatable,
                List<StepUserDto> users,
                List<StepRecipientDto> recipients
        ) {}

        public record StepRecipientDto(
                Long id,
                StepUserDto user
        ) {}

        public record StepUserDto(
                Long id,
                String firstname,
                String name,
                String email,
                String phone,
                String hidedPhone,
                UserType userType
        ) {}

        public record TargetDto(
                String targetUri,
                String protectedTargetUri,
                Boolean targetOk
        ) {}

        public record RecipientActionDto(
                ActionType actionType,
                Date date
        ) {}
}

