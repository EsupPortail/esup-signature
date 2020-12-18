package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiveWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @OneToMany(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @OrderColumn
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<LiveWorkflowStep> workflowSteps = new ArrayList<>();

    @OneToOne(cascade = CascadeType.REMOVE)
    @LazyCollection(LazyCollectionOption.FALSE)
    private LiveWorkflowStep currentStep;

    @Enumerated(EnumType.STRING)
    private DocumentIOType targetType;

    private String documentsTargetUri;

    @ManyToOne()
    private Workflow workflow;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<LiveWorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<LiveWorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }

    public LiveWorkflowStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(LiveWorkflowStep currentStep) {
        this.currentStep = currentStep;
    }

    public DocumentIOType getTargetType() {
        return targetType;
    }

    public void setTargetType(DocumentIOType targetType) {
        this.targetType = targetType;
    }

    public String getDocumentsTargetUri() {
        return documentsTargetUri;
    }

    public void setDocumentsTargetUri(String documentsTargetUri) {
        this.documentsTargetUri = documentsTargetUri;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Integer getCurrentStepNumber() {
        if (this.getWorkflowSteps().isEmpty()) {
            return -1;
        }
        if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getAllSignToComplete()) {
            if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getRecipients().stream().allMatch(Recipient::getSigned)) {
                return this.workflowSteps.indexOf(this.getCurrentStep()) + 2;
            }
        } else {
            if (this.getWorkflowSteps().get(this.getWorkflowSteps().size() - 1).getRecipients().stream().anyMatch(Recipient::getSigned)) {
                return this.getWorkflowSteps().size();
            }
        }
        return this.workflowSteps.indexOf(this.getCurrentStep()) + 1;
    }
}
