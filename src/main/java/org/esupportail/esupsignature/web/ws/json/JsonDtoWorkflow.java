package org.esupportail.esupsignature.web.ws.json;

import org.esupportail.esupsignature.entity.User;

import java.util.Set;

public interface JsonDtoWorkflow {

    Long getId();

    String getName();

    String getTitle();

    String getDescription();

    Integer getCounter();

    User getCreateBy();

    Set<String> getRoles();

    String getManagerRole();

}
