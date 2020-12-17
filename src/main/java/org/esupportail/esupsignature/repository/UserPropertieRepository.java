package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserPropertie;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserPropertieRepository extends CrudRepository<UserPropertie, Long>  {
	List<UserPropertie> findByUserEppn(String userEppn);
	List<UserPropertie> findByWorkflowStep(WorkflowStep workflowStep);
	List<UserPropertie> findByUserEppnAndWorkflowStepId(String userEppn, Long workflowStepId);
	List<UserPropertie> findByUserAndWorkflowStepAndUsersIn(User user, WorkflowStep workflowStep, List<User> users);
	List<UserPropertie> findByUserAndTargetEmailAndWorkflowStep(User user, String targetEmail, WorkflowStep workflowStep);
}
