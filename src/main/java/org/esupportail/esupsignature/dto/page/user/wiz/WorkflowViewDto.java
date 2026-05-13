package org.esupportail.esupsignature.dto.page.user.wiz;

import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.List;

public class WorkflowViewDto {

    private Long id;
    private String description;
    private String mailFrom;
    private String documentsSourceUri;
    private Boolean sendAlertToAllRecipients;
    private Boolean fromCode;
    private String messageToDisplay;
    private List<TargetDto> targets;
    private List<ViewerDto> viewers;
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

    public String getDocumentsSourceUri() {
        return documentsSourceUri;
    }

    public void setDocumentsSourceUri(String documentsSourceUri) {
        this.documentsSourceUri = documentsSourceUri;
    }

    public Boolean getSendAlertToAllRecipients() {
        return sendAlertToAllRecipients;
    }

    public void setSendAlertToAllRecipients(Boolean sendAlertToAllRecipients) {
        this.sendAlertToAllRecipients = sendAlertToAllRecipients;
    }

    public Boolean getFromCode() {
        return fromCode;
    }

    public void setFromCode(Boolean fromCode) {
        this.fromCode = fromCode;
    }

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public void setMessageToDisplay(String messageToDisplay) {
        this.messageToDisplay = messageToDisplay;
    }

    public List<TargetDto> getTargets() {
        return targets;
    }

    public void setTargets(List<TargetDto> targets) {
        this.targets = targets;
    }

    public List<ViewerDto> getViewers() {
        return viewers;
    }

    public void setViewers(List<ViewerDto> viewers) {
        this.viewers = viewers;
    }

