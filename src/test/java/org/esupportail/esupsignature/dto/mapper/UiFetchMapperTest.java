package org.esupportail.esupsignature.dto.mapper;

import org.esupportail.esupsignature.dto.page.user.wiz.WorkflowViewDto;
import org.esupportail.esupsignature.dto.page.admin.AdminWorkflowUpdateViewDto;
import org.esupportail.esupsignature.entity.Workflow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiFetchMapperTest {

    private final UiFetchMapper uiFetchMapper = new UiFetchMapper();

    @Test
    void toWorkflowViewDtoShouldMapMailFromAndDefaultCollections() {
        Workflow workflow = new Workflow();
        workflow.setId(42L);
        workflow.setDescription("Circuit de test");
        workflow.setMailFrom("contact@example.org");
        workflow.setDocumentsSourceUri("smb://source");
        workflow.setSendAlertToAllRecipients(Boolean.TRUE);
        workflow.setFromCode(Boolean.FALSE);

        WorkflowViewDto dto = uiFetchMapper.toWorkflowViewDto(workflow, "Aide contextuelle");

        assertNotNull(dto);
        assertEquals(42L, dto.getId());
        assertEquals("Circuit de test", dto.getDescription());
        assertEquals("contact@example.org", dto.getMailFrom());
        assertEquals("smb://source", dto.getDocumentsSourceUri());
        assertEquals(Boolean.TRUE, dto.getSendAlertToAllRecipients());
        assertEquals(Boolean.FALSE, dto.getFromCode());
        assertEquals("Aide contextuelle", dto.getMessageToDisplay());
        assertNotNull(dto.getTargets());
        assertNotNull(dto.getViewers());
        assertNotNull(dto.getWorkflowSteps());
        assertTrue(dto.getTargets().isEmpty());
        assertTrue(dto.getViewers().isEmpty());
        assertTrue(dto.getWorkflowSteps().isEmpty());
    }

    @Test
    void toAdminWorkflowUpdateWorkflowDtoShouldMapDisableUpdateByCreator() {
        Workflow workflow = new Workflow();
        workflow.setId(7L);
        workflow.setDescription("Circuit administrable");
        workflow.setDisableDeleteByCreator(Boolean.TRUE);
        workflow.setDisableUpdateByCreator(Boolean.TRUE);

        AdminWorkflowUpdateViewDto.WorkflowDto dto = uiFetchMapper.toAdminWorkflowUpdateWorkflowDto(workflow);

        assertNotNull(dto);
        assertEquals(7L, dto.getId());
        assertEquals("Circuit administrable", dto.getDescription());
        assertEquals(Boolean.TRUE, dto.getDisableDeleteByCreator());
        assertEquals(Boolean.TRUE, dto.getDisableUpdateByCreator());
    }
}

