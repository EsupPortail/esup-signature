package org.esupportail.esupsignature.service.interfaces.workflow;

import org.esupportail.esupsignature.entity.Workflow;

public abstract class ClassWorkflow extends Workflow implements ModelClassWorkflow {

    @Override
    public Boolean getFromCode() {
        return true;
    }
}
