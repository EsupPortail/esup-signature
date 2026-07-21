package org.esupportail.esupsignature.dto.ui.global;

import java.util.List;

public class UiHomeDto {

    private Long startFormId;
    private Long startWorkflowId;
    private String warningReadUrl;
    private String searchUrl;
    private String searchTitlesUrl;
    private List<SignBookItem> toSignSignBooks;
    private List<SignBookItem> pendingSignBooks;

    public UiHomeDto() {
    }

    public UiHomeDto(Long startFormId, Long startWorkflowId, String warningReadUrl, String searchUrl,
                     String searchTitlesUrl, List<SignBookItem> toSignSignBooks,
                     List<SignBookItem> pendingSignBooks) {
        this.startFormId = startFormId;
        this.startWorkflowId = startWorkflowId;
        this.warningReadUrl = warningReadUrl;
        this.searchUrl = searchUrl;
        this.searchTitlesUrl = searchTitlesUrl;
        this.toSignSignBooks = toSignSignBooks;
        this.pendingSignBooks = pendingSignBooks;
    }

    public Long getStartFormId() { return startFormId; }
    public void setStartFormId(Long startFormId) { this.startFormId = startFormId; }
    public Long getStartWorkflowId() { return startWorkflowId; }
    public void setStartWorkflowId(Long startWorkflowId) { this.startWorkflowId = startWorkflowId; }
    public String getWarningReadUrl() { return warningReadUrl; }
    public void setWarningReadUrl(String warningReadUrl) { this.warningReadUrl = warningReadUrl; }
    public String getSearchUrl() { return searchUrl; }
    public void setSearchUrl(String searchUrl) { this.searchUrl = searchUrl; }
    public String getSearchTitlesUrl() { return searchTitlesUrl; }
    public void setSearchTitlesUrl(String searchTitlesUrl) { this.searchTitlesUrl = searchTitlesUrl; }
    public List<SignBookItem> getToSignSignBooks() { return toSignSignBooks; }
    public void setToSignSignBooks(List<SignBookItem> toSignSignBooks) { this.toSignSignBooks = toSignSignBooks; }
    public List<SignBookItem> getPendingSignBooks() { return pendingSignBooks; }
    public void setPendingSignBooks(List<SignBookItem> pendingSignBooks) { this.pendingSignBooks = pendingSignBooks; }

    public Long startFormId() { return startFormId; }
    public Long startWorkflowId() { return startWorkflowId; }
    public String warningReadUrl() { return warningReadUrl; }
    public String searchUrl() { return searchUrl; }
    public String searchTitlesUrl() { return searchTitlesUrl; }
    public List<SignBookItem> toSignSignBooks() { return toSignSignBooks; }
    public List<SignBookItem> pendingSignBooks() { return pendingSignBooks; }

    public static class SignBookItem {
        private Long id;
        private Long primarySignRequestId;
        private String description;
        private String subject;
        private String workflowName;
        private String createDateLabel;
        private String listTitle;
        private int signRequestCount;
        private boolean viewedByCurrentUser;
        private boolean hasAttachments;
        private List<PostitItem> postits;
        private List<SignRequestItem> signRequests;

        public SignBookItem() {
        }

        public SignBookItem(Long id, Long primarySignRequestId, String description, String subject,
                            String workflowName, String createDateLabel, String listTitle,
                            boolean viewedByCurrentUser, boolean hasAttachments, List<PostitItem> postits,
                            List<SignRequestItem> signRequests) {
            this.id = id;
            this.primarySignRequestId = primarySignRequestId;
            this.description = description;
            this.subject = subject;
            this.workflowName = workflowName;
            this.createDateLabel = createDateLabel;
            this.listTitle = listTitle;
            this.viewedByCurrentUser = viewedByCurrentUser;
            this.hasAttachments = hasAttachments;
            this.postits = postits;
            this.signRequests = signRequests;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getPrimarySignRequestId() { return primarySignRequestId; }
        public void setPrimarySignRequestId(Long primarySignRequestId) { this.primarySignRequestId = primarySignRequestId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public String getCreateDateLabel() { return createDateLabel; }
        public void setCreateDateLabel(String createDateLabel) { this.createDateLabel = createDateLabel; }
        public String getListTitle() { return listTitle; }
        public void setListTitle(String listTitle) { this.listTitle = listTitle; }
        public int getSignRequestCount() { return signRequestCount; }
        public void setSignRequestCount(int signRequestCount) { this.signRequestCount = signRequestCount; }
        public boolean isViewedByCurrentUser() { return viewedByCurrentUser; }
        public void setViewedByCurrentUser(boolean viewedByCurrentUser) { this.viewedByCurrentUser = viewedByCurrentUser; }
        public boolean isHasAttachments() { return hasAttachments; }
        public void setHasAttachments(boolean hasAttachments) { this.hasAttachments = hasAttachments; }
        public List<PostitItem> getPostits() { return postits; }
        public void setPostits(List<PostitItem> postits) { this.postits = postits; }
        public List<SignRequestItem> getSignRequests() { return signRequests; }
        public void setSignRequests(List<SignRequestItem> signRequests) { this.signRequests = signRequests; }

        public Long id() { return id; }
        public Long primarySignRequestId() { return primarySignRequestId; }
        public String description() { return description; }
        public String subject() { return subject; }
        public String workflowName() { return workflowName; }
        public String createDateLabel() { return createDateLabel; }
        public String listTitle() { return listTitle; }
        public int signRequestCount() { return signRequestCount; }
        public boolean viewedByCurrentUser() { return viewedByCurrentUser; }
        public boolean hasAttachments() { return hasAttachments; }
        public List<PostitItem> postits() { return postits; }
        public List<SignRequestItem> signRequests() { return signRequests; }
    }

    public static class SignRequestItem {
        private Long id;
        private String title;
        private String status;

        public SignRequestItem() {
        }

        public SignRequestItem(Long id, String title, String status) {
            this.id = id;
            this.title = title;
            this.status = status;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long id() { return id; }
        public String title() { return title; }
        public String status() { return status; }
    }

    public static class PostitItem {
        private String author;
        private String text;

        public PostitItem() {
        }

        public PostitItem(String author, String text) {
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
}
