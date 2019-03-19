package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.esupportail.esupsignature.domain.SignBook;
import org.esupportail.esupsignature.domain.SignBook.SignBookType;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.domain.User.EmailAlertFrequency;
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

	@Resource
	private SignBookService signBookService;
	
	public boolean isUserReady(User user) {
		return user.isReady();
	}
	
	public void createUser(Authentication authentication) {
		List<PersonLdap> persons =  personDao.getPersonNamesByUid(authentication.getName());
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String email = persons.get(0).getMail();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        createUser(eppn, name, firstName, email);
	}
	
	public void createUser(String eppn, String name, String firstName, String email) {
		User user;
		if(User.countFindUsersByEppnEquals(eppn) > 0) {
    		user = User.findUsersByEppnEquals(eppn).getSingleResult();
    	} else {
	    	user = new User();
			user.setSignImage(null);
			user.setKeystore(null);
			user.setEmailAlertFrequency(EmailAlertFrequency.never);
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
		if(SignBook.countFindSignBooksByRecipientEmailAndSignBookTypeEquals(user.getEmail(), SignBookType.user) == 0) {
			signBookService.createUserSignBook(user);
		}

	}
	
	public void sendEmailAlert(User user) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		System.err.println(calendar.get(Calendar.DAY_OF_MONTH));
		if(!user.getEmailAlertFrequency().equals(EmailAlertFrequency.never)) {
			//TODO alerts
		}
	}
	
    public User getUserFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	String eppn = auth.getName();
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByUid(auth.getName());
    		if(persons.size() > 0) {
    			eppn = persons.get(0).getEduPersonPrincipalName();
    		}
    	}
		if (User.countFindUsersByEppnEquals(eppn) > 0) {
			return User.findUsersByEppnEquals(eppn).getSingleResult();
		} else {
			return null;
		}
    	
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
