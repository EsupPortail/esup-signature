package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;

@Service
public class WorkflowStepService {

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private UserService userService;

    public WorkflowStep create(User creator, String name, SignType signType) {
        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setName(name);
        workflowStep.setSignType(signType);
        workflowStep.getUsers().add(creator);
        workflowStepRepository.save(workflowStep);
        return workflowStep;
    }

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

    public Long setSignTypeForWorkflowStep(SignType signType, WorkflowStep workflowStep) {
        workflowStep.setSignType(signType);
        return workflowStep.getId();
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
        setSignTypeForWorkflowStep(signType, workflowStep);
    }

    public WorkflowStep addStepRecipients(Long workflowStepId, String recipientsEmails) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
        return workflowStep;
    }

    public WorkflowStep removeStepRecipient(Long workflowStepId, Long userId) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        User recipientToRemove = userService.getUserById(userId);
        workflowStep.getUsers().remove(recipientToRemove);
        return workflowStep;
    }


    public void updateStep(SignType signType, String description, Boolean changeable, Boolean allSignToComplete, WorkflowStep workflowStep) {
        changeSignType(workflowStep, null, signType);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflowStep.setAllSignToComplete(allSignToComplete);
    }

    public void delete(WorkflowStep workflowStep) {
        workflowStepRepository.delete(workflowStep);
    }

}
