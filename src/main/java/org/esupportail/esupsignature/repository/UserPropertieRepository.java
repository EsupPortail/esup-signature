package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUser(User user);
	List<UserPropertie> findByWorkflowStep(WorkflowStep workflowStep);
	List<UserPropertie> findByUserAndWorkflowStep(User user, WorkflowStep workflowStep);
	List<UserPropertie> findByUserAndTargetEmailAndWorkflowStep(User user, String targetEmail, WorkflowStep workflowStep);
}
