package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class WorkflowStepService {

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private UserService userService;

    @Transactional
    public WorkflowStep create(User creator, String name, SignType signType) {
        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setName(name);
        workflowStep.setSignType(signType);
        workflowStep.getUsers().add(creator);
        workflowStepRepository.save(workflowStep);
        return workflowStep;
    }

    @Transactional
    public WorkflowStep createWorkflowStep(String name, Boolean allSignToComplete, SignType signType, String... recipientEmails) {
        WorkflowStep workflowStep = new WorkflowStep();
        if (name != null) {
            workflowStep.setName(name);
        }
        if (allSignToComplete == null) {
            workflowStep.setAllSignToComplete(false);
        } else {
            workflowStep.setAllSignToComplete(allSignToComplete);
        }
        workflowStep.setSignType(signType);
        workflowStepRepository.save(workflowStep);
        if (recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipientEmails);
        }
        return workflowStep;
    }

    public void addRecipientsToWorkflowStep(WorkflowStep workflowStep, String... recipientsEmail) {
        recipientsEmail = Arrays.stream(recipientsEmail).distinct().toArray(String[]::new);
        for (String recipientEmail : recipientsEmail) {
            User recipientUser = userService.getUserByEmail(recipientEmail);
            if (workflowStep.getId() != null) {
                for (User user : workflowStep.getUsers()) {
                    if (user.equals(recipientUser)) {
                        return;
                    }
                }
            }
            workflowStep.getUsers().add(recipientUser);
        }
    }

    public void changeSignType(WorkflowStep workflowStep, String name, SignType signType) {
        if (name != null) {
            workflowStep.setName(name);
        }
        workflowStep.setSignType(signType);
    }

    @Transactional
    public WorkflowStep addStepRecipients(Long workflowStepId, String recipientsEmails) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
        return workflowStep;
    }

    @Transactional
    public WorkflowStep removeStepRecipient(Long workflowStepId, Long userId) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        User recipientToRemove = userService.getById(userId);
        workflowStep.getUsers().remove(recipientToRemove);
        return workflowStep;
    }

    @Transactional
    public void updateStep(Long workflowStepId, SignType signType, String description, Boolean changeable, Boolean allSignToComplete) {
        WorkflowStep workflowStep = getById(workflowStepId);
        changeSignType(workflowStep, null, signType);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflowStep.setAllSignToComplete(allSignToComplete);
    }

    @Transactional
    public void addStep(Long workflowId, String signType, String description, String[] recipientsEmails, Boolean changeable, Boolean allSignToComplete) {
        Workflow workflow = workflowService.getById(workflowId);
        WorkflowStep workflowStep = createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflow.getWorkflowSteps().add(workflowStep);
    }

    @Transactional
    public void removeStep(Workflow workflow, Integer stepNumber) {
        WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
        workflow.getWorkflowSteps().remove(workflowStep);
        delete(workflowStep);
    }

    @Transactional
    public void delete(WorkflowStep workflowStep) {
        workflowStepRepository.delete(workflowStep);
    }

    public WorkflowStep getById(Long workflowStepId) {
        return workflowStepRepository.findById(workflowStepId).get();
    }

}
