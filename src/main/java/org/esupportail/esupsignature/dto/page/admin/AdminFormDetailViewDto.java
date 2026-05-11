package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestParamsFrontDto;
import org.esupportail.esupsignature.entity.enums.FieldType;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.SignType;

import java.util.List;
import java.util.Map;

public class AdminFormDetailViewDto {

    private final String workflowRole;
    private final FormDto form;
    private final WorkflowSummaryDto workflow;
    private final List<String> roles;
    private final List<WorkflowOptionDto> workflowTypes;
    private final List<PreFillOptionDto> preFillTypes;
    private final List<TagDto> allTags;
    private final List<Long> selectedTagIds;
    private final DocumentDto document;
    private final Map<String, List<String>> preFillTypeOptions;
    private final List<SignRequestParamsFrontDto> spots;
    private final Map<Integer, Long> srpMap;
    private final Integer defaultSignImageNumber;

    public AdminFormDetailViewDto(String workflowRole,
                                  FormDto form,
                                  WorkflowSummaryDto workflow,
                                  List<String> roles,
                                  List<WorkflowOptionDto> workflowTypes,
                                  List<PreFillOptionDto> preFillTypes,
                                  List<TagDto> allTags,
                                  List<Long> selectedTagIds,
                                  DocumentDto document,
                                  Map<String, List<String>> preFillTypeOptions,
                                  List<SignRequestParamsFrontDto> spots,
                                  Map<Integer, Long> srpMap,
                                  Integer defaultSignImageNumber) {
        this.workflowRole = workflowRole;
        this.form = form;
        this.workflow = workflow;
        this.roles = roles;
        this.workflowTypes = workflowTypes;
        this.preFillTypes = preFillTypes;
        this.allTags = allTags;
        this.selectedTagIds = selectedTagIds;
        this.document = document;
        this.preFillTypeOptions = preFillTypeOptions;
        this.spots = spots;
        this.srpMap = srpMap;
        this.defaultSignImageNumber = defaultSignImageNumber;
    }

    public String getWorkflowRole() {
        return workflowRole;
    }

    public FormDto getForm() {
        return form;
    }

