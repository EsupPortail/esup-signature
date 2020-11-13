package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUser(User user);
	List<UserPropertie> findByWorkflowName(String workflowName);
	List<UserPropertie> findByUserAndStepAndWorkflowName(User user, int step, String workflowName);
	List<UserPropertie> findByUserAndTargetEmailAndWorkflowName(User user, String targetEmail, String workflowName);
}
