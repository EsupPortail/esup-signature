package org.esupportail.esupsignature.dto.page.user.wiz;

import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.entity.enums.UserType;

import java.util.List;

public class WorkflowViewDto {

    private final Long id;
    private final String description;
    private final String documentsSourceUri;
    private final Boolean sendAlertToAllRecipients;
    private final Boolean fromCode;
    private final String messageToDisplay;
    private final List<TargetDto> targets;
    private final List<ViewerDto> viewers;
    private final List<WorkflowStepDto> workflowSteps;

    public WorkflowViewDto(Long id,
                           String description,
                           String documentsSourceUri,
                           Boolean sendAlertToAllRecipients,
                           Boolean fromCode,
                           String messageToDisplay,
                           List<TargetDto> targets,
                           List<ViewerDto> viewers,
                           List<WorkflowStepDto> workflowSteps) {
        this.id = id;
        this.description = description;
        this.documentsSourceUri = documentsSourceUri;
        this.sendAlertToAllRecipients = sendAlertToAllRecipients;
        this.fromCode = fromCode;
        this.messageToDisplay = messageToDisplay;
        this.targets = targets;
        this.viewers = viewers;
        this.workflowSteps = workflowSteps;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDocumentsSourceUri() {
        return documentsSourceUri;
    }

    public Boolean getSendAlertToAllRecipients() {
        return sendAlertToAllRecipients;
    }

    public Boolean getFromCode() {
        return fromCode;
    }

    public String getMessageToDisplay() {
        return messageToDisplay;
    }

    public List<TargetDto> getTargets() {
        return targets;
    }

    public List<ViewerDto> getViewers() {
        return viewers;
    }

    public static class TargetDto {
        private final Long id;
        private final String targetUri;

        public TargetDto(Long id, String targetUri) {
            this.id = id;
            this.targetUri = targetUri;
        }

        public Long getId() {
            return id;
        }

        public String getTargetUri() {
            return targetUri;
        }
    }

    public List<WorkflowStepDto> getWorkflowSteps() {
        return workflowSteps;
    }

    public static class ViewerDto {
        private final String email;
        private final String firstname;
        private final String name;

        public ViewerDto(String email, String firstname, String name) {
            this.email = email;
            this.firstname = firstname;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getName() {
            return name;
        }
    }

    public static class WorkflowStepDto {
        private final Long id;
        private final String description;
        private final Boolean autoSign;
        private final SignType signType;
        private final SignLevel minSignLevel;
        private final SignLevel maxSignLevel;
        private final Boolean sealVisa;
        private final Integer maxRecipients;
        private final Boolean changeable;
        private final Boolean repeatable;
        private final Boolean multiSign;
        private final Boolean singleSignWithAnnotation;
        private final Boolean allSignToComplete;
        private final Boolean attachmentAlert;
        private final Boolean attachmentRequire;
        private final List<UserDto> users;
        private final CertificatDto certificat;

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

        public String getDescription() {
            return description;
        }

        public Boolean getAutoSign() {
            return autoSign;
        }

        public SignType getSignType() {
            return signType;
        }

        public SignLevel getMinSignLevel() {
            return minSignLevel;
        }

        public SignLevel getMaxSignLevel() {
            return maxSignLevel;
        }

        public Boolean getSealVisa() {
            return sealVisa;
        }

        public Integer getMaxRecipients() {
            return maxRecipients;
        }

        public Boolean getChangeable() {
            return changeable;
        }

        public Boolean getRepeatable() {
            return repeatable;
        }

        public Boolean getMultiSign() {
            return multiSign;
        }

        public Boolean getSingleSignWithAnnotation() {
            return singleSignWithAnnotation;
        }

        public Boolean getAllSignToComplete() {
            return allSignToComplete;
        }

        public Boolean getAttachmentAlert() {
            return attachmentAlert;
        }

        public Boolean getAttachmentRequire() {
            return attachmentRequire;
        }

        public List<UserDto> getUsers() {
            return users;
        }

        public CertificatDto getCertificat() {
            return certificat;
        }
    }

    public static class UserDto {
        private final String eppn;
        private final String email;
        private final String firstname;
        private final String name;
        private final String hidedPhone;
        private final UserType userType;
        private final UserDto currentReplaceByUser;

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

        public String getEmail() {
            return email;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getName() {
            return name;
        }

        public String getHidedPhone() {
            return hidedPhone;
        }

        public UserType getUserType() {
            return userType;
        }

        public UserDto getCurrentReplaceByUser() {
            return currentReplaceByUser;
        }
    }

    public static class CertificatDto {
        private final Long id;

        public CertificatDto(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }
}

