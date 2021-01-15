package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.ldap.LdapOrganizationalUnitService;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.esupportail.esupsignature.service.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.esupportail.esupsignature.service.security.SecurityService;
import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.esupportail.esupsignature.service.security.shib.ShibSecurityServiceImpl;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    private WebSecurityProperties webSecurityProperties;

    @Resource
    private GlobalProperties globalProperties;

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private UserRepository userRepository;

    @Resource
    private List<SecurityService> securityServices;

    @Autowired(required = false)
    private LdapOrganizationalUnitService ldapOrganizationalUnitService;

    @Resource
    private FileService fileService;

    @Resource
    private DocumentService documentService;

    public User getById(Long id) {
        return userRepository.findById(id).get();
    }

    public User getByEppn(String eppn) {
        List<User> users = userRepository.findByEppn(eppn);
        if(users.size() > 0) {
            return users.get(0);
        }
        return null;
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

    public User getUserByEmail(String email) {
        if (userRepository.countByEmail(email) > 0) {
            return userRepository.findByEmail(email).get(0);
        } else {
            return createUserWithEmail(email);
        }
    }

    @Transactional
    public User getUserByEppn(String eppn) {
        if (eppn.equals("scheduler")) {
            return getSchedulerUser();
        }
        if (eppn.equals("creator")) {
            return getCreatorUser();
        }
        eppn = buildEppn(eppn);
        User user = getByEppn(eppn);
        if (user != null) {
            user.setKeystoreFileName(this.getKeystoreFileName(user));
            user.setSignImagesIds(this.getSignImagesIds(user));
            return user;
        }
		if(!eppn.startsWith("anonymousUser")) {
            logger.error("unable to find user : " + eppn);
		}
        return null;
    }

    public String buildEppn(String uid) {
        for (SecurityService securityService : securityServices) {
            if (securityService instanceof CasSecurityServiceImpl
                    && uid.split("@").length == 1
                    && !(uid.equals("creator") || uid.equals("system") || uid.equals("scheduler") || uid.equals("generic") )) {
                uid = uid + "@" + globalProperties.getDomain();
            }
        }
        return uid;
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
                    eppn = buildEppn(personLdaps.get(0).getUid());
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

    @Transactional
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
        List<PersonLdap> personLdaps =  ldapPersonService.getIfAvailable().getPersonLdap(uid);
        String eppn = personLdaps.get(0).getEduPersonPrincipalName();
        if (eppn == null) {
            eppn = buildEppn(personLdaps.get(0).getUid());
        }
        String mail = personLdaps.get(0).getMail();
        String name = personLdaps.get(0).getSn();
        String firstName = personLdaps.get(0).getGivenName();
        return createUser(eppn, name, firstName, mail, UserType.ldap);
    }

    public User createUser(String eppn, String name, String firstName, String email, UserType userType) {
        User user;
        if (userRepository.countByEppn(eppn) > 0) {
            user = getByEppn(eppn);
        } else if(userRepository.countByEmail(email) > 0) {
            user = userRepository.findByEmail(email).get(0);
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                String userName = buildEppn(auth.getName());
                if (webSecurityProperties.getGroupToRoleFilterPattern() != null && eppn.equals(userName)) {
                    logger.info("Mise à jour des rôles de l'utilisateur " + eppn);
                    Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) auth.getAuthorities();
                    if (authorities.size() > 0) {
                        user.getRoles().clear();
                        for (GrantedAuthority authority : authorities) {
                            if(authority.getAuthority().startsWith("ROLE_")) {
                                user.getRoles().add(authority.getAuthority());
                            }
                        }
                    }
                }
            }
        }
        userRepository.save(user);
        return user;
    }

    @Transactional
    public void updateUser(String authUserEppn, String signImageBase64, EmailAlertFrequency emailAlertFrequency, Integer emailAlertHour, DayOfWeek emailAlertDay, MultipartFile multipartKeystore) throws IOException {
        User authUser = getByEppn(authUserEppn);
        if(multipartKeystore != null && !multipartKeystore.isEmpty()) {
            if(authUser.getKeystore() != null) {
                documentService.delete(authUser.getKeystore());
            }
            authUser.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), authUser.getEppn() + "_" + multipartKeystore.getOriginalFilename().split("\\.")[0] + ".p12", multipartKeystore.getContentType()));
        }
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
            authUser.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signImageBase64), authUser.getEppn() + "_sign.png", "image/png"));
        }
        authUser.setEmailAlertFrequency(emailAlertFrequency);
        authUser.setEmailAlertHour(emailAlertHour);
        authUser.setEmailAlertDay(emailAlertDay);
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

    @Transactional
    public void disableIntro(String authUserEppn, String name) {
        User authUser = getByEppn(authUserEppn);
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
        if(signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().size() > 0) {
            for (LiveWorkflowStep liveWorkflowStep : signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps()) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().getUserType().equals(UserType.external) || (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && recipient.getUser().getEppn().equals(recipient.getUser().getName()))) {
                        users.add(recipient.getUser());
                    }
                }
            }
        }
        return new ArrayList<>(users);
    }


    public Map<String, Object> getKeystoreByUser(String authUserEppn) throws IOException {
        User authUser = getByEppn(authUserEppn);
        Map<String, Object> keystore = new HashMap<>();
        keystore.put("bytes", authUser.getKeystore().getInputStream().readAllBytes());
        keystore.put("fileName", authUser.getKeystore().getFileName());
        keystore.put("contentType", authUser.getKeystore().getContentType());
        return keystore;
    }

    @Transactional
    public Map<String, Object> getSignatureByUserAndId(String authUserEppn, Long id) throws IOException {
        Map<String, Object> signature = new HashMap<>();
        User authUser = getByEppn(authUserEppn);
        Optional<Document> signImage = authUser.getSignImages().stream().filter(document -> document.getId().equals(id)).findFirst();
        if(signImage.isPresent()) {
            signature.put("bytes", signImage.get().getInputStream().readAllBytes());
            signature.put("fileName", signImage.get().getFileName());
            signature.put("contentType", signImage.get().getContentType());
        }
        return signature;
    }

    private List<Long> getSignImagesIds(User user) {
        return user.getSignImages().stream().map(Document::getId).collect(Collectors.toList());
    }

    private String getKeystoreFileName(User user) {
        if(user.getKeystore() != null) {
            return user.getKeystore().getFileName();
        }
        return null;
    }

    @Transactional
    public void deleteSign(String authUserEppn, long id) {
        User authUser = getByEppn(authUserEppn);
        Document signDocument = documentService.getById(id);
        authUser.getSignImages().remove(signDocument);
    }

    @Transactional
    public void setFormMessage(String authUserEppn, long formId) {
        User authUser = getByEppn(authUserEppn);
        authUser.setFormMessages(authUser.getFormMessages() + " " + formId);
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }
}
