package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.User;
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
    
	@Resource
	private DocumentService documentService;
	
    public String getEppnFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    	String eppn = auth.getName();
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByUid(auth.getName());
			eppn = persons.get(0).getEduPersonPrincipalName();
    	}
    	return eppn;
    }
	
    public User addSignImage(User user, String signImageBase64) throws IOException {
    	user.setSignImage(documentService.addFile(user.getSignImageBase64(), user.getEppn() + "_sign", "application/png"));
    	user.merge();
    	return user;
    	
    }
    
    public User addKeystore(User user) {
    	
    	return user;
    	
    }
}
