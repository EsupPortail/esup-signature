package org.esupportail.esupsignature.dto.json;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.Workflow;

import java.util.List;

public interface FormDto {

    Long getId();

    String getName();

    String getTitle();

    String getDescription();

    Workflow getWorkflow();

    List<Field> getFields();

}
