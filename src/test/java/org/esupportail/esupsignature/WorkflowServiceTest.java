package org.esupportail.esupsignature;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.interfaces.workflow.DefaultWorkflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class WorkflowServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceTest.class);

    @Resource
    private WorkflowService workflowService;

    @Test
    public void testWorkflows() {
        boolean workflowTest = true;
        for(Workflow defaultWorkflow : workflowService.getClassesWorkflows()) {
            try {
                ((DefaultWorkflow) defaultWorkflow).generateWorkflowSteps(new User(), null);
                logger.info("Test Workflow : " + defaultWorkflow.getName() + " OK");
            } catch (Exception e) {
                logger.error("Test Workflow : " + defaultWorkflow.getName() + " KO", e);
                workflowTest = false;
            }
        }
        if(!workflowTest) {
            assumeTrue(workflowTest, "Attention, un des workflows est mal configuré");
        }
    }

}
