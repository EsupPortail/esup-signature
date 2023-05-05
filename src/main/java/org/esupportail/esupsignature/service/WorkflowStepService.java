package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
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
    private UserService userService;

    @Resource
    private UserPropertieService userPropertieService;

    @Resource
    private FieldService fieldService;

    @Resource
    private LiveWorkflowStepService liveWorkflowStepService;

    @Resource
    private CertificatService certificatService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private UserListService userListService;

    @Transactional
    public WorkflowStep createWorkflowStep(String name, Boolean allSignToComplete, SignType signType, Boolean changeable, String... recipientEmails) throws EsupSignatureRuntimeException {
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
        workflowStep.setChangeable(changeable);
        workflowStepRepository.save(workflowStep);
        if (recipientEmails != null && recipientEmails.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipientEmails);
        }
        return workflowStep;
    }

    public void addRecipientsToWorkflowStep(WorkflowStep workflowStep, String... recipientsEmail) throws EsupSignatureRuntimeException {
        recipientsEmail = Arrays.stream(recipientsEmail).distinct().toArray(String[]::new);
        for (String recipientEmail : recipientsEmail) {
            List<String> groupList = userListService.getUsersEmailFromList(recipientEmail);
            if(groupList.size() == 0) {
                User recipientUser = userService.getUserByEmail(recipientEmail);
                if (workflowStep.getId() != null) {
                    for (User user : workflowStep.getUsers()) {
                        if (user.equals(recipientUser)) {
                            return;
                        }
                    }
                }
                workflowStep.getUsers().add(recipientUser);
            } else {
                workflowStep.getUsers().add(userService.createGroupUserWithEmail(recipientEmail));
            }
        }
    }

    public void changeSignType(WorkflowStep workflowStep, String name, SignType signType) {
        if (name != null) {
            workflowStep.setName(name);
        }
        workflowStep.setSignType(signType);
    }

    @Transactional
    public WorkflowStep addStepRecipients(Long workflowStepId, String[] recipientsEmails) throws EsupSignatureRuntimeException {
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
    public void updateStep(Long workflowStepId, SignType signType, String description, Boolean changeable, Boolean repeatable, Boolean multiSign, Boolean allSignToComplete, Integer maxRecipients, Boolean attachmentAlert, Boolean attachmentRequire, Boolean autoSign, Long certificatId) throws EsupSignatureRuntimeException {
        if(repeatable != null && repeatable && signType.getValue() > 2) {
            throw new EsupSignatureRuntimeException(signType.name() + ", type de signature impossible pour une étape infinie");
        }
        if(autoSign == null) autoSign = false;
        if(autoSign) {
            signType = SignType.certSign;
        }
        WorkflowStep workflowStep = getById(workflowStepId);
        changeSignType(workflowStep, null, signType);
        workflowStep.setDescription(description);
        workflowStep.setChangeable(Objects.requireNonNullElse(changeable, false));
        workflowStep.setRepeatable(Objects.requireNonNullElse(repeatable, false));
        workflowStep.setAttachmentAlert(attachmentAlert);
        workflowStep.setAttachmentRequire(attachmentRequire);
        workflowStep.setMultiSign(Objects.requireNonNullElse(multiSign, false));
        workflowStep.setAllSignToComplete(Objects.requireNonNullElse(allSignToComplete, false));
        workflowStep.setAutoSign(autoSign);
        if(autoSign) {
            workflowStep.getUsers().clear();
        }
        if(certificatId != null) {
            workflowStep.setCertificat(certificatService.getById(certificatId));
        } else {
            workflowStep.setCertificat(null);
        }
        if(maxRecipients != null) {
            workflowStep.setMaxRecipients(maxRecipients);
        }
    }

    @Transactional
    public void addStep(Long workflowId, String signType, String description, String[] recipientsEmails, Boolean changeable, Boolean allSignToComplete, Integer maxRecipients, String authUserEppn, boolean saveFavorite, Boolean attachmentRequire, Boolean autoSign, Long certificatId) throws EsupSignatureRuntimeException {
        if(autoSign && certificatId == null) {
            throw new EsupSignatureRuntimeException("Certificat is empty");
        }
        Workflow workflow = workflowRepository.findById(workflowId).get();
        WorkflowStep workflowStep = createWorkflowStep("", allSignToComplete, SignType.valueOf(signType), changeable, recipientsEmails);
        workflowStep.setDescription(description);
        if(maxRecipients != null) {
            workflowStep.setMaxRecipients(maxRecipients);
        }
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

    @Transactional
    public void anonymize(String userEppn) {
        User user = userService.getByEppn(userEppn);
        for(WorkflowStep workflowStep : workflowStepRepository.findAll()) {
            workflowStep.getUsers().removeIf(user1 -> user1.equals(user));
        }
    }

}
