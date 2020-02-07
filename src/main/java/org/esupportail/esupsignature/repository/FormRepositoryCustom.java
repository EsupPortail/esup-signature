package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;

import java.util.List;


public interface FormRepositoryCustom {

    List<Form> findFormByUserAndActiveVersion(User user, Boolean activeVersion);


}
