package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserShareRepository extends CrudRepository<UserShare, Long>  {
	List<UserShare> findByUser(User user);
	List<UserShare> findByForm(Form form);
	List<UserShare> findByUserAndWorkflow(User user, Workflow workflow);
	List<UserShare> findByToUsersIn(List<User> toUsers);
	List<UserShare> findByUserAndToUsersIn(User user, List<User> toUsers);
	List<UserShare> findByUserAndToUsersInAndShareTypesContains(User user, List<User> toUsers, ShareType shareType);
	List<UserShare> findByWorkflowId(Long workflowId);
	List<UserShare> findByFormId(Long formId);
	List<UserShare> findByUserAndToUsersInAndWorkflowAndShareTypesContains(User user, List<User> toUsers, Workflow workflow, ShareType shareType);
	List<UserShare> findByUserAndToUsersInAndFormAndShareTypesContains(User user, List<User> toUsers, Form form, ShareType shareType);
	List<UserShare> findByToUsersInAndShareTypesContains(List<User> toUsers, ShareType shareType);
	Long countByUserAndToUsersInAndShareTypesContains(User user, List<User> toUsers, ShareType shareType);
}
