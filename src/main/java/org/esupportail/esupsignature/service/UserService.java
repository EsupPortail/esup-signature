package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.ShareType;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.repository.ldap.OrganizationalUnitLdapRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private LdapPersonService ldapPersonService;

	@Resource
	private GlobalProperties globalProperties;

	private LdapProperties ldapProperties;

	@Autowired(required = false)
	public void setLdapPersonService(LdapPersonService ldapPersonService) {
		this.ldapPersonService = ldapPersonService;
	}

	@Autowired(required = false)
	public void setLdapProperties(LdapProperties ldapProperties) {
		this.ldapProperties = ldapProperties;
	}

	@Resource
	private UserRepository userRepository;

	@Resource
	List<SecurityService> securityServices;

	@Resource
	private SignRequestService signRequestService;

	@Resource
	private PersonLdapRepository personLdapRepository;

	@Resource
	private MessageRepository messageRepository;

	@Resource
	private OrganizationalUnitLdapRepository organizationalUnitLdapRepository;

	@Resource
	private UserShareRepository userShareRepository;

	@Resource
	private DataRepository dataRepository;

	@Resource
	private MailService mailService;

	@Resource
	private FileService fileService;

	@Resource
	private HttpServletRequest httpServletRequest;

	public UserService(@Autowired(required = false) LdapPersonService ldapPersonService) {
		this.ldapPersonService = ldapPersonService;
	}


	public void setSuEppn(String eppn) {
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		attr.getRequest().getSession().setAttribute("suEppn", eppn);
	}

	public String getSuEppn() {
		ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
		return (String) attr.getRequest().getSession().getAttribute("suEppn");
	}

	public User getUserFromAuthentication() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null) {
			String eppn = auth.getName();
			return getUserByEppn(eppn);
		} else {
			return null;
		}
	}

	public User getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth != null) {
			String eppn = auth.getName();
			if (getSuEppn() != null) {
				eppn = getSuEppn();
			}
			return getUserByEppn(eppn);
		} else {
			return null;
		}
	}

	public User getSystemUser() {
		if(userRepository.countByEppn("system") > 0) {
			return userRepository.findByEppn("system").get(0);
		} else {
			User user = new User();
			user.setEppn("system");
			userRepository.save(user);
			return user;
		}
	}

	public User getSchedulerUser() {
		if(userRepository.countByEppn("scheduler") > 0) {
			return userRepository.findByEppn("scheduler").get(0);
		} else {
			User user = new User();
			user.setEppn("scheduler");
			user.setIp("127.0.0.1");
			user.setFirstname("Automate");
			user.setName("Esup-Signature");
			user.setEmail("esup-signature@univ-rouen.fr");
			userRepository.save(user);
			return user;
		}
	}

	public User getGenericUser(String name, String firstname) {
		User user = new User();
		user.setName(name);
		user.setFirstname(firstname);
		user.setEppn("Generic");
		return user;
	}

	public List<User> getAllUsers() {
		List<User> list = new ArrayList<>();
		userRepository.findAll().forEach(e -> list.add(e));
		return list;
	}

	public User getCreatorUser() {
		if(userRepository.countByEppn("creator") > 0) {
			return  userRepository.findByEppn("creator").get(0);
		} else {
			return createUser("creator", "Createur de la demande", "", "creator", UserType.system);
		}
	}

	public boolean preAuthorizeNotInShare(User user, User authUser) {
		return user.equals(authUser);
	}

	public User checkUserByEmail(String email) {
		if(userRepository.countByEmail(email) > 0) {
			return userRepository.findByEmail(email).get(0);
		} else {
			return createUserWithEmail(email);
		}
	}

	public User getUserByEmail(String email) {
		if(userRepository.countByEmail(email) > 0) {
			return userRepository.findByEmail(email).get(0);
		}
		return null;
	}

	public User getUserByEppn(String eppn) {
		if(eppn.equals("scheduler")) {
			return getSchedulerUser();
		}
		if(userRepository.countByEppn(eppn) == 0) {
			if (eppn.split("@").length == 1) {
				for (SecurityService securityService : this.securityServices) {
					if (securityService instanceof CasSecurityServiceImpl) {
						eppn = eppn + "@" + globalProperties.getDomain();
					}
				}
			}
		}
		if(userRepository.countByEppn(eppn) > 0) {
			User user = userRepository.findByEppn(eppn).get(0);
			if(user.getSignImages().size() > 0 && user.getSignImages().get(0) != null) {
				try {
					user.setIp(httpServletRequest.getRemoteAddr());
				} catch (Exception e) {
					logger.warn("unable to get ip");
				}
			}
			return user;
		}
		return getSystemUser();
	}

	private String buildEppn(PersonLdap personLdap) {
		if (personLdap == null) {
			return null;
		}
		String eppn = null;
		for (SecurityService securityService : securityServices) {
			if (securityService instanceof CasSecurityServiceImpl) {
				eppn = personLdap.getUid() + "@" + globalProperties.getDomain();
			}
		}
		return eppn;
	}

	public User createUserWithEppn(String eppn) throws EsupSignatureUserException {
		User user = getUserByEppn(eppn);
		if(!user.getEppn().equals(getSystemUser().getEppn())) {
			return user;
		}
		if(ldapPersonService != null) {
			List<PersonLdap> personLdaps = personLdapRepository.findByEduPersonPrincipalName(eppn);
			if (personLdaps.size() > 0) {
				String name = personLdaps.get(0).getSn();
				String firstName = personLdaps.get(0).getGivenName();
				String mail = personLdaps.get(0).getMail();
				return createUser(eppn, name, firstName, mail, UserType.ldap);
			} else {
				throw new EsupSignatureUserException("ldap user not found : " + eppn);
			}
		}
		return null;
	}

	public User createUserWithEmail(String mail) {
		if(ldapPersonService != null) {
			List<PersonLdap> personLdaps = personLdapRepository.findByMail(mail);
			if (personLdaps.size() > 0) {
				String eppn = personLdaps.get(0).getEduPersonPrincipalName();
				if (eppn == null) {
					eppn = buildEppn(personLdaps.get(0));
				}
				String name = personLdaps.get(0).getSn();
				String firstName = personLdaps.get(0).getGivenName();
				return createUser(eppn, name, firstName, mail, UserType.ldap);
			}
		}
		UserType userType = checkMailDomain(mail);
		if(userType.equals(UserType.external)) {
			logger.info("ldap user not found : " + mail + ". Creating temp acccount");
			return createUser(mail, mail, "Nouvel utilisateur", mail, UserType.external);
		} else if (userType.equals(UserType.shib)) {
			return createUser(mail, mail, "Nouvel utilisateur", mail, UserType.shib);
		}
		return null;
	}
	
	public User createUserWithAuthentication(Authentication authentication) {
		String uid;
		if(authentication.getName().contains("@")) {
			uid = authentication.getName().substring(0, authentication.getName().indexOf("@"));
		} else {
			uid = authentication.getName();
		}
		logger.info("controle de l'utilisateur " + uid);
		List<PersonLdap> personLdaps =  personLdapRepository.findByUid(uid);
		String eppn = personLdaps.get(0).getEduPersonPrincipalName();
		if (eppn == null) {
			eppn = buildEppn(personLdaps.get(0));
		}
        String mail = personLdaps.get(0).getMail();
        String name = personLdaps.get(0).getSn();
        String firstName = personLdaps.get(0).getGivenName();
        return createUser(eppn, name, firstName, mail, UserType.ldap);
	}
	
	public User createUser(String eppn, String name, String firstName, String email, UserType userType) {
		User user;
		if(userRepository.countByEppn(eppn) > 0) {
			logger.info("mise à jour de l'utilisateur " + eppn);
    		user = userRepository.findByEppn(eppn).get(0);
    	} else {
			logger.info("creation de l'utilisateur " + eppn);
	    	user = new User();
			user.setKeystore(null);
			//user.setEmailAlertFrequency(EmailAlertFrequency.never);
    	}
		user.setName(name);
		user.setFirstname(firstName);
		user.setEppn(eppn);
		user.setEmail(email);
		user.setUserType(userType);
		List<String> recipientEmails = new ArrayList<>();
		recipientEmails.add(user.getEmail());
		try {
			Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
			if (authorities.size() > 0) {
				user.getRoles().clear();
				Set<String> roles = new HashSet<>();
				for (GrantedAuthority authority : authorities) {
					if (authority.getAuthority().toLowerCase().contains(globalProperties.getGroupPrefixRoleName())) {
						String role = authority.getAuthority().toLowerCase().split(globalProperties.getGroupPrefixRoleName() + ".")[1].split(",")[0];
						roles.add(role);
					}
				}
				user.getRoles().addAll(roles);
			}
		} catch (Exception e) {
			logger.error("unable to get roles " + e);
		}
		userRepository.save(user);
		return user;
	}

	public boolean checkEmailAlert(User user) {
		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		long diffInMillies = Long.MAX_VALUE;
		if(user.getLastSendAlertDate() != null) {
			diffInMillies = Math.abs(date.getTime() - user.getLastSendAlertDate().getTime());
		}
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		long diff = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
		if((user.getEmailAlertFrequency() == null && diff > 0)
			|| (EmailAlertFrequency.hourly.equals(user.getEmailAlertFrequency()) && diff >= 1 && minute == 0)
			|| (EmailAlertFrequency.daily.equals(user.getEmailAlertFrequency()) && diff >= 24 && user.getEmailAlertHour().equals(hour))
			|| (EmailAlertFrequency.weekly.equals(user.getEmailAlertFrequency()) && diff >= 168 && user.getEmailAlertDay().equals(DayOfWeek.of(calendar.get(Calendar.DAY_OF_WEEK))))) {
			return true;
		}
		return false;
	}

	public void sendSignRequestEmailAlert(User recipientUser, SignRequest signRequest) {
		Date date = new Date();
		List<String> toEmails = new ArrayList<>();
		toEmails.add(recipientUser.getEmail());
		for(UserShare userShare : userShareRepository.findByUser(recipientUser)) {
			if (userShare.getShareTypes().contains(ShareType.sign)) {
				for (User toUser : userShare.getToUsers()) {
					List<SignRequest> toSignSharedSignRequests = signRequestService.getToSignRequests(toUser);
					for (SignRequest toSignSharedSignRequest : toSignSharedSignRequests) {
						if (toSignSharedSignRequest.getParentSignBook() != null) {
							List<Data> datas = dataRepository.findBySignBook(toSignSharedSignRequest.getParentSignBook());
							if(datas.size() > 0) {
								if (userShare.getForm().equals(datas.get(0).getForm())) {
									if (!signRequest.equals(toSignSharedSignRequest)) {
										toEmails.add(toUser.getEmail());
									}
								}
							}
						}
					}
				}
			}
		}
		recipientUser.setLastSendAlertDate(date);
		mailService.sendSignRequestAlert(toEmails, signRequest);
		userRepository.save(recipientUser);
	}


	public void sendEmailAlertSummary(User recipientUser) {
		Date date = new Date();
		List<SignRequest> toSignSignRequests = signRequestService.getToSignRequests(recipientUser);
		toSignSignRequests.addAll(signRequestService.getSharedToSignSignRequests(recipientUser));
		if(toSignSignRequests.size() > 0 ) {
			recipientUser.setLastSendAlertDate(date);
			mailService.sendSignRequestSummaryAlert(Arrays.asList(recipientUser.getEmail()), toSignSignRequests);
			userRepository.save(recipientUser);
		}
	}

	public Boolean getSignShare(User user, User authUser) {
		if(userShareRepository.countByUserAndToUsersInAndShareTypesContains(user, Arrays.asList(authUser), ShareType.sign) > 0) {
			return true;
		};
		return false;
	 }

	public List<PersonLdap> getPersonLdaps(String searchString) {
		List<PersonLdap> personLdaps = new ArrayList<>();
		List<User> users = new ArrayList<>();
		addAllUnique(users, userRepository.findByEppnStartingWith(searchString));
		addAllUnique(users, userRepository.findByNameStartingWithIgnoreCase(searchString));
		addAllUnique(users, userRepository.findByEmailStartingWith(searchString));
		for (User user : users) {
			personLdaps.add(getPersonLdapFromUser(user));
		}
		if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
			List<PersonLdap> ldapSearchList = ldapPersonService.search(searchString);
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
		personLdap.setSn(user.getName());
		personLdap.setGivenName(user.getFirstname());
		personLdap.setDisplayName(user.getFirstname() + " " + user.getName());
		personLdap.setMail(user.getEmail());
		personLdap.setEduPersonPrincipalName(user.getEppn());
		return personLdap;
	}

	public PersonLdap getPersonLdapFromHeaders() {
		PersonLdap personLdap = new PersonLdap();
		Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		while(headerNames.hasMoreElements()){
			String headerAttributeName = headerNames.nextElement();
			String personAttributeName = headerAttributeName;
			switch (headerAttributeName){
				case "eppn":
					personAttributeName = "eduPersonPrincipalName";
					break;
				case "primary-affiliation":
					personAttributeName = "eduPersonPrimaryAffiliation";
					break;
				default:
					break;
			}
			try {
				java.lang.reflect.Field field = PersonLdap.class.getDeclaredField(personAttributeName);
				field.setAccessible(true);
				Class<?> type = field.getType();
				//TODO manage other types
				if(type.equals(String.class)) {
					field.set(personLdap, httpServletRequest.getHeader(headerAttributeName));
				}
			} catch (IllegalAccessException | NoSuchFieldException e) {
				logger.debug("error on set personLdap attribut " + headerAttributeName, e);
			}
		}
		return personLdap;
	}

	public PersonLdap findPersonLdapByUser(User user) {
		PersonLdap personLdap = null;
		if (ldapPersonService != null) {
			List<PersonLdap> personLdaps = personLdapRepository.findByEduPersonPrincipalName(user.getEppn());
			if(personLdaps.size() > 0) {
				personLdap = personLdaps.get(0);
			}
		} else {
			personLdap = getPersonLdapFromHeaders();
			if(personLdap.getEduPersonPrincipalName() == null) {
				personLdap = getPersonLdapFromUser(user);
			}
		}
		return personLdap;
	}

	public OrganizationalUnitLdap findOrganizationalUnitLdapByPersonLdap(PersonLdap personLdap) {
		List<OrganizationalUnitLdap> organizationalUnitLdap = organizationalUnitLdapRepository.findBySupannCodeEntite(personLdap.getSupannEntiteAffectationPrincipale());
		if(organizationalUnitLdap.size() > 0) {
			return organizationalUnitLdapRepository.findBySupannCodeEntite(personLdap.getSupannEntiteAffectationPrincipale()).get(0);
		}
		return null;
	}

    public List<Message> getMessages(User authUser) {
		return messageRepository.findByUsersNotContainsAndEndDateAfter(authUser, new Date());
    }

	public void disableLastMessage(User authUser) {
		if(messageRepository.countByUsersNotContainsAndEndDateAfter(authUser, new Date()) > 0) {
			messageRepository.findByUsersNotContainsAndEndDateAfter(authUser, new Date()).get(0).getUsers().add(authUser);
		}
	}

	public void disableMessage(User authUser, long id) {
		Message message = messageRepository.findById(id).get();
		message.getUsers().add(authUser);
	}

	public UserType checkMailDomain(String email) {
		String[] emailSplit = email.split("@");
		if(emailSplit.length > 1) {
			String domain = emailSplit[1];
			for (SecurityService securityService : securityServices) {
				if (securityService instanceof CasSecurityServiceImpl && domain.equals(globalProperties.getDomain())) {
					return UserType.ldap;
				}
				if (securityService instanceof ShibSecurityServiceImpl) {
					File whiteListFile = ((ShibSecurityServiceImpl) securityService).getDomainsWhiteList();
					if (fileService.isFileContainsText(whiteListFile, domain)) {
						return UserType.shib;
					}
				}
			}
		}
		return UserType.external;
	}
}
