package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private ObjectProvider<LdapPersonService> ldapPersonService;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private UserRepository userRepository;

    @Resource
    List<SecurityService> securityServices;

    @Resource
    LdapOrganizationalUnitService ldapOrganizationalUnitService;

    @Resource
    private FileService fileService;

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
        if (auth != null) {
            String eppn = auth.getName();
            return getUserByEppn(eppn);
        } else {
            return null;
        }
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
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
        return createUser("system", "", "", "system", UserType.system);
    }

    public User getCreatorUser() {
        return createUser("creator", "Createur de la demande", "", "creator", UserType.system);
    }

    public User getSchedulerUser() {
        return createUser("scheduler", "Esup-Signature", "Automate", globalProperties.getApplicationEmail(), UserType.system);
    }

    public User getGenericUser() {
        return createUser("generic", "Utilisateur issue des favoris", "", "generic", UserType.system);
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        userRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public boolean preAuthorizeNotInShare(User user, User authUser) {
        return user.equals(authUser);
    }

    public User checkUserByEmail(String email) {
        if (userRepository.countByEmail(email) > 0) {
            return userRepository.findByEmail(email).get(0);
        } else {
            return createUserWithEmail(email);
        }
    }

    public User getUserByEmail(String email) {
        if (userRepository.countByEmail(email) > 0) {
            return userRepository.findByEmail(email).get(0);
        } else {
            return createUserWithEmail(email);
        }
    }

    public User getUserByEppn(String eppn) {
        if (eppn.equals("scheduler")) {
            return getSchedulerUser();
        }
        if (userRepository.countByEppn(eppn) == 0) {
            if (eppn.split("@").length == 1) {
                for (SecurityService securityService : this.securityServices) {
                    if (securityService instanceof CasSecurityServiceImpl) {
                        eppn = eppn + "@" + globalProperties.getDomain();
                    }
                }
            }
        }
        if (userRepository.countByEppn(eppn) > 0) {
            return userRepository.findByEppn(eppn).get(0);
        }
		if(!eppn.startsWith("anonymousUser")) {
            logger.error("unable to find user : " + eppn);
		}
        return null;
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
        if (!user.getEppn().equals(getSystemUser().getEppn())) {
            return user;
        }
        if (ldapPersonService.getIfAvailable() != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getIfAvailable().getPersonLdapRepository().findByEduPersonPrincipalName(eppn);
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
        if (ldapPersonService.getIfAvailable() != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getIfAvailable().getPersonLdapRepository().findByMail(mail);
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
        if (userType.equals(UserType.external)) {
            logger.info("ldap user not found : " + mail + ". Creating temp acccount");
            return createUser(mail, mail, "Nouvel utilisateur", mail, UserType.external);
        } else if (userType.equals(UserType.shib)) {
            return createUser(mail, mail, "Nouvel utilisateur", mail, UserType.shib);
        }
        return null;
    }

    public User createUserWithAuthentication(Authentication authentication) {
        String uid;
        if (authentication.getName().contains("@")) {
            uid = authentication.getName().substring(0, authentication.getName().indexOf("@"));
        } else {
            uid = authentication.getName();
        }
        if(ldapPersonService.getIfAvailable() == null) {
        	throw new EsupSignatureRuntimeException("Creation of user not implemented without ldap configuration");
        }
        logger.info("controle de l'utilisateur " + uid);
        List<PersonLdap> personLdaps =  ldapPersonService.getIfAvailable().getPersonLdapRepository().findByUid(uid);
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
        if (userRepository.countByEppn(eppn) > 0) {
            user = userRepository.findByEppn(eppn).get(0);
        } else {
            logger.info("creation de l'utilisateur " + eppn);
            user = new User();
            user.setKeystore(null);

        }
        user.setName(name);
        user.setFirstname(firstName);
        user.setEppn(eppn);
        user.setEmail(email);
        user.setUserType(userType);
        if(!user.getUserType().equals(UserType.system)) {
        	// TODO ! 
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth != null && globalProperties.getGroupPrefixRoleName() != null && eppn.equals(auth.getName())) {
                logger.info("Mise à jour des rôles de l'utilisateur " + eppn);
            	try {
            		Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) auth.getAuthorities();
            		if (authorities.size() > 0) {
            			user.getRoles().clear();
            			Set<String> roles = new HashSet<>();
            			for (GrantedAuthority authority : authorities) {
            				if (authority.getAuthority().toLowerCase().contains(globalProperties.getGroupPrefixRoleName())) {
            					// TODO ! 
            					String role = authority.getAuthority().toLowerCase().split(globalProperties.getGroupPrefixRoleName() + ".")[1].split(",")[0];
            					roles.add(role);
            				}
            			}
            			user.getRoles().addAll(roles);
            		}
            	} catch (Exception e) {
            		logger.warn(String.format("unable to get/update roles for user %s", eppn), e);
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
        long diffInMillies = Long.MAX_VALUE;
        if (user.getLastSendAlertDate() != null) {
            diffInMillies = Math.abs(date.getTime() - user.getLastSendAlertDate().getTime());
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        long diff = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        if ((EmailAlertFrequency.hourly.equals(user.getEmailAlertFrequency()) && diff >= 1 && minute == 0)
                || (EmailAlertFrequency.daily.equals(user.getEmailAlertFrequency()) && diff >= 24 && user.getEmailAlertHour().equals(hour))
                || (EmailAlertFrequency.weekly.equals(user.getEmailAlertFrequency()) && diff >= 168 && user.getEmailAlertDay().equals(DayOfWeek.of(calendar.get(Calendar.DAY_OF_WEEK))))) {
            return true;
        }
        return false;
    }

    public List<PersonLdap> getPersonLdaps(String searchString) {
        List<PersonLdap> personLdaps = new ArrayList<>();
        Set<User> users = new HashSet<>();
        users.addAll(userRepository.findByEppnStartingWith(searchString));
        users.addAll(userRepository.findByNameStartingWithIgnoreCase(searchString));
        users.addAll(userRepository.findByEmailStartingWith(searchString));
        for (User user : users) {
            personLdaps.add(getPersonLdapFromUser(user));
        }
        if (ldapPersonService.getIfAvailable() != null && !searchString.trim().isEmpty() && searchString.length() > 3) {
            List<PersonLdap> ldapSearchList = ldapPersonService.getIfAvailable().search(searchString);
            if (ldapSearchList.size() > 0) {
                List<PersonLdap> ldapList = ldapSearchList.stream().sorted(Comparator.comparing(PersonLdap::getDisplayName)).collect(Collectors.toList());
                for (PersonLdap personLdapList : ldapList) {
                    if (personLdapList.getMail() != null) {
                        if (!personLdaps.stream().anyMatch(personLdap -> personLdap.getMail().equals(personLdapList.getMail()))) {
                            personLdaps.add(personLdapList);
                        }
                    }
                }
            }
        }
        return personLdaps;
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

    public PersonLdap findPersonLdapByUser(User user) {
        PersonLdap personLdap = null;
        if (ldapPersonService.getIfAvailable() != null) {
            List<PersonLdap> personLdaps =  ldapPersonService.getIfAvailable().getPersonLdapRepository().findByEduPersonPrincipalName(user.getEppn());
            if (personLdaps.size() > 0) {
                personLdap = personLdaps.get(0);
            }
        } else {
            personLdap = getPersonLdapFromUser(user);
        }
        return personLdap;
    }

    public OrganizationalUnitLdap findOrganizationalUnitLdapByPersonLdap(PersonLdap personLdap) {
        if (ldapPersonService.getIfAvailable() != null) {
            return ldapOrganizationalUnitService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
        }
        return null;
    }

    public void disableIntro(User authUser, String name) {
        authUser.getUiParams().put(UiParams.valueOf(name), "true");
    }

    public UserType checkMailDomain(String email) {
        String[] emailSplit = email.split("@");
        if (emailSplit.length > 1) {
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

    @Transactional
    public List<String> getBase64UserSignatures(User user) {
        List<String> base64UserSignatures = new ArrayList<>();
        for (Document signature : user.getSignImages()) {
            try {
                base64UserSignatures.add(fileService.getBase64Image(signature));
            } catch (IOException e) {
                logger.error("error on convert sign image document : " + signature.getId() + " for " + user.getEppn());
            }
        }
        return base64UserSignatures;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).get();
    }

    public List<User> getTempUsersFromRecipientList(List<String> recipientsEmails) {
        List<User> tempUsers = new ArrayList<>();
        for (String recipientEmail : recipientsEmails) {
            if(recipientEmail.contains("*")) {
                recipientEmail = recipientEmail.split("\\*")[1];
            }
            User recipientUser = getUserByEmail(recipientEmail);
            if(recipientUser.getUserType().equals(UserType.external)) {
                tempUsers.add(recipientUser);
            }
        }
        return tempUsers;
    }

    public boolean isTempUsers(SignRequest signRequest) {
        boolean isTempUsers = false;
        if(getTempUsers(signRequest).size() > 0) {
            isTempUsers = true;
        }
        return isTempUsers;
    }

    public List<User> getTempUsers(SignRequest signRequest, List<String> recipientsEmails) {
        Set<User> users = new HashSet<>();
        users.addAll(getTempUsers(signRequest));
        if(recipientsEmails != null) {
            users.addAll(getTempUsersFromRecipientList(recipientsEmails));
        }
        return new ArrayList<>(users);
    }

    public List<User> getTempUsers(SignRequest signRequest) {
        Set<User> users = new HashSet<>();
        if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflowSteps().size() > 0) {
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getWorkflowSteps()) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().getUserType().equals(UserType.external) || (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && recipient.getUser().getEppn().equals(recipient.getUser().getName()))) {
                        users.add(recipient.getUser());
                    }
                }
            }
        }
        return new ArrayList<>(users);
    }
}
