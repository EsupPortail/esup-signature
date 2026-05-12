package org.esupportail.esupsignature.dto.page.user.share;

import org.esupportail.esupsignature.dto.projection.jpa.UserDto;
import org.esupportail.esupsignature.entity.enums.ShareType;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class UserShareViewDto {

    private final Long id;
    private final String userEppn;
    private final Boolean signWithOwnSign;
    private final Boolean forceTransmitEmails;
    private final FormRefDto form;
    private final WorkflowRefDto workflow;
    private final Boolean allSignRequests;
    private final Date beginDate;
    private final Date endDate;
    private final Date createDate;
    private final Set<ShareType> shareTypes;
    private final List<UserDto> toUsers;

    public UserShareViewDto(Long id,
                            String userEppn,
                            Boolean signWithOwnSign,
                            Boolean forceTransmitEmails,
                            FormRefDto form,
                            WorkflowRefDto workflow,
                            Boolean allSignRequests,
                            Date beginDate,
                            Date endDate,
                            Date createDate,
                            Set<ShareType> shareTypes,
                            List<UserDto> toUsers) {
        this.id = id;
        this.userEppn = userEppn;
        this.signWithOwnSign = signWithOwnSign;
        this.forceTransmitEmails = forceTransmitEmails;
        this.form = form;
        this.workflow = workflow;
        this.allSignRequests = allSignRequests;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.createDate = createDate;
        this.shareTypes = shareTypes;
        this.toUsers = toUsers;
    }

    public Long getId() {
        return id;
    }

    public String getUserEppn() {
        return userEppn;
    }

    public Boolean getSignWithOwnSign() {
        return signWithOwnSign;
    }

    public Boolean getForceTransmitEmails() {
        return forceTransmitEmails;
    }

    public FormRefDto getForm() {
        return form;
    }

    public WorkflowRefDto getWorkflow() {
        return workflow;
    }

    public Boolean getAllSignRequests() {
        return allSignRequests;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public Set<ShareType> getShareTypes() {
        return shareTypes;
    }

    public List<UserDto> getToUsers() {
        return toUsers;
    }

    public static class FormRefDto {

        private final String title;
        private final Set<ShareType> authorizedShareTypes;

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

        private final String title;
        private final String description;
        private final Set<ShareType> authorizedShareTypes;

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

        private final String name;
        private final String firstname;
        private final String eppn;
        private final String email;

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
