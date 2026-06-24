package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;
import java.util.Map;

public class AdminFormDetailViewDto {

    private String workflowRole;
    private FormDto form;
    private WorkflowSummaryDto workflow;
    private List<String> roles;
    private List<WorkflowOptionDto> workflowTypes;
    private List<PreFillOptionDto> preFillTypes;
    private List<TagDto> allTags;
    private List<Long> selectedTagIds;
    private DocumentDto document;
    private Map<String, List<String>> preFillTypeOptions;
    private List<SignRequestParamsFrontDto> spots;
    private Map<Integer, Long> srpMap;
    private Integer defaultSignImageNumber;

    public String getWorkflowRole() {
        return workflowRole;
    }

    public void setWorkflowRole(String workflowRole) {
        this.workflowRole = workflowRole;
    }

    public FormDto getForm() {
        return form;
    }

    public void setForm(FormDto form) {
        this.form = form;
    }

    public WorkflowSummaryDto getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowSummaryDto workflow) {
        this.workflow = workflow;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<WorkflowOptionDto> getWorkflowTypes() {
        return workflowTypes;
    }

    public void setWorkflowTypes(List<WorkflowOptionDto> workflowTypes) {
        this.workflowTypes = workflowTypes;
    }

    public List<PreFillOptionDto> getPreFillTypes() {
        return preFillTypes;
    }

    public void setPreFillTypes(List<PreFillOptionDto> preFillTypes) {
        this.preFillTypes = preFillTypes;
    }

    public List<TagDto> getAllTags() {
        return allTags;
    }

    public void setAllTags(List<TagDto> allTags) {
        this.allTags = allTags;
    }

    public List<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public void setSelectedTagIds(List<Long> selectedTagIds) {
        this.selectedTagIds = selectedTagIds;
    }

    public DocumentDto getDocument() {
        return document;
    }

    public void setDocument(DocumentDto document) {
        this.document = document;
    }

    public Map<String, List<String>> getPreFillTypeOptions() {
        return preFillTypeOptions;
    }

    public void setPreFillTypeOptions(Map<String, List<String>> preFillTypeOptions) {
        this.preFillTypeOptions = preFillTypeOptions;
    }

    public List<SignRequestParamsFrontDto> getSpots() {
        return spots;
    }

    public void setSpots(List<SignRequestParamsFrontDto> spots) {
        this.spots = spots;
    }

    public Map<Integer, Long> getSrpMap() {
        return srpMap;
    }

    public void setSrpMap(Map<Integer, Long> srpMap) {
        this.srpMap = srpMap;
    }

    public Integer getDefaultSignImageNumber() {
        return defaultSignImageNumber;
    }

    public void setDefaultSignImageNumber(Integer defaultSignImageNumber) {
        this.defaultSignImageNumber = defaultSignImageNumber;
    }

    public static class FormDto {
        private Long id;
        private String title;
        private String name;
        private String description;
        private Boolean isFeatured;
        private Boolean activeVersion;
        private List<ShareType> authorizedShareTypes;
        private Boolean publicUsage;
        private List<String> roles;
        private Boolean hideButton;
        private String preFillType;
        private Boolean pdfDisplay;
        private String action;
        private String message;
        private WorkflowSummaryDto workflow;
        private DocumentDto document;
        private List<FieldDto> fields;
        private List<SignRequestParamsFrontDto> signRequestParams;
        private Boolean deleted;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getIsFeatured() {
            return isFeatured;
        }

        public void setIsFeatured(Boolean featured) {
            isFeatured = featured;
        }

        public Boolean getActiveVersion() {
            return activeVersion;
        }

        public void setActiveVersion(Boolean activeVersion) {
            this.activeVersion = activeVersion;
        }

        public List<ShareType> getAuthorizedShareTypes() {
            return authorizedShareTypes;
        }

        public void setAuthorizedShareTypes(List<ShareType> authorizedShareTypes) {
            this.authorizedShareTypes = authorizedShareTypes;
        }

        public Boolean getPublicUsage() {
            return publicUsage;
        }

        public void setPublicUsage(Boolean publicUsage) {
            this.publicUsage = publicUsage;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public Boolean getHideButton() {
            return hideButton;
        }

        public void setHideButton(Boolean hideButton) {
            this.hideButton = hideButton;
        }

        public String getPreFillType() {
            return preFillType;
        }

        public void setPreFillType(String preFillType) {
            this.preFillType = preFillType;
        }

        public Boolean getPdfDisplay() {
            return pdfDisplay;
        }

        public void setPdfDisplay(Boolean pdfDisplay) {
            this.pdfDisplay = pdfDisplay;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public WorkflowSummaryDto getWorkflow() {
            return workflow;
        }

        public void setWorkflow(WorkflowSummaryDto workflow) {
            this.workflow = workflow;
        }

        public DocumentDto getDocument() {
            return document;
        }

        public void setDocument(DocumentDto document) {
            this.document = document;
        }

        public List<FieldDto> getFields() {
            return fields;
        }

        public void setFields(List<FieldDto> fields) {
            this.fields = fields;
        }

        public List<SignRequestParamsFrontDto> getSignRequestParams() {
            return signRequestParams;
        }

        public void setSignRequestParams(List<SignRequestParamsFrontDto> signRequestParams) {
            this.signRequestParams = signRequestParams;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
    }

    public static class WorkflowSummaryDto {
        private Long id;
        private String description;
        private String mailFrom;
        private List<WorkflowStepDto> workflowSteps;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getMailFrom() {
            return mailFrom;
        }

        public void setMailFrom(String mailFrom) {
            this.mailFrom = mailFrom;
        }

        public List<WorkflowStepDto> getWorkflowSteps() {
            return workflowSteps;
        }

        public void setWorkflowSteps(List<WorkflowStepDto> workflowSteps) {
            this.workflowSteps = workflowSteps;
        }
    }

    public static class WorkflowStepDto {
        private Long id;
        private SignType signType;
        private Boolean allSignToComplete;
        private String name;
        private List<UserDto> users;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public SignType getSignType() {
            return signType;
        }

        public void setSignType(SignType signType) {
            this.signType = signType;
        }

        public Boolean getAllSignToComplete() {
            return allSignToComplete;
        }

        public void setAllSignToComplete(Boolean allSignToComplete) {
            this.allSignToComplete = allSignToComplete;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<UserDto> getUsers() {
            return users;
        }

        public void setUsers(List<UserDto> users) {
            this.users = users;
        }
    }

    public static class UserDto {
        private String firstname;
        private String name;

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class WorkflowOptionDto {
        private Long id;
        private String description;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class PreFillOptionDto {
        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class TagDto {
        private Long id;
        private String name;
        private String color;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class DocumentDto {
        private String fileName;
        private String contentType;

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    public static class FieldDto {
        private Long id;
        private String name;
        private String description;
        private FieldType type;
        private Boolean favorisable;
        private Boolean required;
        private Boolean readOnly;
        private String extValueServiceName;
        private String searchServiceName;
        private String extValueType;
        private String searchType;
        private String extValueReturn;
        private String searchReturn;
        private List<Long> workflowStepIds;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public FieldType getType() {
            return type;
        }

        public void setType(FieldType type) {
            this.type = type;
        }

        public Boolean getFavorisable() {
            return favorisable;
        }

        public void setFavorisable(Boolean favorisable) {
            this.favorisable = favorisable;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public void setReadOnly(Boolean readOnly) {
            this.readOnly = readOnly;
        }

        public String getExtValueServiceName() {
            return extValueServiceName;
        }

        public void setExtValueServiceName(String extValueServiceName) {
            this.extValueServiceName = extValueServiceName;
        }

        public String getSearchServiceName() {
            return searchServiceName;
        }

        public void setSearchServiceName(String searchServiceName) {
            this.searchServiceName = searchServiceName;
        }

        public String getExtValueType() {
            return extValueType;
        }

        public void setExtValueType(String extValueType) {
            this.extValueType = extValueType;
        }

        public String getSearchType() {
            return searchType;
        }

        public void setSearchType(String searchType) {
            this.searchType = searchType;
        }

        public String getExtValueReturn() {
            return extValueReturn;
        }

        public void setExtValueReturn(String extValueReturn) {
            this.extValueReturn = extValueReturn;
        }

        public String getSearchReturn() {
            return searchReturn;
        }

        public void setSearchReturn(String searchReturn) {
            this.searchReturn = searchReturn;
        }

        public List<Long> getWorkflowStepIds() {
            return workflowStepIds;
        }

        public void setWorkflowStepIds(List<Long> workflowStepIds) {
            this.workflowStepIds = workflowStepIds;
        }
    }
}