    public List<WorkflowStepDto> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStepDto> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public static class TargetDto {
        private Long id;
        private String targetUri;

        public TargetDto() {
        }

        public TargetDto(Long id, String targetUri) {
            this.id = id;
            this.targetUri = targetUri;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTargetUri() {
            return targetUri;
        }

        public void setTargetUri(String targetUri) {
            this.targetUri = targetUri;
        }
    }

    public static class ViewerDto {
        private String email;
        private String firstname;
        private String name;

        public ViewerDto() {
        }

        public ViewerDto(String email, String firstname, String name) {
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

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

    public static class WorkflowStepDto {
        private Long id;
        private String description;
        private Boolean autoSign;
        private SignType signType;
        private SignLevel minSignLevel;
        private SignLevel maxSignLevel;
        private Boolean sealVisa;
        private Integer maxRecipients;
        private Boolean changeable;
        private Boolean repeatable;
        private Boolean multiSign;
        private Boolean singleSignWithAnnotation;
        private Boolean allSignToComplete;
        private Boolean attachmentAlert;
        private Boolean attachmentRequire;
        private List<UserDto> users;
        private CertificatDto certificat;

        public WorkflowStepDto() {
        }

        public WorkflowStepDto(Long id,
                               String description,
                               Boolean autoSign,
                               SignType signType,
                               SignLevel minSignLevel,
                               SignLevel maxSignLevel,
                               Boolean sealVisa,
                               Integer maxRecipients,
                               Boolean changeable,
                               Boolean repeatable,
                               Boolean multiSign,
                               Boolean singleSignWithAnnotation,
                               Boolean allSignToComplete,
                               Boolean attachmentAlert,
                               Boolean attachmentRequire,
                               List<UserDto> users,
                               CertificatDto certificat) {
            this.id = id;
            this.description = description;
            this.autoSign = autoSign;
            this.signType = signType;
            this.minSignLevel = minSignLevel;
            this.maxSignLevel = maxSignLevel;
            this.sealVisa = sealVisa;
            this.maxRecipients = maxRecipients;
            this.changeable = changeable;
            this.repeatable = repeatable;
            this.multiSign = multiSign;
            this.singleSignWithAnnotation = singleSignWithAnnotation;
            this.allSignToComplete = allSignToComplete;
            this.attachmentAlert = attachmentAlert;
            this.attachmentRequire = attachmentRequire;
            this.users = users;
            this.certificat = certificat;
        }

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

        public Boolean getAutoSign() {
            return autoSign;
        }

        public void setAutoSign(Boolean autoSign) {
            this.autoSign = autoSign;
        }

        public SignType getSignType() {
            return signType;
        }

        public void setSignType(SignType signType) {
            this.signType = signType;
        }

        public SignLevel getMinSignLevel() {
            return minSignLevel;
        }

        public void setMinSignLevel(SignLevel minSignLevel) {
            this.minSignLevel = minSignLevel;
        }

        public SignLevel getMaxSignLevel() {
            return maxSignLevel;
        }

        public void setMaxSignLevel(SignLevel maxSignLevel) {
            this.maxSignLevel = maxSignLevel;
        }

        public Boolean getSealVisa() {
            return sealVisa;
        }

        public void setSealVisa(Boolean sealVisa) {
            this.sealVisa = sealVisa;
        }

        public Integer getMaxRecipients() {
            return maxRecipients;
        }

        public void setMaxRecipients(Integer maxRecipients) {
            this.maxRecipients = maxRecipients;
        }

        public Boolean getChangeable() {
            return changeable;
        }

        public void setChangeable(Boolean changeable) {
            this.changeable = changeable;
        }

        public Boolean getRepeatable() {
            return repeatable;
        }

        public void setRepeatable(Boolean repeatable) {
            this.repeatable = repeatable;
        }

        public Boolean getMultiSign() {
            return multiSign;
        }

        public void setMultiSign(Boolean multiSign) {
            this.multiSign = multiSign;
        }

        public Boolean getSingleSignWithAnnotation() {
            return singleSignWithAnnotation;
        }

        public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) {
            this.singleSignWithAnnotation = singleSignWithAnnotation;
        }

        public Boolean getAllSignToComplete() {
            return allSignToComplete;
        }

        public void setAllSignToComplete(Boolean allSignToComplete) {
            this.allSignToComplete = allSignToComplete;
        }

        public Boolean getAttachmentAlert() {
            return attachmentAlert;
        }

        public void setAttachmentAlert(Boolean attachmentAlert) {
            this.attachmentAlert = attachmentAlert;
        }

        public Boolean getAttachmentRequire() {
            return attachmentRequire;
        }

        public void setAttachmentRequire(Boolean attachmentRequire) {
            this.attachmentRequire = attachmentRequire;
        }

        public List<UserDto> getUsers() {
            return users;
        }

        public void setUsers(List<UserDto> users) {
            this.users = users;
        }

        public CertificatDto getCertificat() {
            return certificat;
        }

        public void setCertificat(CertificatDto certificat) {
            this.certificat = certificat;
        }
    }

    public static class UserDto {
        private String eppn;
        private String email;
        private String firstname;
        private String name;
        private String hidedPhone;
        private UserType userType;
        private UserDto currentReplaceByUser;

        public UserDto() {
        }

        public UserDto(String eppn,
                       String email,
                       String firstname,
                       String name,
                       String hidedPhone,
                       UserType userType,
                       UserDto currentReplaceByUser) {
            this.eppn = eppn;
            this.email = email;
            this.firstname = firstname;
            this.name = name;
            this.hidedPhone = hidedPhone;
            this.userType = userType;
            this.currentReplaceByUser = currentReplaceByUser;
        }

        public String getEppn() {
            return eppn;
        }

        public void setEppn(String eppn) {
            this.eppn = eppn;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

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

        public String getHidedPhone() {
            return hidedPhone;
        }

        public void setHidedPhone(String hidedPhone) {
            this.hidedPhone = hidedPhone;
        }

        public UserType getUserType() {
            return userType;
        }

        public void setUserType(UserType userType) {
            this.userType = userType;
        }

        public UserDto getCurrentReplaceByUser() {
            return currentReplaceByUser;
        }

        public void setCurrentReplaceByUser(UserDto currentReplaceByUser) {
            this.currentReplaceByUser = currentReplaceByUser;
        }
    }

    public static class CertificatDto {
        private Long id;

        public CertificatDto() {
        }

        public CertificatDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
