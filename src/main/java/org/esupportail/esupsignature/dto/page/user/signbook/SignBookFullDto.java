package org.esupportail.esupsignature.dto.page.user.signbook;

import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class SignBookFullDto {

    private Long id;
    private String subject;
    private String description;
    private String workflowName;
    private String createByDisplayName;
    private String createByEppn;
    private String status;
    private boolean deleted;
    private String archiveStatus;
    private boolean deleteableByCurrentUser;
    private boolean displayNotif;
    private boolean hiddenByCurrentUser;
    private String currentSignType;
    private List<ParticipantStepDto> participantSteps;
    private String endDateLabel;
    private String deletedDateLabel;
    private String lastSignedDocumentDateLabel;
    private String refusedCommentTitle;
    private PrimarySignRequestDto primarySignRequest;
    private List<SignRequestDocumentDto> signRequests;
    private List<PostitDto> postits;
    private boolean editable;
    private Integer liveWorkflowCurrentStepNumber;
    private List<ShowSignRequestDto.SignBookViewerDto> viewers;
    private List<ShowSignRequestDto.StepDto> liveWorkflowSteps;
    private List<ShowSignRequestDto.TargetDto> liveWorkflowTargets;
    private List<TagDto> tags;

    public SignBookFullDto() {
    }

    public SignBookFullDto(Long id, String subject, String description, String workflowName,
                           String createByDisplayName, String createByEppn, String status, boolean deleted,
                           String archiveStatus, boolean deleteableByCurrentUser, boolean displayNotif,
                           boolean hiddenByCurrentUser, String currentSignType,
                           List<ParticipantStepDto> participantSteps, String endDateLabel,
                           String deletedDateLabel, String lastSignedDocumentDateLabel,
                           String refusedCommentTitle, PrimarySignRequestDto primarySignRequest,
                           List<SignRequestDocumentDto> signRequests, List<PostitDto> postits,
                           boolean editable, Integer liveWorkflowCurrentStepNumber,
                           List<ShowSignRequestDto.SignBookViewerDto> viewers,
                           List<ShowSignRequestDto.StepDto> liveWorkflowSteps,
                           List<ShowSignRequestDto.TargetDto> liveWorkflowTargets,
                           List<TagDto> tags) {
        this.id = id;
        this.subject = subject;
        this.description = description;
        this.workflowName = workflowName;
        this.createByDisplayName = createByDisplayName;
        this.createByEppn = createByEppn;
        this.status = status;
        this.deleted = deleted;
        this.archiveStatus = archiveStatus;
        this.deleteableByCurrentUser = deleteableByCurrentUser;
        this.displayNotif = displayNotif;
        this.hiddenByCurrentUser = hiddenByCurrentUser;
        this.currentSignType = currentSignType;
        this.participantSteps = participantSteps;
        this.endDateLabel = endDateLabel;
        this.deletedDateLabel = deletedDateLabel;
        this.lastSignedDocumentDateLabel = lastSignedDocumentDateLabel;
        this.refusedCommentTitle = refusedCommentTitle;
        this.primarySignRequest = primarySignRequest;
        this.signRequests = signRequests;
        this.postits = postits;
        this.editable = editable;
        this.liveWorkflowCurrentStepNumber = liveWorkflowCurrentStepNumber;
        this.viewers = viewers;
        this.liveWorkflowSteps = liveWorkflowSteps;
        this.liveWorkflowTargets = liveWorkflowTargets;
        this.tags = tags;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public String getCreateByDisplayName() { return createByDisplayName; }
    public void setCreateByDisplayName(String createByDisplayName) { this.createByDisplayName = createByDisplayName; }
    public String getCreateByEppn() { return createByEppn; }
    public void setCreateByEppn(String createByEppn) { this.createByEppn = createByEppn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public String getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
    public boolean isDeleteableByCurrentUser() { return deleteableByCurrentUser; }
    public void setDeleteableByCurrentUser(boolean deleteableByCurrentUser) { this.deleteableByCurrentUser = deleteableByCurrentUser; }
    public boolean isDisplayNotif() { return displayNotif; }
    public void setDisplayNotif(boolean displayNotif) { this.displayNotif = displayNotif; }
    public boolean isHiddenByCurrentUser() { return hiddenByCurrentUser; }
    public void setHiddenByCurrentUser(boolean hiddenByCurrentUser) { this.hiddenByCurrentUser = hiddenByCurrentUser; }
    public String getCurrentSignType() { return currentSignType; }
    public void setCurrentSignType(String currentSignType) { this.currentSignType = currentSignType; }
    public List<ParticipantStepDto> getParticipantSteps() { return participantSteps; }
    public void setParticipantSteps(List<ParticipantStepDto> participantSteps) { this.participantSteps = participantSteps; }
    public String getEndDateLabel() { return endDateLabel; }
    public void setEndDateLabel(String endDateLabel) { this.endDateLabel = endDateLabel; }
    public String getDeletedDateLabel() { return deletedDateLabel; }
    public void setDeletedDateLabel(String deletedDateLabel) { this.deletedDateLabel = deletedDateLabel; }
    public String getLastSignedDocumentDateLabel() { return lastSignedDocumentDateLabel; }
    public void setLastSignedDocumentDateLabel(String lastSignedDocumentDateLabel) { this.lastSignedDocumentDateLabel = lastSignedDocumentDateLabel; }
    public String getRefusedCommentTitle() { return refusedCommentTitle; }
    public void setRefusedCommentTitle(String refusedCommentTitle) { this.refusedCommentTitle = refusedCommentTitle; }
    public PrimarySignRequestDto getPrimarySignRequest() { return primarySignRequest; }
    public void setPrimarySignRequest(PrimarySignRequestDto primarySignRequest) { this.primarySignRequest = primarySignRequest; }
    public List<SignRequestDocumentDto> getSignRequests() { return signRequests; }
    public void setSignRequests(List<SignRequestDocumentDto> signRequests) { this.signRequests = signRequests; }
    public List<PostitDto> getPostits() { return postits; }
    public void setPostits(List<PostitDto> postits) { this.postits = postits; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public Integer getLiveWorkflowCurrentStepNumber() { return liveWorkflowCurrentStepNumber; }
    public void setLiveWorkflowCurrentStepNumber(Integer liveWorkflowCurrentStepNumber) { this.liveWorkflowCurrentStepNumber = liveWorkflowCurrentStepNumber; }
    public List<ShowSignRequestDto.SignBookViewerDto> getViewers() { return viewers; }
    public void setViewers(List<ShowSignRequestDto.SignBookViewerDto> viewers) { this.viewers = viewers; }
    public List<ShowSignRequestDto.StepDto> getLiveWorkflowSteps() { return liveWorkflowSteps; }
    public void setLiveWorkflowSteps(List<ShowSignRequestDto.StepDto> liveWorkflowSteps) { this.liveWorkflowSteps = liveWorkflowSteps; }
    public List<ShowSignRequestDto.TargetDto> getLiveWorkflowTargets() { return liveWorkflowTargets; }
    public void setLiveWorkflowTargets(List<ShowSignRequestDto.TargetDto> liveWorkflowTargets) { this.liveWorkflowTargets = liveWorkflowTargets; }
    public List<TagDto> getTags() { return tags; }
    public void setTags(List<TagDto> tags) { this.tags = tags; }

    public Long id() { return id; }
    public String subject() { return subject; }
    public String description() { return description; }
    public String workflowName() { return workflowName; }
    public String createByDisplayName() { return createByDisplayName; }
    public String createByEppn() { return createByEppn; }
    public String status() { return status; }
    public boolean deleted() { return deleted; }
    public String archiveStatus() { return archiveStatus; }
    public boolean deleteableByCurrentUser() { return deleteableByCurrentUser; }
    public boolean displayNotif() { return displayNotif; }
    public boolean hiddenByCurrentUser() { return hiddenByCurrentUser; }
    public String currentSignType() { return currentSignType; }
    public List<ParticipantStepDto> participantSteps() { return participantSteps; }
    public String endDateLabel() { return endDateLabel; }
    public String deletedDateLabel() { return deletedDateLabel; }
    public String lastSignedDocumentDateLabel() { return lastSignedDocumentDateLabel; }
    public String refusedCommentTitle() { return refusedCommentTitle; }
    public PrimarySignRequestDto primarySignRequest() { return primarySignRequest; }
    public List<SignRequestDocumentDto> signRequests() { return signRequests; }
    public List<PostitDto> postits() { return postits; }
    public boolean editable() { return editable; }
    public Integer liveWorkflowCurrentStepNumber() { return liveWorkflowCurrentStepNumber; }
    public List<ShowSignRequestDto.SignBookViewerDto> viewers() { return viewers; }
    public List<ShowSignRequestDto.StepDto> liveWorkflowSteps() { return liveWorkflowSteps; }
    public List<ShowSignRequestDto.TargetDto> liveWorkflowTargets() { return liveWorkflowTargets; }
    public List<TagDto> tags() { return tags; }

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

    public List<TagDto> getApplicationTags() {
        if (tags == null) return List.of();
        Map<String, TagDto> uniqueRoots = new LinkedHashMap<>();
        for (TagDto t : tags) {
            String root = t.getRootName();
            if (root != null && !uniqueRoots.containsKey(root)) {
                uniqueRoots.put(root, new TagDto(null, root, t.getColor(), null, null, root));
            }
        }
        return new ArrayList<>(uniqueRoots.values());
    }

    public List<TagDto> getTagsForGroup(String groupName) {
        if (tags == null || groupName == null) return List.of();
        List<TagDto> groupTags = new ArrayList<>();
        for (TagDto t : tags) {
            if (groupName.equalsIgnoreCase(t.getParentName())) {
                groupTags.add(t);
            }
        }
        return groupTags;
    }

    // -------------------------------------------------------------------
    // Inner DTO : Tag
    // -------------------------------------------------------------------
    public static class TagDto {
        private Long id;
        private String name;
        private String color;
        private String groupColor;   // couleur du groupe parent (pour l'affichage en badge)
        private String parentName;   // nom du parent direct (= groupe)
        private String rootName;     // nom du tag racine (= thème/application)

        public TagDto() {}

        public TagDto(Long id, String name, String color, String groupColor, String parentName, String rootName) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.groupColor = groupColor;
            this.parentName = parentName;
            this.rootName = rootName;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getGroupColor() { return groupColor; }
        public void setGroupColor(String groupColor) { this.groupColor = groupColor; }
        public String getParentName() { return parentName; }
        public void setParentName(String parentName) { this.parentName = parentName; }
        public String getRootName() { return rootName; }
        public void setRootName(String rootName) { this.rootName = rootName; }
    }

    public ParticipantStepDto getCurrentParticipantStep() {
        if (liveWorkflowCurrentStepNumber == null || participantSteps == null || participantSteps.isEmpty()) {
            return null;
        }
        return participantSteps.stream()
                .filter(participantStep -> participantStep != null && Objects.equals(participantStep.getStepNumber(), liveWorkflowCurrentStepNumber))
                .findFirst()
                .orElse(null);
    }

    public boolean hasCurrentParticipantStep() {
        return getCurrentParticipantStep() != null;
    }

    public static class PrimarySignRequestDto {
        private Long id;
        private String title;
        private String status;
        private String createDateLabel;
        private boolean viewedByCurrentUser;
        private boolean hasAttachments;
        private boolean deleted;
        private String rowTitle;
        private boolean canDownload;
        private boolean canDownloadAll;
        private Long paperlessSignedDocumentId;

        public PrimarySignRequestDto() {
        }

        public PrimarySignRequestDto(Long id, String title, String status, String createDateLabel,
                                     boolean viewedByCurrentUser, boolean hasAttachments, boolean deleted,
                                     String rowTitle, boolean canDownload, boolean canDownloadAll,
                                     Long paperlessSignedDocumentId) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.createDateLabel = createDateLabel;
            this.viewedByCurrentUser = viewedByCurrentUser;
            this.hasAttachments = hasAttachments;
            this.deleted = deleted;
            this.rowTitle = rowTitle;
            this.canDownload = canDownload;
            this.canDownloadAll = canDownloadAll;
            this.paperlessSignedDocumentId = paperlessSignedDocumentId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreateDateLabel() { return createDateLabel; }
        public void setCreateDateLabel(String createDateLabel) { this.createDateLabel = createDateLabel; }
        public boolean isViewedByCurrentUser() { return viewedByCurrentUser; }
        public void setViewedByCurrentUser(boolean viewedByCurrentUser) { this.viewedByCurrentUser = viewedByCurrentUser; }
        public boolean isHasAttachments() { return hasAttachments; }
        public void setHasAttachments(boolean hasAttachments) { this.hasAttachments = hasAttachments; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
        public String getRowTitle() { return rowTitle; }
        public void setRowTitle(String rowTitle) { this.rowTitle = rowTitle; }
        public boolean isCanDownload() { return canDownload; }
        public void setCanDownload(boolean canDownload) { this.canDownload = canDownload; }
        public boolean isCanDownloadAll() { return canDownloadAll; }
        public void setCanDownloadAll(boolean canDownloadAll) { this.canDownloadAll = canDownloadAll; }
        public Long getPaperlessSignedDocumentId() { return paperlessSignedDocumentId; }
        public void setPaperlessSignedDocumentId(Long paperlessSignedDocumentId) { this.paperlessSignedDocumentId = paperlessSignedDocumentId; }

        public Long id() { return id; }
        public String title() { return title; }
        public String status() { return status; }
        public String createDateLabel() { return createDateLabel; }
        public boolean viewedByCurrentUser() { return viewedByCurrentUser; }
        public boolean hasAttachments() { return hasAttachments; }
        public boolean deleted() { return deleted; }
        public String rowTitle() { return rowTitle; }
        public boolean canDownload() { return canDownload; }
        public boolean canDownloadAll() { return canDownloadAll; }
    }

    public static class SignRequestDocumentDto {
        private Long id;
        private String title;
        private String status;
        private String fileName;
        private String createDateLabel;
        private String createByDisplayName;
        private String createByEppn;

        public SignRequestDocumentDto() {
        }

        public SignRequestDocumentDto(Long id, String title, String status, String fileName,
                                      String createDateLabel, String createByDisplayName,
                                      String createByEppn) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.fileName = fileName;
            this.createDateLabel = createDateLabel;
            this.createByDisplayName = createByDisplayName;
            this.createByEppn = createByEppn;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getCreateDateLabel() { return createDateLabel; }
        public void setCreateDateLabel(String createDateLabel) { this.createDateLabel = createDateLabel; }
        public String getCreateByDisplayName() { return createByDisplayName; }
        public void setCreateByDisplayName(String createByDisplayName) { this.createByDisplayName = createByDisplayName; }
        public String getCreateByEppn() { return createByEppn; }
        public void setCreateByEppn(String createByEppn) { this.createByEppn = createByEppn; }

        public Long id() { return id; }
        public String title() { return title; }
        public String status() { return status; }
        public String fileName() { return fileName; }
        public String createDateLabel() { return createDateLabel; }
        public String createByDisplayName() { return createByDisplayName; }
        public String createByEppn() { return createByEppn; }
    }

    public static class PostitDto {
        private String author;
        private String text;

        public PostitDto() {
        }

        public PostitDto(String author, String text) {
            this.author = author;
            this.text = text;
        }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String author() { return author; }
        public String text() { return text; }
    }

    public static class ParticipantStepDto {
        private Integer stepNumber;
        private List<ParticipantDto> recipients;

        public ParticipantStepDto() {
        }

        public ParticipantStepDto(Integer stepNumber, List<ParticipantDto> recipients) {
            this.stepNumber = stepNumber;
            this.recipients = recipients;
        }

        public Integer getStepNumber() { return stepNumber; }
        public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }
        public List<ParticipantDto> getRecipients() { return recipients; }
        public void setRecipients(List<ParticipantDto> recipients) { this.recipients = recipients; }

        public String getRecipientsSummary() {
            if (recipients == null || recipients.isEmpty()) {
                return null;
            }
            StringJoiner stringJoiner = new StringJoiner(", ");
            recipients.stream()
                    .filter(Objects::nonNull)
                    .map(recipient -> recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getEmail())
                    .filter(Objects::nonNull)
                    .forEach(stringJoiner::add);
            String summary = stringJoiner.toString();
            return summary.isEmpty() ? null : summary;
        }

        public String getStatusKey() {
            if (recipients == null || recipients.isEmpty()) {
                return null;
            }
            String resolvedStatusKey = null;
            int resolvedPriority = -1;
            for (ParticipantDto recipient : recipients) {
                if (recipient == null || recipient.getStatusKey() == null) {
                    continue;
                }
                int candidatePriority = getStatusPriority(recipient.getStatusKey());
                if (candidatePriority > resolvedPriority) {
                    resolvedPriority = candidatePriority;
                    resolvedStatusKey = recipient.getStatusKey();
                }
            }
            return resolvedStatusKey;
        }

        private int getStatusPriority(String statusKey) {
            return switch (statusKey) {
                case "refused" -> 4;
                case "pending" -> 3;
                case "notSigned" -> 2;
                case "signed" -> 1;
                default -> 0;
            };
        }

        public Integer stepNumber() { return stepNumber; }
        public List<ParticipantDto> recipients() { return recipients; }
    }

    public static class ParticipantDto {
        private String email;
        private String displayName;
        private String statusKey;

        public ParticipantDto() {
        }

        public ParticipantDto(String email, String displayName, String statusKey) {
            this.email = email;
            this.displayName = displayName;
            this.statusKey = statusKey;
        }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getStatusKey() { return statusKey; }
        public void setStatusKey(String statusKey) { this.statusKey = statusKey; }

        public String email() { return email; }
        public String displayName() { return displayName; }
        public String statusKey() { return statusKey; }
    }
}
