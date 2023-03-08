package org.esupportail.esupsignature.web.ws.json;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Workflow;

import java.util.List;

public interface JsonDtoForm {

    Long getId();

    String getName();

    String getTitle();

    String getDescription();

    Workflow getWorkflow();

    List<Field> getFields();

}
