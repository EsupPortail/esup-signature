package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.domain.SignBook.DocumentIOType;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.SignRequestParams;
import org.esupportail.esupsignature.domain.SignRequestParams.NewPageType;
import org.esupportail.esupsignature.domain.SignRequestParams.SignType;
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
	
	public void updateUser(Authentication authentication) {
		List<PersonLdap> persons =  personDao.getPersonNamesByUid(authentication.getName());
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String email = persons.get(0).getMail();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        updateUser(eppn, name, firstName, email);
	}
	
	public void updateUser(String eppn, String name, String firstName, String email) {
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
    		user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	} else {
	    	user = new User();
    	}
		user.setName(name);
		user.setFirstname(firstName);
		user.setEppn(eppn);
		user.setEmail(email);
		if(user.getId() == null) {
			user.persist();
		} else {
			user.merge();
		}
		if(SignBook.countFindSignBooksByRecipientEmailAndSignBookTypeEquals(email, SignBookType.user) == 0) {
			SignBook signbook = new SignBook();
			signbook.setName(firstName + " " + name);
			signbook.setCreateBy(eppn);
			signbook.setCreateDate(new Date());
			signbook.setRecipientEmail(email);
			signbook.setSignRequestParams(null);
			signbook.setSignBookType(SignBookType.user);
			signbook.setSourceType(DocumentIOType.none);
			signbook.setTargetType(DocumentIOType.none);
			SignRequestParams signRequestParams = new SignRequestParams();
			signRequestParams.setNewPageType(NewPageType.none);
			signRequestParams.setSignType(SignType.validate);
			signRequestParams.persist();
			signbook.setSignRequestParams(signRequestParams);
			signbook.persist();
		}

	}
	
    public String getEppnFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	String eppn = auth.getName();
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByUid(auth.getName());
    		if(persons.size() > 0) {
    			eppn = persons.get(0).getEduPersonPrincipalName();
    		}
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
