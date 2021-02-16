package org.esupportail.esupsignature.service.interfaces.workflow;

import org.esupportail.esupsignature.entity.Target;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

public class DefaultWorkflow extends Workflow implements Cloneable {

    private List<WorkflowStep> workflowSteps = new ArrayList<>();

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
    public List<Target> getTargets() {
        return new ArrayList<>();
    }

    public List<WorkflowStep> getWorkflowSteps(User user) {
        if(this.workflowSteps == null) {
            try {
                this.workflowSteps = generateWorkflowSteps(user, null);
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

    public void fillWorkflowSteps(Workflow workflow, List<String> recipentEmailsStep) throws EsupSignatureUserException { }

}
