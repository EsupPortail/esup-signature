package org.esupportail.esupsignature.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequest.SignRequestStatus;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.mail.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired(required = false)
	private PersonLdapDao personDao;
    	
	@Resource
	private DocumentService documentService;

	@Autowired
	private SignBookRepository signBookRepository;
	
	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private EmailService emailService;
	
	public List<User> getAllUsers() {
		List<User> list = new ArrayList<User>();
		userRepository.findAll().forEach(e -> list.add(e));
		return list;
	}
	
	public boolean isUserReady(User user) {
		return signBookService.getUserSignBook(user) != null;
	}

	public SignBook createUserWithSignBook(String email) {
		List<PersonLdap> persons =  personDao.getPersonLdaps("mail", email);
		String eppn = persons.get(0).getEduPersonPrincipalName();
		String name = persons.get(0).getSn();
		String firstName = persons.get(0).getGivenName();
		return createUser(eppn, name, firstName, email, true);
	}

	public void createUser(String email) {
		List<PersonLdap> persons =  personDao.getPersonLdaps("mail", email);
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        createUser(eppn, name, firstName, email, false);
	}
	
	public void createUser(Authentication authentication) {
		String uid;
		if(authentication.getName().contains("@")) {
			uid = authentication.getName().substring(0, authentication.getName().indexOf("@"));
		} else {
			uid = authentication.getName();
		}
		List<PersonLdap> persons =  personDao.getPersonNamesByUid(uid);
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String email = persons.get(0).getMail();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        createUser(eppn, name, firstName, email, false);
	}
	
	public SignBook createUser(String eppn, String name, String firstName, String email, boolean withSignBook) {
		User user;
		if(userRepository.countByEppn(eppn) > 0) {
    		user = userRepository.findByEppn(eppn).get(0);
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
		userRepository.save(user);
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		if(withSignBook) {
			if (signBookRepository.countByRecipientEmailsAndSignBookType(recipientEmails, SignBookType.user) == 0) {
				return signBookService.createUserSignBook(user);
			} else {
				return signBookService.getUserSignBook(user);
			}
		}
		return null;
	}
	
	public boolean checkEmailAlert(User user) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long diffInMillies = Math.abs(date.getTime() - user.getLastSendAlertDate().getTime());
		long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
		if((user.getEmailAlertFrequency() == null && diff > 0)
		|| (EmailAlertFrequency.daily.equals(user.getEmailAlertFrequency()) && diff > 0)
		|| (EmailAlertFrequency.weekly.equals(user.getEmailAlertFrequency()) && diff > 7)) {
			return true;
		}
		return false;
	}

	public void sendEmailAlert(User user) {
		Date date = new Date();
		List<SignRequest> signRequests = signRequestService.findSignRequestByUserAndStatusEquals(user, SignRequestStatus.pending);
		if(signRequests.size() > 0) {
			emailService.sendSignRequestAlert("test", user.getEmail(), signRequests);
		}
		user.setLastSendAlertDate(date);
		userRepository.save(user);
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
		if (userRepository.countByEppn(eppn) > 0) {
			User user = userRepository.findByEppn(eppn).get(0);
			return user;
		} else {
			return null;
		}
    	
    }
	
    public User addSignImage(User user, String signImageBase64) throws IOException {
    	user.setSignImage(documentService.createDocument(user.getSignImageBase64(), user.getEppn() + "_sign", "application/png"));
    	userRepository.save(user);
    	return user;
    	
    }
    
    public PersonLdap getPersonLdap(User user) {
    	if(personDao != null) {
    		List<PersonLdap> persons =  personDao.getPersonNamesByEppn(user.getEppn());
    		if(persons.size() > 0) {
    			return persons.get(0);
    		}
    	}
    	return null;
    }
    
    public User addKeystore(User user) {
    	
    	return user;
    	
    }
}
