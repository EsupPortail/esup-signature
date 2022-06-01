package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.ldap.*;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.web.ws.json.JsonExternalUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(GlobalProperties.class)
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm";

    private final GlobalProperties globalProperties;

    private final WebSecurityProperties webSecurityProperties;

    private final LdapPersonService ldapPersonService;

    private final LdapOrganizationalUnitService ldapOrganizationalUnitService;

    public UserService(GlobalProperties globalProperties,
                       WebSecurityProperties webSecurityProperties,
                       @Autowired(required = false) LdapPersonService ldapPersonService,
                       @Autowired(required = false) LdapOrganizationalUnitService ldapOrganizationalUnitService) {
        this.globalProperties = globalProperties;
        this.webSecurityProperties = webSecurityProperties;
        this.ldapPersonService = ldapPersonService;
        this.ldapOrganizationalUnitService = ldapOrganizationalUnitService;
    }

    @Resource
    private ShibProperties shibProperties;

    @Resource
    private UserRepository userRepository;

    @Resource
    private FileService fileService;

    @Resource
    private DocumentService documentService;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

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
        return createUser("system", "Esup-Signature", "Automate", "system", UserType.system, false);
    }

    public User getCreatorUser() {
        return createUser("creator", "Createur de la demande", "", "creator", UserType.system, false);
    }

    public User getSchedulerUser() {
        return createUser("scheduler", "Esup-Signature", "Automate", globalProperties.getApplicationEmail(), UserType.system, false);
    }

    public User getGenericUser() {
        return createUser("generic", "Utilisateur issue des favoris", "", "generic", UserType.system, false);
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        userRepository.findAll().forEach(list::add);
        return list;
    }

    public User getUserByEmail(String email) {
        if (userRepository.countByEmail(email) > 0) {
            return userRepository.findByEmail(email).get(0);
        } else {
            return createUserWithEmail(email);
        }
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional
    public User getUserByEppn(String eppn) {
        if (eppn.equals("scheduler")) {
            return getSchedulerUser();
        }
        if (eppn.equals("creator")) {
            return getCreatorUser();
        }
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
        uid = uid.trim().toLowerCase();
        if (uid.split("@").length == 1
                && !(uid.equals("creator") || uid.equals("system") || uid.equals("scheduler") || uid.equals("generic") )) {
            uid = uid + "@" + globalProperties.getDomain();
        }
        return uid;
    }

    public User createUserWithEppn(String eppn) throws EsupSignatureUserException {
        User user = getUserByEppn(eppn);
        if (user != null && !user.getEppn().equals(getSystemUser().getEppn())) {
            return user;
        }
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getPersonLdapRepository().findByEduPersonPrincipalName(eppn);
            if (personLdaps.size() > 0) {
                String name = personLdaps.get(0).getSn();
                String firstName = personLdaps.get(0).getGivenName();
                String mail = personLdaps.get(0).getMail();
                return createUser(eppn, name, firstName, mail, UserType.ldap, false);
            } else {
                throw new EsupSignatureUserException("ldap user not found : " + eppn);
            }
        }
        logger.error("user not found with : " + eppn);
        return null;
    }

    @Transactional
    public User createUserWithEmail(String mail) {
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getPersonLdapRepository().findByMail(mail);
            if (personLdaps.size() > 0) {
                String eppn = personLdaps.get(0).getEduPersonPrincipalName();
                if (eppn == null) {
                    eppn = buildEppn(personLdaps.get(0).getUid());
                }
                String name = personLdaps.get(0).getSn();
                String firstName = personLdaps.get(0).getGivenName();
                return createUser(eppn, name, firstName, mail, UserType.ldap, false);
            } else {
                logger.warn(mail + " not found in ldap when search by email");
            }
        } else {
            logger.warn("no ldap service available");
        }

        UserType userType = checkMailDomain(mail);
        if (userType.equals(UserType.external)) {
            logger.info("ldap user not found : " + mail + ". Creating temp account");
            return createUser(UUID.randomUUID().toString(), "", "", mail, UserType.external, false);
        } else if (userType.equals(UserType.shib)) {
            return createUser(mail, mail, "Nouvel utilisateur fédération", mail, UserType.shib, false);
        }
        logger.error("user not found with : " + mail);
        return null;
    }

    @Transactional
    public void createUserWithAuthentication(Authentication authentication) {
        String authName;
        if (authentication.getName().contains("@")) {
            authName = authentication.getName().substring(0, authentication.getName().indexOf("@"));
        } else {
            authName = authentication.getName();
        }
        logger.info("user control for " + authName);
        List<PersonLdap> personLdaps =  Objects.requireNonNull(ldapPersonService).getPersonLdap(authName);
        String eppn = personLdaps.get(0).getEduPersonPrincipalName();
        if (eppn == null) {
            eppn = buildEppn(authName);
        }
        String mail = personLdaps.get(0).getMail();
        String name = personLdaps.get(0).getSn();
        String firstName = personLdaps.get(0).getGivenName();
        createUser(eppn, name, firstName, mail, UserType.ldap, true);
    }

    @Transactional
    public User createUser(String eppn, String name, String firstName, String email, UserType userType, boolean updateCurrentUserRoles) {
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
        if(updateCurrentUserRoles) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                logger.info("Mise à jour des rôles de l'utilisateur " + eppn);
                Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
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
        if(userType.equals(UserType.shib) && globalProperties.getShibUsersDomainWhiteList() != null && globalProperties.getShibUsersDomainWhiteList().size() > 0 && globalProperties.getShibUsersDomainWhiteList().contains(user.getEppn().split("@")[1])) {
            user.getRoles().remove("ROLE_USER");
            user.getRoles().add("ROLE_OTP");
        }
        userRepository.save(user);
        return user;
    }

    @Transactional
    public void updateUser(String authUserEppn, String signImageBase64, EmailAlertFrequency emailAlertFrequency, Integer emailAlertHour, DayOfWeek emailAlertDay, MultipartFile multipartKeystore, String signRequestParamsJsonString) throws IOException {
        User authUser = getByEppn(authUserEppn);
        if(signRequestParamsJsonString != null && !signRequestParamsJsonString.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            SignRequestParams signRequestParams = objectMapper.readValue(signRequestParamsJsonString, SignRequestParams.class);
            signRequestParams.setxPos(0);
            signRequestParams.setyPos(0);
            signRequestParams.setSignWidth(300);
            signRequestParams.setSignHeight(150);
            if(authUser.getFavoriteSignRequestParams() == null) {
                signRequestParamsRepository.save(signRequestParams);
                authUser.setFavoriteSignRequestParams(signRequestParams);
            } else {
                //affectation
                authUser.getFavoriteSignRequestParams().setAddExtra(signRequestParams.getAddExtra());
                authUser.getFavoriteSignRequestParams().setAddWatermark(signRequestParams.getAddWatermark());
                authUser.getFavoriteSignRequestParams().setExtraType(signRequestParams.getExtraType());
                authUser.getFavoriteSignRequestParams().setExtraDate(signRequestParams.getExtraDate());
                authUser.getFavoriteSignRequestParams().setExtraName(signRequestParams.getExtraName());
                authUser.getFavoriteSignRequestParams().setExtraText(signRequestParams.getExtraText());
                authUser.getFavoriteSignRequestParams().setExtraOnTop(signRequestParams.getExtraOnTop());
            }
        } else {
            if(authUser.getFavoriteSignRequestParams() != null) {
                SignRequestParams signRequestParams = authUser.getFavoriteSignRequestParams();
                authUser.setFavoriteSignRequestParams(null);
                signRequestParamsRepository.delete(signRequestParams);
            }
        }
        if(multipartKeystore != null && !multipartKeystore.isEmpty() && !globalProperties.getDisableCertStorage()) {
            if(authUser.getKeystore() != null) {
                documentService.delete(authUser.getKeystore());
            }
            authUser.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), authUser.getEppn() + "_" + multipartKeystore.getOriginalFilename().split("\\.")[0] + ".p12", multipartKeystore.getContentType()));
        }
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
            authUser.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signImageBase64), authUser.getEppn() + "_sign.png", "image/png"));
            if(authUser.getSignImages().size() == 1) {
                authUser.setDefaultSignImageNumber(0);
            }
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

    public List<PersonLdapLight> getPersonLdapsLight(String searchString) {
        List<PersonLdapLight> personLdaps = new ArrayList<>();
        Set<User> users = new HashSet<>();
        users.addAll(userRepository.findByEppnStartingWith(searchString));
        users.addAll(userRepository.findByNameStartingWithIgnoreCase(searchString));
        users.addAll(userRepository.findByEmailStartingWith(searchString));
        if (ldapPersonService != null && !searchString.trim().isEmpty() && searchString.length() > 2) {
            List<PersonLdapLight> ldapSearchList = ldapPersonService.searchLight(searchString);
            if (ldapSearchList.size() > 0) {
                List<PersonLdapLight> ldapList = ldapSearchList.stream().sorted(Comparator.comparing(PersonLdapLight::getCn)).collect(Collectors.toList());
                for (PersonLdapLight personLdapList : ldapList) {
                    if (personLdapList.getMail() != null) {
                        if (personLdaps.stream().noneMatch(personLdap -> personLdap.getMail().equals(personLdapList.getMail()))) {
                            personLdaps.add(personLdapList);
                        }
                    }
                }
            }
        }
        for (User user : users) {
            if(user.getReplaceByUser() != null) {
                personLdaps.remove(personLdaps.stream().filter(personLdap -> personLdap.getMail().equals(user.getEmail())).findFirst().get());
            }
            if(personLdaps.stream().noneMatch(personLdapLight -> personLdapLight.getMail().equals(user.getEmail()))) {
                personLdaps.add(getPersonLdapLightFromUser(user));
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

    public PersonLdapLight getPersonLdapLightFromUser(User user) {
        PersonLdapLight personLdap = new PersonLdapLight();
        if(user.getReplaceByUser() != null) {
            personLdap.setUid(user.getReplaceByUser().getEppn());
            personLdap.setSn(user.getReplaceByUser().getName());
            personLdap.setGivenName(user.getReplaceByUser().getFirstname());
            personLdap.setDisplayName(user.getFirstname() + " " + user.getName() + " remplacé par " + user.getReplaceByUser().getFirstname() + " " + user.getReplaceByUser().getName());
            personLdap.setMail(user.getReplaceByUser().getEmail());
        } else {
            personLdap.setUid(user.getEppn());
            personLdap.setSn(user.getName());
            personLdap.setGivenName(user.getFirstname());
            personLdap.setDisplayName(user.getFirstname() + " " + user.getName());
            personLdap.setMail(user.getEmail());
        }
        return personLdap;
    }

    public PersonLdap findPersonLdapByUser(User user) {
        PersonLdap personLdap = null;
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdapRepository().findByEduPersonPrincipalName(user.getEppn());
            if (personLdaps.size() > 0) {
                personLdap = personLdaps.get(0);
            } else {
                personLdaps =  ldapPersonService.getPersonLdap(user.getEppn().split("@")[0]);
                if (personLdaps.size() > 0) {
                    personLdap = personLdaps.get(0);
                }
            }
        } else {
            personLdap = getPersonLdapFromUser(user);
        }
        return personLdap;
    }

    public OrganizationalUnitLdap findOrganizationalUnitLdapByPersonLdap(PersonLdap personLdap) {
        if (ldapPersonService != null) {
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
            if (domain.equals(globalProperties.getDomain()) && ldapPersonService != null) {
                return UserType.ldap;
            } else if(domain.equals(globalProperties.getDomain())) {
                return UserType.shib;
            } else if(shibProperties.getDomainsWhiteListUrl() != null) {
                InputStream whiteListFile = getDomainsWhiteList();
                if (fileService.isFileContainsText(whiteListFile, domain)) {
                    return UserType.shib;
                }
            }
        }
        return UserType.external;
    }

    public InputStream getDomainsWhiteList() {
        try {
            return fileService.getFileFromUrl(shibProperties.getDomainsWhiteListUrl());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public List<User> getTempUsersFromRecipientList(List<String> recipientsEmails) {
        List<User> tempUsers = new ArrayList<>();
        for (String recipientEmail : recipientsEmails) {
            if(recipientEmail != null) {
                if (recipientEmail.contains("*")) {
                    recipientEmail = recipientEmail.split("\\*")[1];
                }
                if (!recipientEmail.contains(globalProperties.getDomain())) {
                    User recipientUser = getUserByEmail(recipientEmail);
                    if (recipientUser.getUserType().equals(UserType.external)) {
                        tempUsers.add(recipientUser);
                    }
                }
            }
        }
        return tempUsers;
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
                    if (recipient.getUser().getUserType().equals(UserType.external) || (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && !recipient.getUser().getUserType().equals(UserType.shib) && recipient.getUser().getEppn().equals(recipient.getUser().getName()))) {
                        users.add(recipient.getUser());
                    }
                }
            }
        } else if(signRequest.getParentSignBook().getLiveWorkflow().getWorkflow() != null) {
            if (signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getWorkflowSteps().size() > 0) {
                for (WorkflowStep workflowStep : signRequest.getParentSignBook().getLiveWorkflow().getWorkflow().getWorkflowSteps()) {
                    for (User user : workflowStep.getUsers()) {
                        if (user.getUserType().equals(UserType.external) || (user.getEppn().equals(user.getEmail()) && user.getEppn().equals(user.getName()))) {
                            users.add(user);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(users);
    }


    @Transactional
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
        int test = authUser.getSignImages().indexOf(signDocument);
        if(authUser.getDefaultSignImageNumber().equals(authUser.getSignImages().indexOf(signDocument))) {
            authUser.setDefaultSignImageNumber(0);
        } else {
            authUser.setDefaultSignImageNumber(authUser.getDefaultSignImageNumber() - 1);
        }
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

    @Transactional
    public List<User> getUserWithoutCertificate(List<String> userEmails) {
        List<User> users = new ArrayList<>();
        userEmails.forEach(ue -> users.add(this.getUserByEmail(ue)));
        return users.stream().filter(u -> u.getKeystoreFileName() == null).collect(Collectors.toList());
    }

    @Transactional
    public Map<UiParams, String> getUiParams(String authUserEppn) {
        User user = getUserByEppn(authUserEppn);
        Map<UiParams, String> uiParamsStringMap = new HashMap<>(user.getUiParams());
        return uiParamsStringMap;
    }

    @Transactional
    public void setUiParams(String authUserEppn, UiParams key, String value) {
        User user = getUserByEppn(authUserEppn);
        user.getUiParams().put(key, value);
    }

    @Transactional
    public void setDefaultSignImage(String authUserEppn, int signImageNumber) {
        User user = getUserByEppn(authUserEppn);
        user.setDefaultSignImageNumber(signImageNumber);
    }

    public List<JsonExternalUserInfo> getJsonExternalUserInfos(List<String> emails, List<String> names, List<String> firstnames, List<String> phones) {
        List<JsonExternalUserInfo> externalUsersInfos = new ArrayList<>();
        if(emails != null) {
            for (int i = 0; i < emails.size(); i++) {
                JsonExternalUserInfo jsonExternalUserInfo = new JsonExternalUserInfo();
                jsonExternalUserInfo.setEmail(emails.get(i));
                jsonExternalUserInfo.setName(names.get(i));
                jsonExternalUserInfo.setFirstname(firstnames.get(i));
                if(phones.size() >= i + 1) {
                    jsonExternalUserInfo.setPhone(phones.get(i));
                }
                externalUsersInfos.add(jsonExternalUserInfo);
            }
        }
        return externalUsersInfos;
    }

    @Transactional
    public void updateRoles(String userEppn, List<String> roles) {
        User user = getUserByEppn(userEppn);
        user.getRoles().clear();
        user.getRoles().addAll(roles);
    }

    @Transactional
    public void updatePhone(String userEppn, String phone) {
        User user = getUserByEppn(userEppn);
        user.setPhone(phone);
    }

    public List<String> getAllRoles() {
        List<String> roles = new ArrayList<>(userRepository.getAllRoles());
        for(String role : webSecurityProperties.getMappingGroupsRoles().values()){
            if(!roles.contains(role)) {
                roles.add(role);
            }
        }
        return roles;
    }

    public List<User> getByManagersRoles(String role) {
        return userRepository.findByManagersRolesIn(Collections.singletonList(role));
    }

    @Transactional
    public void updateReplaceUserBy(String eppn, String[] byEmail, String beginDate, String endDate) {
        User user = getUserByEppn(eppn);
        if(user != null ) {
            if(byEmail != null) {
                User byUser = getUserByEmail(byEmail[0]);
                Date beginDateDate = null;
                Date endDateDate = null;
                if (beginDate != null && endDate != null) {
                    try {
                        beginDateDate = new SimpleDateFormat(DATE_PATTERN).parse(beginDate);
                        endDateDate = new SimpleDateFormat(DATE_PATTERN).parse(endDate);
                    } catch (ParseException e) {
                        logger.error("error on parsing dates");
                    }
                }
                user.setReplaceByUser(byUser);
                user.setReplaceBeginDate(beginDateDate);
                user.setReplaceEndDate(endDateDate);
            } else {
                user.setReplaceByUser(null);
                user.setReplaceBeginDate(null);
                user.setReplaceEndDate(null);
            }
        }
    }

    public String tryGetEppnFromLdap(Authentication auth) {
        String eppn = auth.getName();
        if(ldapPersonService != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getPersonLdap(auth.getName());
            if(personLdaps.size() > 0) {
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
                if (eppn == null) {
                    eppn = buildEppn(auth.getName());
                }
            }
        }
        return eppn;
    }

    public String getFavoriteSignRequestParamsJson(String userEppn) throws JsonProcessingException {
        User user = getUserByEppn(userEppn);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writer().writeValueAsString(user.getFavoriteSignRequestParams());
    }

    @Transactional
    public void updateUserInfos(Long id, String eppn, String name, String firstname) {
        User user = getById(id);
        user.setEppn(eppn);
        user.setName(name);
        user.setFirstname(firstname);
    }
}
