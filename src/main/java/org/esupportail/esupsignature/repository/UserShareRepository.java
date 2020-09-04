package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserShareRepository extends CrudRepository<UserShare, Long>  {
	List<UserShare> findByUser(User user);
	List<UserShare> findByFormsContains(Form form);
	List<UserShare> findByToUsersIn(List<User> toUsers);
	List<UserShare> findByUserAndToUsersIn(User user, List<User> toUsers);
	List<UserShare> findByUserAndToUsersInAndShareType(User user, List<User> toUsers, UserShare.ShareType shareType);
	List<UserShare> findByToUsersInAndShareType(List<User> toUsers, UserShare.ShareType shareType);
	Long countByUserAndToUsersInAndShareType(User user, List<User> toUsers, UserShare.ShareType shareType);
}
