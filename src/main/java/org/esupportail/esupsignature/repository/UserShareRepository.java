package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserShareRepository extends CrudRepository<UserShare, Long>  {
	List<UserShare> findByUserId(Long userId);
	List<UserShare> findByUserEppnAndWorkflow(String userEppn, Workflow workflow);
	List<UserShare> findByToUsersIdIn(List<Long> toUsers);
	List<UserShare> findByUserIdAndToUsersIdIn(Long user, List<Long> toUsers);
	List<UserShare> findByUserIdAndToUsersIdInAndShareTypesContains(Long fromUserId, List<Long> toUsers, ShareType shareType);
	List<UserShare> findByWorkflowId(Long workflowId);
	List<UserShare> findByFormId(Long formId);
	List<UserShare> findByUserIdAndToUsersIdInAndWorkflowAndShareTypesContains(Long userId, List<Long> toUsers, Workflow workflow, ShareType shareType);
	List<UserShare> findByUserIdAndToUsersIdInAndFormAndShareTypesContains(Long userId, List<Long> toUsers, Form form, ShareType shareType);
	List<UserShare> findByToUsersIdInAndShareTypesContains(List<Long> toUsers, ShareType shareType);
}
