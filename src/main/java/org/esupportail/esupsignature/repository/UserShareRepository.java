package org.esupportail.esupsignature.repository;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.UserShare;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserShareRepository extends CrudRepository<UserShare, Long>  {
	List<UserShare> findByUser(User user);
	List<UserShare> findByToUsers(List<User> toUsers);
	List<UserShare> findByUserAndToUsers(User user, List<User> toUsers);
	List<UserShare> findByUserAndToUsersAndShareType(User user, List<User> toUsers, UserShare.ShareType shareType);
}
