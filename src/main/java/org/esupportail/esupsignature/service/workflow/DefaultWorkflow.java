package org.esupportail.esupsignature.service.workflow;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserPropertieService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        return "DefaultWorkflow";
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
    public String getDescription() {
        return "Workflow par défaut";
    }

    @Override
    public List<WorkflowStep> getWorkflowSteps() {
        return this.workflowSteps;
    }

    public List<WorkflowStep> generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) throws EsupSignatureUserException {
        return new ArrayList<>();
    }

    public void initWorkflowSteps() {
        this.workflowSteps = new ArrayList<>();
    }

}
