package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.interfaces.workflow.ClassWorkflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class WorkflowServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceTest.class);

    @Resource
    private WorkflowService workflowService;

    @Test
    public void testWorkflows() {
        List<Workflow> classWorkflows = workflowService.getClassesWorkflows();
        assertFalse(classWorkflows.isEmpty(), "Au moins un workflow de classe doit être déclaré.");

        for(Workflow defaultWorkflow : classWorkflows) {
            ClassWorkflow classWorkflow = assertInstanceOf(ClassWorkflow.class, defaultWorkflow, "Chaque workflow de classe doit implémenter ClassWorkflow : " + defaultWorkflow.getName());
            List<?> workflowSteps = classWorkflow.getWorkflowSteps();
            assertNotNull(workflowSteps, "Les étapes du workflow ne doivent pas être nulles : " + defaultWorkflow.getName());
            logger.info("Test Workflow : {} OK ({} étapes)", defaultWorkflow.getName(), workflowSteps.size());
        }
    }

}
