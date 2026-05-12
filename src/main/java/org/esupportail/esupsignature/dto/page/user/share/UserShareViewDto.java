package org.esupportail.esupsignature.dto.page.user.share;

import org.esupportail.esupsignature.dto.projection.jpa.UserDto;
import org.esupportail.esupsignature.entity.enums.ShareType;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserShareViewDto {

    private Long id;
    private String userEppn;
    private Boolean signWithOwnSign;
    private Boolean forceTransmitEmails;
    private FormRefDto form;
    private WorkflowRefDto workflow;
    private Boolean allSignRequests;
    private Date beginDate;
    private Date endDate;
    private Date createDate;
    private Set<ShareType> shareTypes;
    private List<UserDto> toUsers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEppn() {
        return userEppn;
    }

    public void setUserEppn(String userEppn) {
        this.userEppn = userEppn;
    }

    public Boolean getSignWithOwnSign() {
        return signWithOwnSign;
    }

    public void setSignWithOwnSign(Boolean signWithOwnSign) {
        this.signWithOwnSign = signWithOwnSign;
    }

    public Boolean getForceTransmitEmails() {
        return forceTransmitEmails;
    }

    public void setForceTransmitEmails(Boolean forceTransmitEmails) {
        this.forceTransmitEmails = forceTransmitEmails;
    }

    public FormRefDto getForm() {
        return form;
    }

    public void setForm(FormRefDto form) {
        this.form = form;
    }

    public WorkflowRefDto getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowRefDto workflow) {
        this.workflow = workflow;
    }

    public Boolean getAllSignRequests() {
        return allSignRequests;
    }

    public void setAllSignRequests(Boolean allSignRequests) {
        this.allSignRequests = allSignRequests;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Set<ShareType> getShareTypes() {
        return shareTypes;
    }

    public void setShareTypes(Set<ShareType> shareTypes) {
        this.shareTypes = shareTypes;
    }

    public List<UserDto> getToUsers() {
        return toUsers;
    }

    public void setToUsers(List<UserDto> toUsers) {
        this.toUsers = toUsers;
    }

    public static class FormRefDto {

        private String title;
        private Set<ShareType> authorizedShareTypes;

        public FormRefDto(String title, Set<ShareType> authorizedShareTypes) {
            this.title = title;
            this.authorizedShareTypes = authorizedShareTypes;
        }

        public String getTitle() {
            return title;
        }

        public Set<ShareType> getAuthorizedShareTypes() {
            return authorizedShareTypes;
        }
    }

    public static class WorkflowRefDto {

        private String title;
        private String description;
        private Set<ShareType> authorizedShareTypes;

        public WorkflowRefDto(String title, String description, Set<ShareType> authorizedShareTypes) {
            this.title = title;
            this.description = description;
            this.authorizedShareTypes = authorizedShareTypes;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Set<ShareType> getAuthorizedShareTypes() {
            return authorizedShareTypes;
        }
    }

    public static class DelegatedUserDto implements UserDto {

        private String name;
        private String firstname;
        private String eppn;
        private String email;

        public DelegatedUserDto(String name, String firstname, String eppn, String email) {
            this.name = name;
            this.firstname = firstname;
            this.eppn = eppn;
            this.email = email;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getFirstname() {
            return firstname;
        }

        @Override
        public String getEppn() {
            return eppn;
        }

        @Override
        public String getEmail() {
            return email;
        }
    }
}
