package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapDao;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.scheduler.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.data.spel.spi.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.method.P;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

	private PersonLdapDao personDao;

	@Autowired(required = false)
	public void setPersonDao(PersonLdapDao personDao) {
		this.personDao = personDao;
	}

	@Resource
	private UserRepository userRepository;

	@Autowired(required = false)
	private LdapPersonService ldapPersonService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private MailService mailService;
	
	public List<User> getAllUsers() {
		List<User> list = new ArrayList<User>();
		userRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public User getUser(String email) {
		if(userRepository.countByEmail(email) > 0) {
			return  userRepository.findByEmail(email).get(0);
		} else {
			return createUser(email);
		}
	}

	//For thymeleaf
	public User getUserByEppn(String eppn) {
		if(eppn.equals("Scheduler")) {
			return ScheduledTaskService.getSchedulerUser();
		}
		if(userRepository.countByEppn(eppn) > 0) {
			return userRepository.findByEppn(eppn).get(0);
		}
		return null;
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
			//user.setEmailAlertFrequency(EmailAlertFrequency.never);
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
		List<SignRequest> signRequests = signRequestService.getToSignRequests(user);
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
			return getSystemUser();
		}
    	
    }

	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}

	public List<PersonLdap> getPersonLdaps(@RequestParam("searchString") String searchString, @RequestParam(required = false) String ldapTemplateName) {
		List<PersonLdap> personLdaps = new ArrayList<>();
		List<User> users = new ArrayList<>();
		addAllUnique(users, userRepository.findByEppnStartingWith(searchString));
		addAllUnique(users, userRepository.findByNameStartingWith(searchString));
		addAllUnique(users, userRepository.findByEmailStartingWith(searchString));
		for (User user : users) {
			personLdaps.add(getPersonLdapFromUser(user));
		}
		if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			List<PersonLdap> ldapList = ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList());
			for(PersonLdap personLdapList : ldapList) {
				if(personLdaps.stream().filter(personLdap -> personLdap.getMail().equals(personLdapList.getMail())).count() > 0) {
					continue;
				} else {
					personLdaps.add(personLdapList);
				}
			}
		}
		return personLdaps;
	}

	public void addAllUnique(List<User> users, List<User> usersToAdd) {
		for (User user : usersToAdd) {
			if(!users.contains(user)) {
				users.add(user);
			}
		}
	}

	public PersonLdap getPersonLdapFromUser(User user) {
		PersonLdap personLdap = new PersonLdap();
		personLdap.setUid(user.getEppn());
		personLdap.setDisplayName(user.getFirstname() + " " + user.getName());
		personLdap.setMail(user.getEmail());
		personLdap.setEduPersonPrincipalName(user.getEppn());
		return personLdap;
	}

}
