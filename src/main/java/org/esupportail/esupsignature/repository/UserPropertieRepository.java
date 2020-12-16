package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUserId(Long userId);
	List<UserPropertie> findByWorkflowStep(WorkflowStep workflowStep);
	List<UserPropertie> findByUserAndWorkflowStep(User user, WorkflowStep workflowStep);
	List<UserPropertie> findByUserAndWorkflowStepAndUsersIn(User user, WorkflowStep workflowStep, List<User> users);
	List<UserPropertie> findByUserAndTargetEmailAndWorkflowStep(User user, String targetEmail, WorkflowStep workflowStep);
}
