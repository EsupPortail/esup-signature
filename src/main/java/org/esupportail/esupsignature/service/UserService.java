package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.mail.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.Function;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements EvaluationContextExtension {

	private PersonLdapDao personDao;

	@Autowired(required = false)
	public void setPersonDao(PersonLdapDao personDao) {
		this.personDao = personDao;
	}

	@Resource
	private UserRepository userRepository;

	@Resource
	private DocumentService documentService;

	@Resource
	private SignBookRepository signBookRepository;

	@Resource
	private SignBookService signBookService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private MailService mailService;
	
	public List<User> getAllUsers() {
		List<User> list = new ArrayList<User>();
		userRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public User createUser(String email) {
		List<PersonLdap> persons =  personDao.getPersonLdaps("mail", email);
		String eppn = persons.get(0).getEduPersonPrincipalName();
        String name = persons.get(0).getSn();
        String firstName = persons.get(0).getGivenName();
        return createUser(eppn, name, firstName, email);
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
        createUser(eppn, name, firstName, email);
	}
	
	public User createUser(String eppn, String name, String firstName, String email) {
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
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		userRepository.save(user);
		return user;
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
		List<SignRequest> signRequests = signRequestService.getTosignRequests(user);
		if(signRequests.size() > 0) {
			mailService.sendSignRequestAlert(user.getEmail(), signRequests);
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
			return userRepository.findByEppn(eppn).get(0);
		} else {
			return null;
		}
    	
    }

	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}

	public String getNameFromEppn(String eppn) {
		return userRepository.findByEppn(eppn).get(0).getFirstname() + " " + userRepository.findByEppn(eppn).get(0).getName();
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	@Override
	public Map<String, Function> getFunctions() {
		return null;
	}

	@Override
	public Object getRootObject() {
		return null;
	}

	@Override
	public String getExtensionId() {
		return null;
	}
}
