package org.esupportail.esupsignature.service;

import java.util.List;

import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired(required = false)
	PersonLdapDao personDao;
    
    public String getEppnFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    	String eppn = auth.getName();
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByUid(auth.getName());
			eppn = persons.get(0).getEduPersonPrincipalName();
    	}
    	return eppn;
    }
	
}
