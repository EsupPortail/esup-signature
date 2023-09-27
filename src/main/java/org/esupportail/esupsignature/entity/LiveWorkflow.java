package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
public class LiveWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    

    private String title;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderColumn
    private List<LiveWorkflowStep> liveWorkflowSteps = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private LiveWorkflowStep currentStep;

    @OneToMany(cascade = CascadeType.REMOVE)
    private Set<Target> targets = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Workflow workflow;

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

    

    public List<LiveWorkflowStep> getLiveWorkflowSteps() {
        return liveWorkflowSteps;
    }

    public void setLiveWorkflowSteps(List<LiveWorkflowStep> workflowSteps) {
        this.liveWorkflowSteps = workflowSteps;
    }

    public LiveWorkflowStep getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(LiveWorkflowStep currentStep) {
        this.currentStep = currentStep;
    }

    public Set<Target> getTargets() {
        return targets;
    }

    public void setTargets(Set<Target> targets) {
        this.targets = targets;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Integer getCurrentStepNumber() {
        if (this.getLiveWorkflowSteps().isEmpty()) {
            return -1;
        }
        if (this.getLiveWorkflowSteps().get(this.getLiveWorkflowSteps().size() - 1).getAllSignToComplete()) {
            if (this.getLiveWorkflowSteps().get(this.getLiveWorkflowSteps().size() - 1).getRecipients().stream().allMatch(Recipient::getSigned)) {
                return this.liveWorkflowSteps.indexOf(this.getCurrentStep()) + 2;
            }
        } else {
            if (this.getLiveWorkflowSteps().get(this.getLiveWorkflowSteps().size() - 1).getRecipients().stream().anyMatch(Recipient::getSigned)) {
                return this.getLiveWorkflowSteps().size();
            }
        }
        return this.liveWorkflowSteps.indexOf(this.getCurrentStep()) + 1;
    }
}
