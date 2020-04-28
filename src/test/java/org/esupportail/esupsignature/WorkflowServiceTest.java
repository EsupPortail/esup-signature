package org.esupportail.esupsignature;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Arrays;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = "app.scheduling.enable=false")
public class WorkflowServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowServiceTest.class);

    @Resource
    private WorkflowService workflowService;

    @Test(timeout = 5000)
    public void testWorkflows() {
        boolean workflowTest = true;
        for(Workflow defaultWorkflow : workflowService.getClassesWorkflows()) {
            try {
                ((DefaultWorkflow) defaultWorkflow).generateWorkflowSteps(new User(), new Data(), Arrays.asList(""));
                logger.info("Test Workflow : " + defaultWorkflow.getName() + " OK");
            } catch (EsupSignatureUserException e) {
                logger.error("Test Workflow : " + defaultWorkflow.getName() + " KO");
                workflowTest = false;
            }
        }
        if(!workflowTest) {
            fail();
        }
    }

}
