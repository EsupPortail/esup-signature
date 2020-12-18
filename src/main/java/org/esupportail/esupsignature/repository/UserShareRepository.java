package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserShareRepository extends CrudRepository<UserShare, Long>  {
	List<UserShare> findByUserEppn(String userEppn);
	List<UserShare> findByUserEppnAndWorkflow(String userEppn, Workflow workflow);
	List<UserShare> findByToUsersEppnIn(List<String> toUsers);
	List<UserShare> findByUserEppnAndToUsersEppnIn(String userEppn, List<String> toUsers);
	List<UserShare> findByUserEppnAndToUsersEppnInAndShareTypesContains(String fromUserEppn, List<String> toUsers, ShareType shareType);
	List<UserShare> findByWorkflowId(Long workflowId);
	List<UserShare> findByFormId(Long formId);
	List<UserShare> findByUserEppnAndToUsersEppnInAndWorkflowAndShareTypesContains(String userEppn, List<String> toUsers, Workflow workflow, ShareType shareType);
	List<UserShare> findByUserEppnAndToUsersEppnInAndFormAndShareTypesContains(String userEppn, List<String> toUsers, Form form, ShareType shareType);
	List<UserShare> findByToUsersEppnInAndShareTypesContains(List<String> toUsers, ShareType shareType);
}
