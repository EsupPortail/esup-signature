package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.user.wiz.StartFormViewDto;
import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.entity.Certificat;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.service.WorkflowService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UiWorkflowMapper {

    private final WorkflowService workflowService;

    public UiWorkflowMapper(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public WorkflowViewDto toWorkflowViewDto(Workflow workflow, String messageToDisplay, String userEppn) {
        if (workflow == null) {
            return null;
        }
        if (workflow.getFromCode()) {
            workflow = workflowService.computeWorkflow(workflow, null, userEppn, true);
        }
        WorkflowViewDto dto = new WorkflowViewDto();
        dto.setId(workflow.getId());
        dto.setDescription(workflow.getDescription());
        dto.setMailFrom(workflow.getMailFrom());
        dto.setDocumentsSourceUri(workflow.getDocumentsSourceUri());
        dto.setSendAlertToAllRecipients(workflow.getSendAlertToAllRecipients());
        dto.setFromCode(workflow.getFromCode());
        dto.setMessageToDisplay(messageToDisplay);
        dto.setTargets(workflow.getTargets() == null ? List.of() : workflow.getTargets().stream().map(this::toWorkflowTargetViewDto).toList());
        dto.setViewers(workflow.getViewers() == null ? List.of() : workflow.getViewers().stream().map(this::toWorkflowViewerDto).toList());
        dto.setWorkflowSteps(workflow.getWorkflowSteps() == null ? List.of() : workflow.getWorkflowSteps().stream().map(this::toWorkflowStepViewDto).toList());
        return dto;
    }

    public StartFormViewDto toStartFormViewDto(Form form, String messageToDisplay, String userEppn) {
        if (form == null) {
            return null;
        }
        StartFormViewDto dto = new StartFormViewDto();
        dto.setId(form.getId());
        dto.setTitle(form.getTitle());
        dto.setMessageToDisplay(messageToDisplay);
        dto.setWorkflow(toWorkflowViewDto(form.getWorkflow(), null, userEppn));
        return dto;
    }

    public WorkflowViewDto.TargetDto toWorkflowTargetViewDto(Target target) {
        if (target == null) {
            return null;
        }
        WorkflowViewDto.TargetDto dto = new WorkflowViewDto.TargetDto();
        dto.setId(target.getId());
        dto.setTargetUri(target.getTargetUri());
        return dto;
    }

    public WorkflowViewDto.ViewerDto toWorkflowViewerDto(User user) {
        if (user == null) {
            return null;
        }
        WorkflowViewDto.ViewerDto dto = new WorkflowViewDto.ViewerDto();
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        return dto;
    }

    public WorkflowViewDto.WorkflowStepDto toWorkflowStepViewDto(WorkflowStep workflowStep) {
        if (workflowStep == null) {
            return null;
        }
        WorkflowViewDto.WorkflowStepDto dto = new WorkflowViewDto.WorkflowStepDto();
        dto.setId(workflowStep.getId());
        dto.setDescription(workflowStep.getDescription());
        dto.setAutoSign(workflowStep.getAutoSign());
        dto.setSignType(workflowStep.getSignType());
        dto.setMinSignLevel(workflowStep.getMinSignLevel());
        dto.setMaxSignLevel(workflowStep.getMaxSignLevel());
        dto.setSealVisa(workflowStep.getSealVisa());
        dto.setMaxRecipients(workflowStep.getMaxRecipients());
        dto.setChangeable(workflowStep.getChangeable());
        dto.setRepeatable(workflowStep.getRepeatable());
        dto.setMultiSign(workflowStep.getMultiSign());
        dto.setSingleSignWithAnnotation(workflowStep.getSingleSignWithAnnotation());
        dto.setAllSignToComplete(workflowStep.getAllSignToComplete());
        dto.setAttachmentAlert(workflowStep.getAttachmentAlert());
        dto.setAttachmentRequire(workflowStep.getAttachmentRequire());
        dto.setUsers(workflowStep.getUsers() == null ? List.of() : workflowStep.getUsers().stream().map(this::toWorkflowStepUserViewDto).toList());
        dto.setCertificat(toWorkflowStepCertificatViewDto(workflowStep.getCertificat()));
        return dto;
    }

    public WorkflowViewDto.UserDto toWorkflowStepUserViewDto(User user) {
        if (user == null) {
            return null;
        }
        WorkflowViewDto.UserDto dto = new WorkflowViewDto.UserDto();
        dto.setEppn(user.getEppn());
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setHidedPhone(user.getHidedPhone());
        dto.setUserType(user.getUserType());
        dto.setCurrentReplaceByUser(toWorkflowReplacementUserViewDto(user.getCurrentReplaceByUser()));
        return dto;
    }

    public WorkflowViewDto.UserDto toWorkflowReplacementUserViewDto(User user) {
        if (user == null) {
            return null;
        }
        WorkflowViewDto.UserDto dto = new WorkflowViewDto.UserDto();
        dto.setEppn(user.getEppn());
        dto.setEmail(user.getEmail());
        dto.setFirstname(user.getFirstname());
        dto.setName(user.getName());
        dto.setHidedPhone(user.getHidedPhone());
        dto.setUserType(user.getUserType());
        dto.setCurrentReplaceByUser(null);
        return dto;
    }

    public WorkflowViewDto.CertificatDto toWorkflowStepCertificatViewDto(Certificat certificat) {
        if (certificat == null) {
            return null;
        }
        WorkflowViewDto.CertificatDto dto = new WorkflowViewDto.CertificatDto();
        dto.setId(certificat.getId());
        return dto;
    }
}
