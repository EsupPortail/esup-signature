package org.esupportail.esupsignature.repository.custom;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;

import java.util.List;


public interface FormRepositoryCustom {

    List<Form> findAuthorizedFormByUser(User user);


}
