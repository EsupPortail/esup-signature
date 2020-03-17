package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.User.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.UserShare;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdapRepository;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.repository.UserShareRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.scheduler.ScheduledTaskService;
import org.esupportail.esupsignature.web.controller.user.SignRequestController;
import org.hibernate.internal.CoreLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	@Resource
	private UserRepository userRepository;

	@Resource
	private LdapPersonService ldapPersonService;

	@Resource
	private FileService fileService;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private PersonLdapRepository personLdapRepository;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private MailService mailService;
	
	public List<User> getAllUsers() {
		List<User> list = new ArrayList<>();
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

	public User getCreatorUser() {
		if(userRepository.countByEppn("creator") > 0) {
			return  userRepository.findByEppn("creator").get(0);
		} else {
			return createUser("creator", "Createur de la demande", "", "");
		}
	}

	public User getUserByEppn(String eppn) {
		if(eppn.equals("Scheduler")) {
			return ScheduledTaskService.getSchedulerUser();
		}
		if(ldapPersonService != null) {
			if(personLdapRepository.findByUid(eppn).size() > 0) {
				eppn = personLdapRepository.findByUid(eppn).get(0).getEduPersonPrincipalName();
			}
		}
		if(userRepository.countByEppn(eppn) > 0) {
			return userRepository.findByEppn(eppn).get(0);
		}
		return null;
	}

	public User createUser(String mail) {
		List<PersonLdap> personLdap =  personLdapRepository.findByMail(mail);
		String eppn = personLdap.get(0).getEduPersonPrincipalName();
        String name = personLdap.get(0).getSn();
        String firstName = personLdap.get(0).getGivenName();
        return createUser(eppn, name, firstName, mail);
	}
	
	public void createUser(Authentication authentication) {
		String uid;
		if(authentication.getName().contains("@")) {
			uid = authentication.getName().substring(0, authentication.getName().indexOf("@"));
		} else {
			uid = authentication.getName();
		}
		List<PersonLdap> personLdaps =  personLdapRepository.findByUid(uid);
		String eppn = uid + "@univ-rouen.fr";
        String mail = personLdaps.get(0).getMail();
        String name = personLdaps.get(0).getSn();
        String firstName = personLdaps.get(0).getGivenName();
        createUser(eppn, name, firstName, mail);
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
		user.getRoles().clear();
		Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
		if (authorities.size() > 0) {
			for (GrantedAuthority authority : authorities) {
				if (authority.getAuthority().startsWith("ROLE_FOR.ESUP-SIGNATURE.USER")) {
					user.getRoles().add(authority.getAuthority().replace("ROLE_FOR.ESUP-SIGNATURE.USER.", ""));
				}
			}
		}
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
		signRequests = signRequests.stream().filter(signRequest -> !signRequest.getCreateBy().equals(user.getEppn())).collect(Collectors.toList());
		if(signRequests.size() > 0) {
			mailService.sendSignRequestAlert(user.getEmail(), signRequests);
		}
		user.setLastSendAlertDate(date);
		userRepository.save(user);
	}
	
    public User getUserFromAuthentication() {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	String eppn = auth.getName() + "@univ-rouen.fr";
    	if(ldapPersonService != null) {
    		if(personLdapRepository.findByUid(auth.getName()).size() > 0) {
    			String ldapEppn = personLdapRepository.findByUid(auth.getName()).get(0).getEduPersonPrincipalName();
    			if(ldapEppn != null) {
    				eppn = ldapEppn;
				}
    		}
    	}
		if (userRepository.countByEppn(eppn) > 0) {
			User user = userRepository.findByEppn(eppn).get(0);
			if(user.getSignImage() != null) {
				try {
					user.setSignImageBase64(fileService.getBase64Image(user.getSignImage()));
				} catch (IOException e) {
					logger.error("sign image read error", e);
				}
			}
			userRepository.save(user);
			return user;
		} else {
			return getSystemUser();
		}
    }

	public User getSystemUser() {
		User user = new User();
		user.setEppn("System");
		return user;
	}

	public User getGenericUser(String name, String firstname) {
		User user = new User();
		user.setName(name);
		user.setFirstname(firstname);
		user.setEppn("Generic");
		return user;
	}

	public User getUserFromSu(String suEppn) {
		if(userRepository.findByEppn(suEppn).size() > 0) {
			User suUser = userRepository.findByEppn(suEppn).get(0);
			List<User> suUsers = getSuUsers();
			if (suUsers.contains(suUser)) {
				return suUser;
			}
		}
		return getUserFromAuthentication();
	}

	public List<User> getSuUsers() {
		User user = getUserFromAuthentication();
		List<User> suUsers = new ArrayList<>();
		for (UserShare userShare : userShareRepository.findByUser(user)) {
			for(User userTo : userShare.getToUsers()) {
				if(!suUsers.contains(userTo)) {
					suUsers.add(userTo);
				}
			}
		}
		return suUsers;
	}

	public List<PersonLdap> getPersonLdaps(String searchString, String ldapTemplateName) {
		List<PersonLdap> personLdaps = new ArrayList<>();
		List<User> users = new ArrayList<>();
		addAllUnique(users, userRepository.findByEppnStartingWith(searchString));
		addAllUnique(users, userRepository.findByNameStartingWithIgnoreCase(searchString));
		addAllUnique(users, userRepository.findByEmailStartingWith(searchString));
		for (User user : users) {
			personLdaps.add(getPersonLdapFromUser(user));
		}
		if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = ldapPersonService.search(searchString, ldapTemplateName);
			if(ldapSearchList.size() > 0) {
				List<PersonLdap> ldapList = ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList());
				for (PersonLdap personLdapList : ldapList) {
					if(personLdapList.getMail() != null) {
						if (!personLdaps.stream().anyMatch(personLdap -> personLdap.getMail().equals(personLdapList.getMail()))) {
							personLdaps.add(personLdapList);
						}
					}
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

	public PersonLdap getPersonLdap(User user) {
		if (ldapPersonService != null) {
			List<PersonLdap> personLdaps = personLdapRepository.findByEduPersonPrincipalName(user.getEppn());
			if(personLdaps.size() > 0) {
				return personLdaps.get(0);
			}
		}
		return null;
	}

	@PostMapping("/change")
	public String change(@RequestParam("suEppn") String suEppn) {
		return "redirect:/user/" + suEppn + "/";
	}

}
