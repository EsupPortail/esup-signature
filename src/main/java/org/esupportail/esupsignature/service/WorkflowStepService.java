package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.RecipientWsDto;
import org.esupportail.esupsignature.dto.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
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
    private SignTypeService signTypeService;

    @Resource
    private CertificatService certificatService;

    @Resource
    private WorkflowRepository workflowRepository;

    @Resource
    private UserListService userListService;

    @Transactional
    public WorkflowStep createWorkflowStep(String name, Boolean allSignToComplete, SignType signType, Boolean changeable, RecipientWsDto ...recipients) throws EsupSignatureRuntimeException {
        WorkflowStep workflowStep = new WorkflowStep();
        if (name != null) {
            workflowStep.setName(name);
        }
        if (allSignToComplete == null) {
            workflowStep.setAllSignToComplete(false);
        } else {
            workflowStep.setAllSignToComplete(allSignToComplete);
        }
        if(signType == null) {
            workflowStep.setSignType(signTypeService.getLessSignType(2));
        } else {
            workflowStep.setSignType(signType);
        }
        workflowStep.setChangeable(changeable);
        workflowStepRepository.save(workflowStep);
        if (recipients != null && recipients.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipients);
        }
        return workflowStep;
    }

    public void addRecipientsToWorkflowStep(WorkflowStep workflowStep, RecipientWsDto[] recipients) throws EsupSignatureRuntimeException {
        for (RecipientWsDto recipient : recipients) {
            List<String> groupList = userListService.getUsersEmailFromList(recipient.getEmail());
            if(groupList.isEmpty() || recipient.getEmail().equals("creator")) {
                User recipientUser = userService.getUserByEmail(recipient.getEmail());
                if (workflowStep.getId() != null) {
                    for (User user : workflowStep.getUsers()) {
                        if (user.equals(recipientUser)) {
                            return;
                        }
                    }
                }
                workflowStep.getUsers().add(recipientUser);
            } else {
                workflowStep.getUsers().add(userService.createGroupUserWithEmail(recipient.getEmail()));
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
    public WorkflowStep addStepRecipients(Long workflowStepId, List<RecipientWsDto> recipients) throws EsupSignatureRuntimeException {
        WorkflowStep workflowStep = workflowStepRepository.findById(workflowStepId).get();
        addRecipientsToWorkflowStep(workflowStep, recipients.toArray(new RecipientWsDto[0]));
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
    public void addStep(Long workflowId, WorkflowStepDto step, String authUserEppn, boolean saveFavorite, Boolean autoSign, Long certificatId) throws EsupSignatureRuntimeException {
        if(autoSign && certificatId == null) {
            throw new EsupSignatureRuntimeException("Certificat is empty");
        }
        Workflow workflow = workflowRepository.findById(workflowId).get();
        WorkflowStep workflowStep = createWorkflowStep("", step.getAllSignToComplete(), step.getSignType(), step.getChangeable(), step.getRecipients().toArray(RecipientWsDto[]::new));
        workflowStep.setDescription(step.getDescription());
        if(step.getMaxRecipients() != null) {
            workflowStep.setMaxRecipients(step.getMaxRecipients());
        }
        workflowStep.setAttachmentRequire(step.getAttachmentRequire());
        workflowStep.setAutoSign(autoSign);
        if(autoSign) {
            Certificat certificat = certificatService.getById(certificatId);
            workflowStep.setCertificat(certificat);
        }
        workflow.getWorkflowSteps().add(workflowStep);
        if(saveFavorite) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(step));
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
