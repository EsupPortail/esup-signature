package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class WorkflowStepService {

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private UserService userService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private FieldService fieldService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private CertificatService certificatService;

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
    public WorkflowStep addStepRecipients(Long workflowStepId, String[] recipientsEmails) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        addRecipientsToWorkflowStep(workflowStep, recipientsEmails);
        return workflowStep;
    }

    @Transactional
    public WorkflowStep removeStepRecipient(Long workflowStepId, String userEppn) {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        User recipientToRemove = userService.getByEppn(userEppn);
        workflowStep.getUsers().remove(recipientToRemove);
        return workflowStep;
    }

    @Transactional
    public void updateStep(Long workflowStepId, SignType signType, String description, Boolean changeable, Boolean repeatable, Boolean multiSign, Boolean allSignToComplete, Integer maxRecipients, Boolean attachmentAlert, Boolean attachmentRequire) {
        WorkflowStep workflowStep = getById(workflowStepId);
        changeSignType(workflowStep, null, signType);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(Objects.requireNonNullElse(changeable, false));
        workflowStep.setRepeatable(Objects.requireNonNullElse(repeatable, false));
        workflowStep.setAttachmentAlert(attachmentAlert);
        workflowStep.setAttachmentRequire(attachmentRequire);
        workflowStep.setMultiSign(Objects.requireNonNullElse(multiSign, false));
        workflowStep.setAllSignToComplete(Objects.requireNonNullElse(allSignToComplete, false));
        if(maxRecipients != null) {
            workflowStep.setMaxRecipients(maxRecipients);
        }
    }

    @Transactional
    public void addStep(Long workflowId, String signType, String description, String[] recipientsEmails, Boolean changeable, Boolean allSignToComplete, Integer maxRecipients, String authUserEppn, boolean saveFavorite, Boolean attachmentRequire, Boolean autoSign, Long certificatId) throws EsupSignatureException {
        if(autoSign && certificatId == null) {
            throw new EsupSignatureException("Certificat is empty");
        }
        Workflow workflow = workflowService.getById(workflowId);
        WorkflowStep workflowStep = createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), recipientsEmails);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(changeable);
        workflowStep.setMaxRecipients(maxRecipients);
        workflowStep.setAttachmentRequire(attachmentRequire);
        workflowStep.setAutoSign(autoSign);
        if(autoSign) {
            Certificat certificat = certificatService.getById(certificatId);
            workflowStep.setCertificat(certificat);
        }
        workflow.getWorkflowSteps().add(workflowStep);
        if(recipientsEmails != null && saveFavorite) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Arrays.asList(recipientsEmails));
        }
    }

    @Transactional
    public void removeStep(Workflow workflow, Integer stepNumber) {
        WorkflowStep workflowStep = workflow.getWorkflowSteps().get(stepNumber);
        workflow.getWorkflowSteps().remove(workflowStep);
        List<Field> fields = fieldService.getFieldsByWorkflowStep(workflowStep);
        for(Field field : fields) {
            field.getWorkflowSteps().remove(workflowStep);
        }
        List<LiveWorkflowStep> liveWorkflowSteps = liveWorkflowStepService.getLiveWorkflowStepByWorkflowStep(workflowStep);
        for(LiveWorkflowStep liveWorkflowStep : liveWorkflowSteps) {
            liveWorkflowStep.setWorkflowStep(null);
        }
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
