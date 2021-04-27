package org.esupportail.esupsignature.repository.custom;

import org.esupportail.esupsignature.entity.Form;

import java.util.List;


public interface FormRepositoryCustom {

    List<Form> findAuthorizedFormByRole(String role);


}
