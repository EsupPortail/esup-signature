package org.esupportail.esupsignature.service;

import java.util.List;

import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired(required = false)
	PersonLdapDao personDao;
    
    public User getUserFromContext(String authName) {
    	String eppn;
    	List<PersonLdap> persons = null;
    	if(personDao != null) {
			persons =  personDao.getPersonNamesByUid(authName);
			eppn = persons.get(0).getEduPersonPrincipalName();
    	} else {
    		eppn = authName;
    	}
    	if(User.countFindUsersByEppnEquals(eppn) > 0) {
    		return User.findUsersByEppnEquals(eppn).getSingleResult();
    	} else {
	    	User user = new User();
			if(personDao != null) {
				user.setName(persons.get(0).getSn());
				user.setFirstname(persons.get(0).getGivenName());
				user.setEppn(persons.get(0).getEduPersonPrincipalName());
				user.setEmail(persons.get(0).getMail());
			}
			return user;
    	}
    }
	
}
