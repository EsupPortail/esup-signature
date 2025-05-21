package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import org.esupportail.esupsignature.entity.enums.SignType;

import jakarta.persistence.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class LiveWorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    @JoinTable(
            indexes = @Index(name = "idx_recipients_live_workflow_step_id", columnList = "live_workflow_step_id")
    )
    private List<Recipient> recipients = new ArrayList<>();

    private String description;

    private Boolean allSignToComplete = false;

    private Boolean attachmentAlert = false;

    private Boolean attachmentRequire = false;

    private Boolean repeatable = false;

    @Enumerated(EnumType.STRING)
    private SignType repeatableSignType;

    private Boolean multiSign = true;

    private Boolean singleSignWithAnnotation = false;

    private Boolean autoSign = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SignType signType;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.DETACH)
    private List<SignRequestParams> signRequestParams = new ArrayList<>();

    @ManyToOne
    private WorkflowStep workflowStep;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public String getDescription() {
        if(description == null && workflowStep != null && StringUtils.hasText(workflowStep.getDescription())) {
            return workflowStep.getDescription();
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public SignType getRepeatableSignType() {
        return repeatableSignType;
    }

    public void setRepeatableSignType(SignType repeatableSignType) {
        this.repeatableSignType = repeatableSignType;
    }

    public Boolean getMultiSign() {
        return Objects.requireNonNullElse(multiSign, true);
    }

    public void setMultiSign(Boolean multiSign) {
        this.multiSign = multiSign;
    }

    public Boolean getSingleSignWithAnnotation() {
        return Objects.requireNonNullElse(singleSignWithAnnotation, false);
    }

    public void setSingleSignWithAnnotation(Boolean singleSignWithAnnotation) {
        this.singleSignWithAnnotation = singleSignWithAnnotation;
    }

    public Boolean getAutoSign() {
        if(autoSign == null) {
            return false;
        }
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

    public List<SignRequestParams> getSignRequestParams() {
        return signRequestParams;
    }

    public void setSignRequestParams(List<SignRequestParams> signRequestParams) {
        this.signRequestParams = signRequestParams;
    }

    public WorkflowStep getWorkflowStep() {
        return workflowStep;
    }

    public void setWorkflowStep(WorkflowStep workflowStep) {
        this.workflowStep = workflowStep;
    }

    public List<User> getUsers() {
        return recipients.stream().map(Recipient::getUser).collect(Collectors.toList());
    }
}
