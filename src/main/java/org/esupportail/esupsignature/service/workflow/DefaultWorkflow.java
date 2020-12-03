package org.esupportail.esupsignature.service.workflow;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class DefaultWorkflow extends Workflow implements Cloneable {

    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    @Resource
    protected UserPropertieService userPropertieService;

    @Resource
    protected WorkflowService workflowService;

    @Resource
    protected UserService userService;

    @Resource
    protected RecipientService recipientService;

    @Override
    public String getName() {
        return "DefaultClassWorkflow";
    }

    @Override
    public String getDescription() {
        return "Workflow par défaut";
    }

    @Override
    public DocumentIOType getSourceType() {
        return DocumentIOType.none;
    }

    @Override
    public DocumentIOType getTargetType() {
        return DocumentIOType.none;
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        if(this.workflowSteps == null) {
            try {
                this.workflowSteps = generateWorkflowSteps(userService.getCurrentUser(), null);
            } catch (EsupSignatureUserException e) {
                return null;
            }
        }
        return this.workflowSteps;
    }

    public List<WorkflowStep> generateWorkflowSteps(User user, List<String> recipentEmailsStep) throws EsupSignatureUserException {
        return new ArrayList<>();
    }

    public void initWorkflowSteps() {
        this.workflowSteps = new ArrayList<>();
    }

    public User getCreateBy() {
        return userService.getSystemUser();
    }

    public void fillWorkflowSteps(Workflow workflow, User user, List<String> recipentEmailsStep) throws EsupSignatureUserException { }

}
