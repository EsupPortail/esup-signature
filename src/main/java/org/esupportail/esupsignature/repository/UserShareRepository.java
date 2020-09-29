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
	List<UserShare> findByFormsContains(Form form);
	List<UserShare> findByToUsersIn(List<User> toUsers);
	List<UserShare> findByUserAndToUsersIn(User user, List<User> toUsers);
	List<UserShare> findByUserAndToUsersInAndShareTypesContains(User user, List<User> toUsers, ShareType shareType);
	List<UserShare> findByUserAndToUsersInAndWorkflowsContainsAndShareTypesContains(User user, List<User> toUsers, Workflow workflow, ShareType shareType);
	List<UserShare> findByToUsersInAndShareTypesContains(List<User> toUsers, ShareType shareType);
	Long countByUserAndToUsersInAndShareTypesContains(User user, List<User> toUsers, ShareType shareType);
}
