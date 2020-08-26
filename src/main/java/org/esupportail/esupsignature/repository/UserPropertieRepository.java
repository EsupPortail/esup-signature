package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUser(User user);
	List<UserPropertie> findByUserAndStepAndForm(User user, int step, Form form);
	List<UserPropertie> findByForm(Form form);
	List<UserPropertie> findByUserAndStepAndRecipientsInAndForm(User user, int step, List<String> recipients, Form form);
	List<UserPropertie> findByUserAndTargetEmailAndForm(User user, String targetEmail, Form form);
}
