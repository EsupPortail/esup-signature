package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dto.ws.RecipientWsDto;
import org.esupportail.esupsignature.dto.ws.WorkflowStepDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignLevel;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.repository.WorkflowStepRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class WorkflowStepService {

    private final WorkflowStepRepository workflowStepRepository;
    private final UserService userService;
    private final UserPropertieService userPropertieService;
    private final FieldService fieldService;
    private final LiveWorkflowStepService liveWorkflowStepService;
    private final CertificatService certificatService;
    private final WorkflowRepository workflowRepository;
    private final UserListService userListService;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

    public WorkflowStepService(WorkflowStepRepository workflowStepRepository, UserService userService, UserPropertieService userPropertieService, FieldService fieldService, LiveWorkflowStepService liveWorkflowStepService, CertificatService certificatService, WorkflowRepository workflowRepository, UserListService userListService) {
        this.workflowStepRepository = workflowStepRepository;
        this.userService = userService;
        this.userPropertieService = userPropertieService;
        this.fieldService = fieldService;
        this.liveWorkflowStepService = liveWorkflowStepService;
        this.certificatService = certificatService;
        this.workflowRepository = workflowRepository;
        this.userListService = userListService;
    }

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
            workflowStep.setSignType(SignType.signature);
        } else {
            workflowStep.setSignType(signType);
        }
        if(changeable == null) {
            changeable = false;
        }
        workflowStep.setChangeable(changeable);
        workflowStepRepository.save(workflowStep);
        if (recipients != null && recipients.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipients);
        }
        return workflowStep;
    }

    @Transactional
    public WorkflowStep createWorkflowStep(LiveWorkflowStep liveWorkflowStep, RecipientWsDto ...recipients) throws EsupSignatureRuntimeException {
        WorkflowStep workflowStep = new WorkflowStep();
        workflowStep.setAllSignToComplete(liveWorkflowStep.getAllSignToComplete());
        workflowStep.setSignType(liveWorkflowStep.getSignType());
        workflowStep.setMinSignLevel(liveWorkflowStep.getMinSignLevel());
        workflowStep.setMaxSignLevel(liveWorkflowStep.getMaxSignLevel());
        workflowStepRepository.save(workflowStep);
        if (recipients != null && recipients.length > 0) {
            addRecipientsToWorkflowStep(workflowStep, recipients);
        }
        return workflowStep;
    }

    @Transactional
    public WorkflowStep createWorkflowStep(WorkflowStepDto dto) throws EsupSignatureRuntimeException {
        WorkflowStep workflowStep = new WorkflowStep();

        if (dto.getTitle() != null) {
            workflowStep.setName(dto.getTitle());
        }
        workflowStep.setAllSignToComplete(dto.getAllSignToComplete() != null ? dto.getAllSignToComplete() : false);
        workflowStep.setSignType(dto.getSignType() != null ? dto.getSignType() : SignType.signature);
        workflowStep.setChangeable(dto.getChangeable() != null ? dto.getChangeable() : false);
        workflowStep.setMinSignLevel(dto.getMinSignLevel());
        workflowStep.setMaxSignLevel(dto.getMaxSignLevel());
        workflowStepRepository.save(workflowStep);

        if (!dto.getRecipients().isEmpty()) {
            addRecipientsToWorkflowStep(workflowStep, dto.getRecipients().toArray(new RecipientWsDto[0]));
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
    public void updateStep(Long id, Integer workflowStepNumber, SignType signType, String description, Boolean changeable, Boolean repeatable, Boolean multiSign, Boolean singleSignWithAnnotation, Boolean allSignToComplete, Integer maxRecipients, Boolean attachmentAlert, Boolean attachmentRequire, Boolean autoSign, String certificatSelection, SignLevel minSignLevel, SignLevel maxSignLevel, Boolean sealVisa) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowRepository.findById(id).orElseThrow();
        Long workflowStepId = workflow.getWorkflowSteps().get(workflowStepNumber).getId();
        if(autoSign == null) autoSign = false;
        if(autoSign) {
            signType = SignType.signature;
        }
        if(repeatable != null && repeatable && signType.getValue() > 2) {
            throw new EsupSignatureRuntimeException(signType.name() + ", type de signature impossible pour une étape infinie");
        }
        WorkflowStep workflowStep = getById(workflowStepId);
        workflowStep.setSignType(signType);
        if(signType.equals(SignType.visa)) {
            workflowStep.setSealVisa(sealVisa);
        } else {
            workflowStep.setSealVisa(false);
        }
        workflowStep.setDescription(description);
        workflowStep.setChangeable(Objects.requireNonNullElse(changeable, false));
        workflowStep.setRepeatable(Objects.requireNonNullElse(repeatable, false));
        workflowStep.setAttachmentAlert(attachmentAlert);
        workflowStep.setAttachmentRequire(attachmentRequire);
        workflowStep.setMultiSign(Objects.requireNonNullElse(multiSign, false));
        if(workflowStep.getMultiSign()) {
            workflowStep.setSingleSignWithAnnotation(true);
        } else {
            workflowStep.setSingleSignWithAnnotation(Objects.requireNonNullElse(singleSignWithAnnotation, false));
        }
        workflowStep.setAllSignToComplete(Objects.requireNonNullElse(allSignToComplete, false));
        workflowStep.setAutoSign(autoSign);
        if(autoSign) {
            workflowStep.getUsers().clear();
            certificatService.applyWorkflowStepCertificateSelection(workflowStep, certificatSelection);
        } else {
            workflowStep.setCertificat(null);
            workflowStep.setSealCertificatName(null);
        }
        if(maxRecipients != null) {
            workflowStep.setMaxRecipients(maxRecipients);
        }
        if(minSignLevel != null) {
            if (minSignLevel.getValue() <= maxSignLevel.getValue()) {
                workflowStep.setMinSignLevel(minSignLevel);
                workflowStep.setMaxSignLevel(maxSignLevel);
            } else {
                throw new EsupSignatureRuntimeException("Le niveau minimum doit est inférieur ou équal au niveau maximum");
            }
        }
    }

    @Transactional
    public void addStep(Long workflowId, WorkflowStepDto step, Integer stepNumber,  String authUserEppn, boolean saveFavorite, Boolean autoSign, String certificatSelection) throws EsupSignatureRuntimeException {
        Workflow workflow = workflowRepository.findById(workflowId).get();
        WorkflowStep workflowStep = createWorkflowStep("", step.getAllSignToComplete(), step.getSignType(), step.getChangeable(), step.getRecipients().toArray(RecipientWsDto[]::new));
        workflowStep.setDescription(step.getDescription());
        if(step.getMaxRecipients() != null) {
            workflowStep.setMaxRecipients(step.getMaxRecipients());
        }
        workflowStep.setAttachmentRequire(step.getAttachmentRequire());
        workflowStep.setAttachmentAlert(step.getAttachmentAlert());
        workflowStep.setAutoSign(autoSign);
        if(autoSign) {
            certificatService.applyWorkflowStepCertificateSelection(workflowStep, certificatSelection);
        } else {
            workflowStep.setCertificat(null);
            workflowStep.setSealCertificatName(null);
        }
        if (stepNumber == -1) {
            workflow.getWorkflowSteps().add(workflowStep);
        } else {
            workflow.getWorkflowSteps().add(stepNumber, workflowStep);
        }
        if(saveFavorite) {
            userPropertieService.createUserPropertieFromMails(userService.getByEppn(authUserEppn), Collections.singletonList(step));
        }
    }

    @Transactional
    public void removeStep(Long workflowId, Integer stepNumber) {
        Workflow managedWorkflow = workflowRepository.findById(workflowId).orElseThrow();

        WorkflowStep workflowStep = managedWorkflow.getWorkflowSteps().get(stepNumber);
        managedWorkflow.getWorkflowSteps().remove(workflowStep);

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

    @Transactional
    public void saveSignPositions(Long workflowStepId, List<Map<String, Integer>> positions) {
        WorkflowStep workflowStep = getById(workflowStepId);
        List<SignRequestParams> oldParams = new ArrayList<>(workflowStep.getSignRequestParams());
        workflowStep.getSignRequestParams().clear();
        for (Map<String, Integer> pos : positions) {
            SignRequestParams params = new SignRequestParams();
            params.setSignPageNumber(pos.getOrDefault("signPageNumber", 1));
            params.setxPos(pos.getOrDefault("xPos", 0));
            params.setyPos(pos.getOrDefault("yPos", 0));
            params.setSignWidth(pos.getOrDefault("signWidth", 200));
            params.setSignHeight(pos.getOrDefault("signHeight", 100));
            signRequestParamsRepository.save(params);
            workflowStep.getSignRequestParams().add(params);
        }
        signRequestParamsRepository.deleteAll(oldParams);
    }

}
