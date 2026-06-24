package org.esupportail.esupsignature.dto.page.admin;

import java.util.List;

public class AdminFormListViewDto {

    private String workflowRole;
    private Boolean activeVersion;
    private List<Long> selectedTagIds;
    private List<TagDto> allTags;
    private List<String> roles;
    private List<WorkflowOptionDto> workflowTypes;
    private List<PreFillOptionDto> preFillTypes;
    private List<RowDto> forms;

    public String getWorkflowRole() { return workflowRole; }
    public void setWorkflowRole(String workflowRole) { this.workflowRole = workflowRole; }
    public Boolean getActiveVersion() { return activeVersion; }
    public void setActiveVersion(Boolean activeVersion) { this.activeVersion = activeVersion; }
    public List<Long> getSelectedTagIds() { return selectedTagIds; }
    public void setSelectedTagIds(List<Long> selectedTagIds) { this.selectedTagIds = selectedTagIds; }
    public List<TagDto> getAllTags() { return allTags; }
    public void setAllTags(List<TagDto> allTags) { this.allTags = allTags; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<WorkflowOptionDto> getWorkflowTypes() { return workflowTypes; }
    public void setWorkflowTypes(List<WorkflowOptionDto> workflowTypes) { this.workflowTypes = workflowTypes; }
    public List<PreFillOptionDto> getPreFillTypes() { return preFillTypes; }
    public void setPreFillTypes(List<PreFillOptionDto> preFillTypes) { this.preFillTypes = preFillTypes; }
    public List<RowDto> getForms() { return forms; }
    public void setForms(List<RowDto> forms) { this.forms = forms; }

    public String workflowRole() { return workflowRole; }
    public Boolean activeVersion() { return activeVersion; }
    public List<Long> selectedTagIds() { return selectedTagIds; }
    public List<TagDto> allTags() { return allTags; }
    public List<String> roles() { return roles; }
    public List<WorkflowOptionDto> workflowTypes() { return workflowTypes; }
    public List<PreFillOptionDto> preFillTypes() { return preFillTypes; }
    public List<RowDto> forms() { return forms; }

    public static class RowDto {
        private Long id;
        private String name;
        private String title;
        private List<TagDto> tags;
        private boolean featured;
        private WorkflowOptionDto workflow;
        private boolean activeVersion;
        private boolean hideButton;
        private boolean deleted;
        private boolean publicUsage;
        private List<String> roles;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<TagDto> getTags() { return tags; }
        public void setTags(List<TagDto> tags) { this.tags = tags; }
        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean featured) { this.featured = featured; }
        public WorkflowOptionDto getWorkflow() { return workflow; }
        public void setWorkflow(WorkflowOptionDto workflow) { this.workflow = workflow; }
        public boolean isActiveVersion() { return activeVersion; }
        public void setActiveVersion(boolean activeVersion) { this.activeVersion = activeVersion; }
        public boolean isHideButton() { return hideButton; }
        public void setHideButton(boolean hideButton) { this.hideButton = hideButton; }
        public boolean isDeleted() { return deleted; }
        public void setDeleted(boolean deleted) { this.deleted = deleted; }
        public boolean isPublicUsage() { return publicUsage; }
        public void setPublicUsage(boolean publicUsage) { this.publicUsage = publicUsage; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public Long id() { return id; }
        public String name() { return name; }
        public String title() { return title; }
        public List<TagDto> tags() { return tags; }
        public boolean featured() { return featured; }
        public WorkflowOptionDto workflow() { return workflow; }
        public boolean activeVersion() { return activeVersion; }
        public boolean hideButton() { return hideButton; }
        public boolean deleted() { return deleted; }
        public boolean publicUsage() { return publicUsage; }
        public List<String> roles() { return roles; }
    }

    public static class WorkflowOptionDto {
        private Long id;
        private String description;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Long id() { return id; }
        public String description() { return description; }
    }

    public static class PreFillOptionDto {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String name() { return name; }
        public String description() { return description; }
    }

    public static class TagDto {
        private Long id;
        private String name;
        private String color;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public Long id() { return id; }
        public String name() { return name; }
        public String color() { return color; }
    }
}
