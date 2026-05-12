package org.esupportail.esupsignature.dto.page.admin;

import org.esupportail.esupsignature.dto.page.user.signbook.SignBookLightDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.ShowSignRequestDto;
import org.esupportail.esupsignature.dto.page.user.signrequest.SignRequestFullDto;

import java.util.Date;
import java.util.List;

public class AdminSignRequestShowViewDto {

    private SignBookLightDto signBookLight;
    private ShowSignRequestDto.SignRequestLightDto signRequestLight;
    private SignRequestFullDto signRequestFull;
    private ShowSignRequestDto.WorkflowMetaDto workflow;
    private List<ShowSignRequestDto.DocumentDto> originalDocuments;
    private List<ShowSignRequestDto.DocumentDto> signedDocuments;
    private List<ShowSignRequestDto.DocumentDto> documentsHistory;
    private List<ShowSignRequestDto.StepDto> steps;
    private List<ShowSignRequestDto.TargetDto> targets;
    private List<CommentDto> comments;
    private List<LogDto> logs;
    private boolean manager;

    public SignBookLightDto getSignBookLight() { return signBookLight; }
    public void setSignBookLight(SignBookLightDto signBookLight) { this.signBookLight = signBookLight; }
    public ShowSignRequestDto.SignRequestLightDto getSignRequestLight() { return signRequestLight; }
    public void setSignRequestLight(ShowSignRequestDto.SignRequestLightDto signRequestLight) { this.signRequestLight = signRequestLight; }
    public SignRequestFullDto getSignRequestFull() { return signRequestFull; }
    public void setSignRequestFull(SignRequestFullDto signRequestFull) { this.signRequestFull = signRequestFull; }
    public ShowSignRequestDto.WorkflowMetaDto getWorkflow() { return workflow; }
    public void setWorkflow(ShowSignRequestDto.WorkflowMetaDto workflow) { this.workflow = workflow; }
    public List<ShowSignRequestDto.DocumentDto> getOriginalDocuments() { return originalDocuments; }
    public void setOriginalDocuments(List<ShowSignRequestDto.DocumentDto> originalDocuments) { this.originalDocuments = originalDocuments; }
    public List<ShowSignRequestDto.DocumentDto> getSignedDocuments() { return signedDocuments; }
    public void setSignedDocuments(List<ShowSignRequestDto.DocumentDto> signedDocuments) { this.signedDocuments = signedDocuments; }
    public List<ShowSignRequestDto.DocumentDto> getDocumentsHistory() { return documentsHistory; }
    public void setDocumentsHistory(List<ShowSignRequestDto.DocumentDto> documentsHistory) { this.documentsHistory = documentsHistory; }
    public List<ShowSignRequestDto.StepDto> getSteps() { return steps; }
    public void setSteps(List<ShowSignRequestDto.StepDto> steps) { this.steps = steps; }
    public List<ShowSignRequestDto.TargetDto> getTargets() { return targets; }
    public void setTargets(List<ShowSignRequestDto.TargetDto> targets) { this.targets = targets; }
    public List<CommentDto> getComments() { return comments; }
    public void setComments(List<CommentDto> comments) { this.comments = comments; }
    public List<LogDto> getLogs() { return logs; }
    public void setLogs(List<LogDto> logs) { this.logs = logs; }
    public boolean isManager() { return manager; }
    public void setManager(boolean manager) { this.manager = manager; }

    public SignBookLightDto signBookLight() { return signBookLight; }
    public ShowSignRequestDto.SignRequestLightDto signRequestLight() { return signRequestLight; }
    public SignRequestFullDto signRequestFull() { return signRequestFull; }
    public ShowSignRequestDto.WorkflowMetaDto workflow() { return workflow; }
    public List<ShowSignRequestDto.DocumentDto> originalDocuments() { return originalDocuments; }
    public List<ShowSignRequestDto.DocumentDto> signedDocuments() { return signedDocuments; }
    public List<ShowSignRequestDto.DocumentDto> documentsHistory() { return documentsHistory; }
    public List<ShowSignRequestDto.StepDto> steps() { return steps; }
    public List<ShowSignRequestDto.TargetDto> targets() { return targets; }
    public List<CommentDto> comments() { return comments; }
    public List<LogDto> logs() { return logs; }
    public boolean manager() { return manager; }

    public static class CommentDto {
        private Long id;
        private UserDto createBy;
        private Date createDate;
        private String text;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public UserDto getCreateBy() { return createBy; }
        public void setCreateBy(UserDto createBy) { this.createBy = createBy; }
        public Date getCreateDate() { return createDate; }
        public void setCreateDate(Date createDate) { this.createDate = createDate; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class LogDto {
        private Date logDate;
        private String eppn;
        private String action;
        private String initialStatus;
        private String finalStatus;
        private String comment;

        public Date getLogDate() { return logDate; }
        public void setLogDate(Date logDate) { this.logDate = logDate; }
        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getInitialStatus() { return initialStatus; }
        public void setInitialStatus(String initialStatus) { this.initialStatus = initialStatus; }
        public String getFinalStatus() { return finalStatus; }
        public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class UserDto {
        private Long id;
        private String eppn;
        private String firstname;
        private String name;
        private String email;
        private String phone;
        private String userType;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
    }
}