    public WorkflowSummaryDto getWorkflow() {
        return workflow;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<WorkflowOptionDto> getWorkflowTypes() {
        return workflowTypes;
    }

    public List<PreFillOptionDto> getPreFillTypes() {
        return preFillTypes;
    }

    public List<TagDto> getAllTags() {
        return allTags;
    }

    public List<Long> getSelectedTagIds() {
        return selectedTagIds;
    }

    public DocumentDto getDocument() {
        return document;
    }

    public Map<String, List<String>> getPreFillTypeOptions() {
        return preFillTypeOptions;
    }

    public List<SignRequestParamsFrontDto> getSpots() {
        return spots;
    }

    public Map<Integer, Long> getSrpMap() {
        return srpMap;
    }

    public Integer getDefaultSignImageNumber() {
        return defaultSignImageNumber;
    }

    public static class FormDto {
        private final Long id;
        private final String title;
        private final String name;
        private final String description;
        private final Boolean isFeatured;
        private final Boolean activeVersion;
        private final List<ShareType> authorizedShareTypes;
        private final Boolean publicUsage;
        private final List<String> roles;
        private final Boolean hideButton;
        private final String preFillType;
        private final Boolean pdfDisplay;
        private final String action;
        private final String message;
        private final WorkflowSummaryDto workflow;
        private final DocumentDto document;
        private final List<FieldDto> fields;
        private final List<SignRequestParamsFrontDto> signRequestParams;
        private final Boolean deleted;

        public FormDto(Long id,
                       String title,
                       String name,
                       String description,
                       Boolean isFeatured,
                       Boolean activeVersion,
                       List<ShareType> authorizedShareTypes,
                       Boolean publicUsage,
                       List<String> roles,
                       Boolean hideButton,
                       String preFillType,
                       Boolean pdfDisplay,
                       String action,
                       String message,
                       WorkflowSummaryDto workflow,
                       DocumentDto document,
                       List<FieldDto> fields,
                       List<SignRequestParamsFrontDto> signRequestParams,
                       Boolean deleted) {
            this.id = id;
            this.title = title;
            this.name = name;
            this.description = description;
            this.isFeatured = isFeatured;
            this.activeVersion = activeVersion;
            this.authorizedShareTypes = authorizedShareTypes;
            this.publicUsage = publicUsage;
            this.roles = roles;
            this.hideButton = hideButton;
            this.preFillType = preFillType;
            this.pdfDisplay = pdfDisplay;
            this.action = action;
            this.message = message;
            this.workflow = workflow;
            this.document = document;
            this.fields = fields;
            this.signRequestParams = signRequestParams;
            this.deleted = deleted;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Boolean getIsFeatured() {
            return isFeatured;
        }

        public Boolean getActiveVersion() {
            return activeVersion;
        }

        public List<ShareType> getAuthorizedShareTypes() {
            return authorizedShareTypes;
        }

        public Boolean getPublicUsage() {
            return publicUsage;
        }

        public List<String> getRoles() {
            return roles;
        }

        public Boolean getHideButton() {
            return hideButton;
        }

        public String getPreFillType() {
            return preFillType;
        }

        public Boolean getPdfDisplay() {
            return pdfDisplay;
        }

        public String getAction() {
            return action;
        }

        public String getMessage() {
            return message;
        }

        public WorkflowSummaryDto getWorkflow() {
            return workflow;
        }

        public DocumentDto getDocument() {
            return document;
        }

        public List<FieldDto> getFields() {
            return fields;
        }

        public List<SignRequestParamsFrontDto> getSignRequestParams() {
            return signRequestParams;
        }

        public Boolean getDeleted() {
            return deleted;
        }
    }

    public static class WorkflowSummaryDto {
        private final Long id;
        private final String description;
        private final String mailFrom;
        private final List<WorkflowStepDto> workflowSteps;

        public WorkflowSummaryDto(Long id, String description, String mailFrom, List<WorkflowStepDto> workflowSteps) {
            this.id = id;
            this.description = description;
            this.mailFrom = mailFrom;
            this.workflowSteps = workflowSteps;
        }

        public Long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getMailFrom() {
            return mailFrom;
        }

        public List<WorkflowStepDto> getWorkflowSteps() {
            return workflowSteps;
        }
    }

    public static class WorkflowStepDto {
        private final Long id;
        private final SignType signType;
        private final Boolean allSignToComplete;
        private final String name;
        private final List<UserDto> users;

        public WorkflowStepDto(Long id, SignType signType, Boolean allSignToComplete, String name, List<UserDto> users) {
            this.id = id;
            this.signType = signType;
            this.allSignToComplete = allSignToComplete;
            this.name = name;
            this.users = users;
        }

        public Long getId() {
            return id;
        }

        public SignType getSignType() {
            return signType;
        }

        public Boolean getAllSignToComplete() {
            return allSignToComplete;
        }

        public String getName() {
            return name;
        }

        public List<UserDto> getUsers() {
            return users;
        }
    }

    public static class UserDto {
        private final String firstname;
        private final String name;

        public UserDto(String firstname, String name) {
            this.firstname = firstname;
            this.name = name;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getName() {
            return name;
        }
    }

    public static class WorkflowOptionDto {
        private final Long id;
        private final String description;

        public WorkflowOptionDto(Long id, String description) {
            this.id = id;
            this.description = description;
        }

        public Long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class PreFillOptionDto {
        private final String name;
        private final String description;

        public PreFillOptionDto(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class TagDto {
        private final Long id;
        private final String name;
        private final String color;

        public TagDto(Long id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getColor() {
            return color;
        }
    }

    public static class DocumentDto {
        private final String fileName;
        private final String contentType;

        public DocumentDto(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }
    }

    public static class FieldDto {
        private final Long id;
        private final String name;
        private final String description;
        private final FieldType type;
        private final Boolean favorisable;
        private final Boolean required;
        private final Boolean readOnly;
        private final String extValueServiceName;
        private final String searchServiceName;
        private final String extValueType;
        private final String searchType;
        private final String extValueReturn;
        private final String searchReturn;
        private final List<Long> workflowStepIds;

        public FieldDto(Long id,
                        String name,
                        String description,
                        FieldType type,
                        Boolean favorisable,
                        Boolean required,
                        Boolean readOnly,
                        String extValueServiceName,
                        String searchServiceName,
                        String extValueType,
                        String searchType,
                        String extValueReturn,
                        String searchReturn,
                        List<Long> workflowStepIds) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.favorisable = favorisable;
            this.required = required;
            this.readOnly = readOnly;
            this.extValueServiceName = extValueServiceName;
            this.searchServiceName = searchServiceName;
            this.extValueType = extValueType;
            this.searchType = searchType;
            this.extValueReturn = extValueReturn;
            this.searchReturn = searchReturn;
            this.workflowStepIds = workflowStepIds;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public FieldType getType() {
            return type;
        }

        public Boolean getFavorisable() {
            return favorisable;
        }

        public Boolean getRequired() {
            return required;
        }

        public Boolean getReadOnly() {
            return readOnly;
        }

        public String getExtValueServiceName() {
            return extValueServiceName;
        }

        public String getSearchServiceName() {
            return searchServiceName;
        }

        public String getExtValueType() {
            return extValueType;
        }

        public String getSearchType() {
            return searchType;
        }

        public String getExtValueReturn() {
            return extValueReturn;
        }

        public String getSearchReturn() {
            return searchReturn;
        }

        public List<Long> getWorkflowStepIds() {
            return workflowStepIds;
        }
    }
}


