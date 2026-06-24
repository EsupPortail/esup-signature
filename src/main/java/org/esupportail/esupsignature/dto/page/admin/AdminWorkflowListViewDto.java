package org.esupportail.esupsignature.dto.page.admin;

import java.util.List;

public class AdminWorkflowListViewDto {

    private String workflowRole;
    private String displayWorkflowType;
    private List<Long> selectedTagIds;
    private List<TagDto> allTags;
    private List<String> roles;
    private List<RowDto> workflows;

    public String getWorkflowRole() { return workflowRole; }
    public void setWorkflowRole(String workflowRole) { this.workflowRole = workflowRole; }
    public String getDisplayWorkflowType() { return displayWorkflowType; }
    public void setDisplayWorkflowType(String displayWorkflowType) { this.displayWorkflowType = displayWorkflowType; }
    public List<Long> getSelectedTagIds() { return selectedTagIds; }
    public void setSelectedTagIds(List<Long> selectedTagIds) { this.selectedTagIds = selectedTagIds; }
    public List<TagDto> getAllTags() { return allTags; }
    public void setAllTags(List<TagDto> allTags) { this.allTags = allTags; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    public List<RowDto> getWorkflows() { return workflows; }
    public void setWorkflows(List<RowDto> workflows) { this.workflows = workflows; }

    public String workflowRole() { return workflowRole; }
    public String displayWorkflowType() { return displayWorkflowType; }
    public List<Long> selectedTagIds() { return selectedTagIds; }
    public List<TagDto> allTags() { return allTags; }
    public List<String> roles() { return roles; }
    public List<RowDto> workflows() { return workflows; }

    public static class RowDto {
        private Long id;
        private String description;
        private List<TagDto> tags;
        private boolean featured;
        private boolean publicUsage;
        private List<String> roles;
        private String createByEppn;
        private List<StepDto> workflowSteps;
        private boolean documentsSourceUriPresent;
        private boolean fromCode;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<TagDto> getTags() { return tags; }
        public void setTags(List<TagDto> tags) { this.tags = tags; }
        public boolean isFeatured() { return featured; }
        public void setFeatured(boolean featured) { this.featured = featured; }
        public boolean isPublicUsage() { return publicUsage; }
        public void setPublicUsage(boolean publicUsage) { this.publicUsage = publicUsage; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public String getCreateByEppn() { return createByEppn; }
        public void setCreateByEppn(String createByEppn) { this.createByEppn = createByEppn; }
        public List<StepDto> getWorkflowSteps() { return workflowSteps; }
        public void setWorkflowSteps(List<StepDto> workflowSteps) { this.workflowSteps = workflowSteps; }
        public boolean isDocumentsSourceUriPresent() { return documentsSourceUriPresent; }
        public void setDocumentsSourceUriPresent(boolean documentsSourceUriPresent) { this.documentsSourceUriPresent = documentsSourceUriPresent; }
        public boolean isFromCode() { return fromCode; }
        public void setFromCode(boolean fromCode) { this.fromCode = fromCode; }

        public Long id() { return id; }
        public String description() { return description; }
        public List<TagDto> tags() { return tags; }
        public boolean featured() { return featured; }
        public boolean publicUsage() { return publicUsage; }
        public List<String> roles() { return roles; }
        public String createByEppn() { return createByEppn; }
        public List<StepDto> workflowSteps() { return workflowSteps; }
        public boolean documentsSourceUriPresent() { return documentsSourceUriPresent; }
        public boolean fromCode() { return fromCode; }
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

    public static class StepDto {
        private int index;
        private List<UserDto> users;
        private boolean changeable;
        private boolean autoSign;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public List<UserDto> getUsers() { return users; }
        public void setUsers(List<UserDto> users) { this.users = users; }
        public boolean isChangeable() { return changeable; }
        public void setChangeable(boolean changeable) { this.changeable = changeable; }
        public boolean isAutoSign() { return autoSign; }
        public void setAutoSign(boolean autoSign) { this.autoSign = autoSign; }

        public int index() { return index; }
        public List<UserDto> users() { return users; }
        public boolean changeable() { return changeable; }
        public boolean autoSign() { return autoSign; }
    }

    public static class UserDto {
        private String firstname;
        private String name;

        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String firstname() { return firstname; }
        public String name() { return name; }
    }
}
