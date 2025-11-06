package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.security.WebSecurityProperties;
import org.esupportail.esupsignature.config.security.shib.ShibProperties;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;
import org.esupportail.esupsignature.dto.json.SignRequestParamsWsDto;
import org.esupportail.esupsignature.dto.view.UserDto;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.EmailAlertFrequency;
import org.esupportail.esupsignature.entity.enums.UiParams;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.esupportail.esupsignature.service.ldap.*;
import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.hibernate.LazyInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private static final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm";

    private final GlobalProperties globalProperties;
    private final WebSecurityProperties webSecurityProperties;
    private final LdapPersonService ldapPersonService;
    private final LdapPersonLightService ldapPersonLightService;
    private final LdapAliasService ldapAliasService;
    private final LdapGroupService ldapGroupService;
    private final LdapOrganizationalUnitService ldapOrganizationalUnitService;
    private final SmsService smsService;
    private final ShibProperties shibProperties;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final DocumentService documentService;
    private final UserListService userListService;
    private final ObjectMapper objectMapper;
    private final SignRequestParamsRepository signRequestParamsRepository;

    public UserService(GlobalProperties globalProperties,
                       WebSecurityProperties webSecurityProperties,
                       @Autowired(required = false) LdapPersonService ldapPersonService,
                       @Autowired(required = false) LdapPersonLightService ldapPersonLightService,
                       @Autowired(required = false) LdapAliasService ldapAliasService,
                       @Autowired(required = false) LdapGroupService ldapGroupService,
                       @Autowired(required = false) LdapOrganizationalUnitService ldapOrganizationalUnitService,
                       @Autowired(required = false) SmsService smsService, ShibProperties shibProperties, UserRepository userRepository, FileService fileService, DocumentService documentService, UserListService userListService, ObjectMapper objectMapper, SignRequestParamsRepository signRequestParamsRepository) {
        this.globalProperties = globalProperties;
        this.webSecurityProperties = webSecurityProperties;
        this.ldapPersonService = ldapPersonService;
        this.ldapPersonLightService = ldapPersonLightService;
        this.ldapAliasService = ldapAliasService;
        this.ldapGroupService = ldapGroupService;
        this.ldapOrganizationalUnitService = ldapOrganizationalUnitService;
        this.smsService = smsService;
        this.shibProperties = shibProperties;
        this.userRepository = userRepository;
        this.fileService = fileService;
        this.documentService = documentService;
        this.userListService = userListService;
        this.objectMapper = objectMapper;
        this.signRequestParamsRepository = signRequestParamsRepository;
    }

    public User getById(Long id) {
        return userRepository.findById(id).get();
    }

    public User getByEppn(String eppn) {
        return  userRepository.findByEppn(eppn).orElse(null);
    }

    public User getByAccessToken(String accessToken) {
        return userRepository.findByAccessToken(accessToken).orElse(null);
    }

    @Transactional
    public User getSystemUser() {
        return createUser("system", globalProperties.getSystemUserName(), globalProperties.getSystemUserFirstName(), "system", UserType.system, false);
    }

    @Transactional
    public User getAnonymousUser() {
        return createUser("anonymous", "Anonyme", "Utilisateur", "anonymous", UserType.system, false);
    }

    @Transactional
    public User getCreatorUser() {
        return createUser("creator", "Createur de la demande", "", "creator", UserType.system, false);
    }

    @Transactional
    public User getSchedulerUser() {
        return createUser("scheduler", globalProperties.getSystemUserName(), globalProperties.getSystemUserFirstName(), globalProperties.getApplicationEmail(), UserType.system, false);
    }

    @Transactional
    public User getGenericUser() {
        return createUser("generic", "Utilisateur issue des favoris", "", "generic", UserType.system, false);
    }

    public List<UserDto> getAllUsersDto() {
        return userRepository.findAllUsersDto();
    }

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        userRepository.findAll().forEach(list::add);
        return list;
    }

    public List<User> getAllLdapUsers() {
        return userRepository.findAllByUserType(UserType.ldap);
    }

    @Transactional
    public User getUserByEmail(String email) {
        if(EmailValidator.getInstance().isValid(email) || email.equals("system") || email.equals("creator") || email.equals("scheduler") || email.equals("generic")) {
            Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
            return optionalUser.orElseGet(() -> createUserWithEmail(email));
        }
        return null;
    }

    public User isUserByEmailExist(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    @Transactional
    public User createGroupUserWithEmail(String email) {
        return createUser(email, email, "", email, UserType.group, false);
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Transactional
    public User getFullUserByEppn(String eppn) {
        if (eppn.equals("scheduler")) {
            return getSchedulerUser();
        }
        if (eppn.equals("creator")) {
            return getCreatorUser();
        }
        User user = getByEppn(eppn);
        if (user != null) {
            if(user.getKeystore() != null) {
                user.setKeystoreFileName(user.getKeystore().getFileName());
            }
            user.setSignImagesIds(user.getSignImages().stream().map(Document::getId).collect(Collectors.toList()));
            if (user.getDefaultSignImageNumber() == null || user.getDefaultSignImageNumber() < 0 || (user.getDefaultSignImageNumber() != 999997 && user.getDefaultSignImageNumber() >= user.getSignImages().size())) {
                user.setDefaultSignImageNumber(999998);
            }
            return user;
        }
		if(!eppn.startsWith("anonymousUser")) {
            logger.error("unable to find user : " + eppn);
		}
        return null;
    }

    public String buildEppn(String uid) {
        uid = uid.trim();
        if (uid.split("@").length == 1
                && !(uid.equalsIgnoreCase("creator") || uid.equalsIgnoreCase("system") || uid.equalsIgnoreCase("scheduler") || uid.equalsIgnoreCase("generic") )) {
            uid = uid + "@" + globalProperties.getDomain();
        }
        return uid;
    }

    @Transactional
    public User createUserWithEppn(String eppn) throws EsupSignatureUserException {
        if(eppn == null || eppn.equals("system")) {
            return getSystemUser();
        }
        User user = getByEppn(eppn);
        if (user != null && !user.getEppn().equals(getSystemUser().getEppn())) {
            return user;
        }
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getPersonLdapByEppn(eppn);
            if (!personLdaps.isEmpty()) {
                String name = personLdaps.get(0).getSn();
                String firstName = personLdaps.get(0).getGivenName();
                String mail = personLdaps.get(0).getMail();
                return createUser(eppn, name, firstName, mail, UserType.ldap, false);
            } else {
                throw new EsupSignatureUserException("ldap user not found : " + eppn);
            }
        }
        logger.error("user not found with eppn : " + eppn);
        return null;
    }

    @Transactional
    public User createUserWithEmail(String mail) {
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps = ldapPersonService.getPersonLdapByMail(mail);
            if (personLdaps.size() == 1) {
                String eppn = personLdaps.get(0).getEduPersonPrincipalName();
                if (!StringUtils.hasText(eppn)) {
                    logger.debug("eppn not found for " + mail);
                    eppn = buildEppn(personLdaps.get(0).getUid());
                } else {
                    logger.debug("eppn found " + eppn);
                }
                String name = personLdaps.get(0).getDisplayName();
                String firstName = "";
                if(StringUtils.hasText(personLdaps.get(0).getSn()) && StringUtils.hasText(personLdaps.get(0).getGivenName())) {
                    name = personLdaps.get(0).getSn();
                    firstName = personLdaps.get(0).getGivenName();
                }
                return createUser(eppn, name, firstName, mail, UserType.ldap, false);
            } else {
                logger.warn(mail + " not found or more than one result in ldap when search by email");
            }
        } else {
            logger.warn("no ldap service available");
        }

        UserType userType = checkMailDomain(mail);
        if (userType.equals(UserType.external)) {
            logger.info("ldap user not found : " + mail + ". Creating temp account");
            return createUser(UUID.randomUUID().toString(), "", "", mail, UserType.external, false);
        } else if (userType.equals(UserType.shib)) {
            if(StringUtils.hasText(shibProperties.getPrincipalRequestHeader())) {
                return createUser(mail, mail, "Nouvel utilisateur fédération", mail, UserType.shib, false);
            } else {
                logger.warn("no shib service available");
                throw new EsupSignatureRuntimeException("Impossible d'ajouter un utilisateur de la fédération car le service Shibboleth n'est pas activé");
            }
        }
        logger.warn("user not found with mail : " + mail);
        return null;
    }

    @Transactional
    public User createUserWithAuthentication(String eppn, String name, String firstName, String mail, Authentication authentication, UserType userType) {
        String authName;
        if (authentication.getName().contains("@")) {
            authName = authentication.getName().substring(0, authentication.getName().indexOf("@"));
        } else {
            authName = authentication.getName();
        }
        logger.info("user control for " + authName);
        logger.debug("authentication attributs found : " + eppn + ", " + name + ", " + firstName + ", " + mail);
        if(ldapPersonLightService != null && StringUtils.hasText(authName)) {
            List<PersonLightLdap> personLdaps = new ArrayList<>();
            if (userType.equals(UserType.ldap)) {
                personLdaps = ldapPersonLightService.getPersonLdapLight(authName);
            } else if(userType.equals(UserType.shib)) {
                personLdaps = ldapPersonLightService.getPersonLdapLightByEppn(eppn);
            }
            if (personLdaps.size() == 1) {
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
                if (!StringUtils.hasText(eppn)) {
                    logger.debug("eppn not found for " + authName);
                    eppn = buildEppn(authName);
                } else {
                    logger.debug("eppn found " + eppn);
                }
                if(StringUtils.hasText(personLdaps.get(0).getMail())) {
                    mail = personLdaps.get(0).getMail();
                }
                if(StringUtils.hasText(personLdaps.get(0).getSn())) {
                    name = personLdaps.get(0).getSn();
                }
                if(StringUtils.hasText(personLdaps.get(0).getGivenName())) {
                    firstName = personLdaps.get(0).getGivenName();
                }
                logger.debug("ldap attributs found : " + eppn + ", " + name + ", " + firstName + ", " + mail);
            } else if (!StringUtils.hasText(eppn)) {
                if (personLdaps.isEmpty()) {
                    logger.debug("no result on ldap search for " + authName);
                } else {
                    logger.debug("more than one result on ldap search for " + authName);
                }
                throw new EsupSignatureUserException("user " + authName + " not found");
            }
        }
        if(mail == null) {
            throw new EsupSignatureRuntimeException("user must have an email");
        }
        return createUser(eppn, name, firstName, mail, userType, true);
    }

    @Transactional
    public User createUser(String eppn, String name, String firstName, String email, UserType userType, boolean updateCurrentUserRoles) {
        User user;
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            Optional<User> optionalUser2 = userRepository.findByEppn(eppn);
            if (optionalUser2.isEmpty()) {
                user.setEppn(eppn);
            }
        } else {
            optionalUser = userRepository.findByEppn(eppn);
            if (optionalUser.isPresent()) {
                user = optionalUser.get();
                Optional<User> optionalUser2 = userRepository.findByEmailIgnoreCase(email);
                if (optionalUser2.isEmpty()) {
                    user.setEmail(email.toLowerCase());
                }
            } else {
                logger.info("create user with : " + eppn + ", " + name + ", " + firstName + ", " + email);
                user = new User();
                user.setKeystore(null);
                user.setEppn(eppn);
                user.setEmail(email.toLowerCase());
            }
        }
        user.setName(name);
        user.setFirstname(firstName);
        user.setUserType(userType);
        if(updateCurrentUserRoles) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                logger.info("Mise à jour des rôles de l'utilisateur " + eppn);
                Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
                if (!authorities.isEmpty()) {
                    user.getRoles().clear();
                    for (GrantedAuthority authority : authorities) {
                        if(authority.getAuthority().startsWith("ROLE_")) {
                            user.getRoles().add(authority.getAuthority());
                        }
                    }
                }
            }
        }
        if(userType.equals(UserType.shib) && globalProperties.getShibUsersDomainWhiteList() != null && !globalProperties.getShibUsersDomainWhiteList().isEmpty() && !globalProperties.getShibUsersDomainWhiteList().contains(user.getEppn().split("@")[1])) {
            user.getRoles().remove("ROLE_USER");
            user.getRoles().add("ROLE_OTP");
        }
        userRepository.save(user);
        return user;
    }

    @Transactional
    public void updateUserAndSignRequestParams(String authUserEppn, String signImageBase64, Boolean saveSignRequestParams, Boolean returnToHomeAfterSign, EmailAlertFrequency emailAlertFrequency, Integer emailAlertHour, DayOfWeek emailAlertDay, MultipartFile multipartKeystore, String signRequestParamsJsonString) throws IOException {
        SignRequestParams signRequestParams = null;
        if(BooleanUtils.isNotTrue(saveSignRequestParams)) {
            try {
                SignRequestParams signRequestParamsJson = objectMapper.readValue(signRequestParamsJsonString, SignRequestParams.class);
                signRequestParams = new SignRequestParams();
                signRequestParams.setSignWidth(signRequestParamsJson.getSignWidth());
                signRequestParams.setSignHeight(signRequestParamsJson.getSignHeight());
                signRequestParams.setxPos(signRequestParamsJson.getxPos());
                signRequestParams.setyPos(signRequestParamsJson.getyPos());
                signRequestParams.setSignImageNumber(signRequestParamsJson.getSignImageNumber());
                signRequestParams.setAddWatermark(signRequestParamsJson.getAddWatermark());
                signRequestParams.setAddExtra(signRequestParamsJson.getAddExtra());
                signRequestParams.setIsExtraText(signRequestParamsJson.getIsExtraText());
                if(signRequestParamsJson.getIsExtraText()) {
                    signRequestParams.setExtraText(" ");
                } else {
                    signRequestParams.setExtraText("");
                }
                signRequestParams.setExtraType(signRequestParamsJson.getExtraType());
                signRequestParams.setExtraName(signRequestParamsJson.getExtraName());
                signRequestParams.setExtraDate(signRequestParamsJson.getExtraDate());
                signRequestParams.setExtraOnTop(signRequestParamsJson.getExtraOnTop());
            } catch (JsonProcessingException e) {
                logger.warn("no signRequestParams returned", e);
            }
        } else {
            signRequestParams = null;
        }
        updateUser(authUserEppn, null, null, signImageBase64, emailAlertFrequency, emailAlertHour, emailAlertDay, multipartKeystore, signRequestParams, returnToHomeAfterSign);
    }

    @Transactional
    public void updateUser(String authUserEppn, String name, String firstName, String signImageBase64, EmailAlertFrequency emailAlertFrequency, Integer emailAlertHour, DayOfWeek emailAlertDay, MultipartFile multipartKeystore, SignRequestParams signRequestParams, Boolean returnToHomeAfterSign) throws IOException {
        User authUser = getByEppn(authUserEppn);
        if(StringUtils.hasText(name)) {
            authUser.setName(name);
        }
        if(StringUtils.hasText(firstName)) {
            authUser.setFirstname(firstName);
        }
        if(signRequestParams != null) {
            signRequestParams.setxPos(0);
            signRequestParams.setyPos(0);
            signRequestParamsRepository.save(signRequestParams);
        }
        authUser.setFavoriteSignRequestParams(signRequestParams);
        if(multipartKeystore != null && !multipartKeystore.isEmpty() && !globalProperties.getDisableCertStorage()) {
            if(authUser.getKeystore() != null) {
                documentService.delete(authUser.getKeystore());
            }
            authUser.setKeystore(documentService.createDocument(multipartKeystore.getInputStream(), authUser, authUser.getEppn() + "_" + multipartKeystore.getOriginalFilename().split("\\.")[0] + ".p12", multipartKeystore.getContentType()));
        }
        if(signImageBase64 != null && !signImageBase64.isEmpty()) {
            authUser.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signImageBase64), authUser, authUser.getEppn() + "_sign.png", "image/png"));
            if(authUser.getSignImages().size() == 1) {
                authUser.setDefaultSignImageNumber(999998);
            }
        }
        authUser.setEmailAlertFrequency(emailAlertFrequency);
        authUser.setEmailAlertHour(emailAlertHour);
        authUser.setEmailAlertDay(emailAlertDay);
        authUser.setReturnToHomeAfterSign(returnToHomeAfterSign);
    }

    /**
     * Vérifie si une alerte par email peut être envoyée à l'utilisateur en fonction de
     * sa fréquence d'alerte configurée, de l'heure et de la date actuelle, ainsi que
     * de la dernière date d'envoi d'une alerte.
     *
     * @param user l'utilisateur pour lequel l'état de l'alerte doit être vérifié. Cet objet
     *             contient les informations concernant la fréquence d'alerte par email,
     *             la date de dernier envoi d'alerte, ainsi que les préférences spécifiques
     *             de l'utilisateur.
     * @return true si une alerte peut être envoyée à l'utilisateur en respectant les paramètres
     *         configurés, sinon false.
     */
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
        if ((EmailAlertFrequency.hourly.equals(user.getEmailAlertFrequency()) && diff >= 1)
                || (EmailAlertFrequency.daily.equals(user.getEmailAlertFrequency()) && diff >= 24 && user.getEmailAlertHour().equals(hour))
                || (EmailAlertFrequency.weekly.equals(user.getEmailAlertFrequency()) && diff >= 168 && user.getEmailAlertDay().equals(DayOfWeek.of(calendar.get(Calendar.DAY_OF_WEEK))))) {
            return true;
        }
        return false;
    }

    @Transactional
    public List<PersonLightLdap> getPersonLdapsLight(String searchString, String authUserEppn) {
        List<PersonLightLdap> personLightLdaps = new ArrayList<>();
        Set<User> users = new HashSet<>();
        if(!searchString.trim().isEmpty() && searchString.length() > 2) {
            users.addAll(userRepository.findByEppnStartingWith(searchString));
            users.addAll(userRepository.findByNameStartingWithIgnoreCase(searchString.toUpperCase()));
            users.addAll(userRepository.findByEmailStartingWith(searchString));
            users.removeIf(user -> user.getEppn().equals("system") || user.getEppn().equals("scheduler") || (!globalProperties.getSearchForExternalUsers() && (user.getUserType().equals(UserType.external)) || user.getEmail() == null || user.getEmail().equals(searchString)));
            for (User user : users) {
                personLightLdaps.add(getPersonLdapLightFromUser(user));
            }
            if (ldapPersonLightService != null) {
                List<PersonLightLdap> ldapSearchList = ldapPersonLightService.searchLight(searchString);
                if (!ldapSearchList.isEmpty()) {
                    List<PersonLightLdap> personLdapListResult = ldapSearchList.stream().sorted(Comparator.comparing(PersonLightLdap::getCn)).toList();
                    for (PersonLightLdap personLightLdap : personLdapListResult) {
                        if (personLightLdap.getMail() != null) {
                            if (personLightLdaps.stream().noneMatch(personLdap -> personLdap != null && personLdap.getMail() != null && personLdap.getMail().equalsIgnoreCase(personLightLdap.getMail()))) {
                                if (personLightLdaps.stream().noneMatch(p -> p.getMail().equals(personLightLdap.getMail()))) {
                                    personLightLdaps.add(personLightLdap);
                                }
                            }
                        }
                    }
                }
            }
            List<PersonLightLdap> personLightLdapsToRemove = new ArrayList<>();
            List<User> personLightLdapsToAdd = new ArrayList<>();
            for(PersonLightLdap personLightLdap : personLightLdaps) {
                User user = isUserByEmailExist(personLightLdap.getMail());
                if(user != null && user.getReplaceByUser() != null) {
                    personLightLdapsToRemove.add(personLightLdap);
                    personLightLdapsToAdd.add(user);
                }
            }
            personLightLdaps.removeAll(personLightLdapsToRemove);
            personLightLdaps.removeAll(personLightLdaps.stream().filter(personLightLdap -> globalProperties.getForcedExternalsDomainList().stream().anyMatch(personLightLdap.getMail()::contains)).toList());
            for (User user : users) {
                if(user.getEppn().equals("creator")) {
                    personLightLdaps.add(getPersonLdapLightFromUser(user));
                }
                if(!personLightLdaps.isEmpty() && personLightLdaps.stream().noneMatch(personLightLdap -> personLightLdap != null && personLightLdap.getMail() != null && personLightLdap.getMail().equalsIgnoreCase(user.getEmail()))) {
                    PersonLightLdap personLightLdap = getPersonLdapLightFromUser(user);
                    if(user.getUserType().equals(UserType.group)) {
                        personLightLdap.setDisplayName(personLightLdap.getDisplayName());
                    }
                    personLightLdaps.add(personLightLdap);
                }
            }
            for(User userToAdd : personLightLdapsToAdd) {
                if(personLightLdaps.stream().noneMatch(personLightLdap -> personLightLdap.getEduPersonPrincipalName().equals(userToAdd.getEppn()))) {
                    personLightLdaps.add(getPersonLdapLightFromUser(userToAdd));
                }
            }
            for(Map.Entry<String,String> string : userListService.getListsNames(searchString).entrySet()) {
                if(personLightLdaps.stream().noneMatch(personLightLdap -> personLightLdap != null && personLightLdap.getMail() != null && personLightLdap.getMail().equals(string.getKey()))) {
                    PersonLightLdap personLightLdap = new PersonLightLdap();
                    personLightLdap.setMail(string.getKey());
                    if(string.getValue() != null) {
                        personLightLdap.setDisplayName(string.getValue());
                    } else {
                        personLightLdap.setDisplayName(string.getKey());
                    }
                    personLightLdaps.add(personLightLdap);
                }
            }
            if(ldapAliasService != null) {
                for (AliasLdap aliasLdap : ldapAliasService.searchByMail(searchString, false)) {
                    personLightLdaps.add(new PersonLightLdap(aliasLdap.getMail()));
                }
            }
            User user = getByEppn(authUserEppn);
            if(user.getRoles().contains("ROLE_ADMIN")) {
                return personLightLdaps.stream().toList();
            } else {
                return personLightLdaps.stream().filter(personLightLdap -> !webSecurityProperties.getExcludedEmails().contains(personLightLdap.getMail())).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
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

    public PersonLightLdap getPersonLdapLightFromUser(User user) {
        PersonLightLdap personLdap = new PersonLightLdap();
        User currentReplaceByUser = user.getCurrentReplaceByUser();
        if(currentReplaceByUser != null) {
            personLdap.setUid(currentReplaceByUser.getEppn());
            personLdap.setSn(currentReplaceByUser.getName());
            personLdap.setGivenName(currentReplaceByUser.getFirstname());
            personLdap.setDisplayName(user.getFirstname() + " " + user.getName() + " remplacé par " + currentReplaceByUser.getFirstname() + " " + currentReplaceByUser.getName());
            personLdap.setMail(currentReplaceByUser.getEmail());
        } else {
            personLdap.setUid(user.getEppn());
            personLdap.setSn(user.getName());
            personLdap.setGivenName(user.getFirstname());
            personLdap.setDisplayName(user.getFirstname() + " " + user.getName());
            personLdap.setMail(user.getEmail());
        }
        return personLdap;
    }

    @Cacheable(value = "ldapLightCache", key = "#user.id")
    public PersonLightLdap findPersonLdapLightByUser(User user) {
        PersonLightLdap personLdap = null;
        if (ldapPersonLightService != null) {
            List<PersonLightLdap> personLdaps =  ldapPersonLightService.getPersonLdapLightByEppn(user.getEppn());
            if (!personLdaps.isEmpty()) {
                personLdap = personLdaps.get(0);
            }
        } else {
            personLdap = getPersonLdapLightFromUser(user);
        }
        return personLdap;
    }

    public PersonLdap findPersonLdapByUser(User user) {
        PersonLdap personLdap = null;
        if (ldapPersonService != null) {
            List<PersonLdap> personLdaps =  ldapPersonService.getPersonLdapByEppn(user.getEppn());
            if (!personLdaps.isEmpty()) {
                personLdap = personLdaps.get(0);
            }
        } else {
            personLdap = getPersonLdapFromUser(user);
        }
        return personLdap;
    }

    public OrganizationalUnitLdap findOrganizationalUnitLdapByPersonLdap(PersonLdap personLdap) {
        if (ldapOrganizationalUnitService != null) {
            return ldapOrganizationalUnitService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
        }
        return null;
    }

    @Transactional
    public void disableIntro(String authUserEppn, String name) {
        User authUser = getByEppn(authUserEppn);
        authUser.getUiParams().put(UiParams.valueOf(name), "true");
    }

    @Transactional
    public List<Long> getFavoriteIds(String authUserEppn, UiParams uiParams) {
        User authUser = getByEppn(authUserEppn);
        if(authUser.getUiParams().containsKey(uiParams)) {
            return Arrays.stream(authUser.getUiParams().get(uiParams).split(",")).map(s -> {
                try {
                    return Long.valueOf(s);
                } catch (NumberFormatException e) {
                    return null;
                }
            }).filter(Objects::nonNull).toList();
        }
        return new ArrayList<>();
    }

    @Transactional
    public boolean toggleFavorite(String authUserEppn, Long id, UiParams uiParams) {
        boolean result;
        User authUser = getByEppn(authUserEppn);
        String favorites = authUser.getUiParams().get(uiParams);
        if(favorites == null || favorites.equals("null")) {
            favorites = "";
        }
        List<Long> favoritesIds = new ArrayList<>(Arrays.stream(favorites.split(","))
                .map(s -> {
                    try {
                        return Long.valueOf(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList());
        if(favoritesIds.contains(id)) {
            favoritesIds.remove(id);
            result = false;
        } else {
            favoritesIds.add(id);
            result = true;
        }
        favorites = favoritesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        authUser.getUiParams().put(uiParams, favorites);
        return result;

    }

    public UserType checkMailDomain(String email) {
        String[] emailSplit = email.split("@");
        if (emailSplit.length > 1) {
            String domain = emailSplit[1];
            if(globalProperties.getForcedExternalsDomainList() != null && !globalProperties.getForcedExternalsDomainList().isEmpty() && globalProperties.getForcedExternalsDomainList().contains(domain)) {
                return UserType.external;
            }
            if (domain.equals(globalProperties.getDomain()) && ldapPersonService != null) {
                return UserType.ldap;
            } else if(domain.equals(globalProperties.getDomain()) && StringUtils.hasText(shibProperties.getPrincipalRequestHeader())) {
                return UserType.shib;
            } else if(StringUtils.hasText(shibProperties.getDomainsWhiteListUrl()) && StringUtils.hasText(shibProperties.getPrincipalRequestHeader())) {
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
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    @Transactional
    public List<User> checkTempUsers(List<RecipientWsDto> recipients) {
        if (recipients!= null && !recipients.isEmpty()) {
            try {
                List<User> users = getTempUsersFromRecipientList(recipients);
                if (smsService != null || !globalProperties.getSmsRequired()) {
                    return users;
                } else {
                    if (!users.isEmpty()) {
                        return null;
                    }
                }
            } catch (EsupSignatureRuntimeException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        return new ArrayList<>();
    }

    @Transactional
    public List<User> getTempUsersFromRecipientList(List<RecipientWsDto> recipients) {
        List<User> tempUsers = new ArrayList<>();
        for (RecipientWsDto recipient : recipients) {
            if(recipient != null) {
                Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(recipient.getEmail());
                if(optionalUser.isPresent() && (optionalUser.get().getUserType().equals(UserType.external) || globalProperties.getForcedExternalsDomainList().stream().anyMatch(optionalUser.get().getEmail()::contains))) {
                    tempUsers.add(optionalUser.get());
                } else {
                    List<String> groupUsers = new ArrayList<>(userListService.getUsersEmailFromList(recipient.getEmail()));
                    if (groupUsers.isEmpty() && (!recipient.getEmail().contains(globalProperties.getDomain()) || globalProperties.getForcedExternalsDomainList().stream().anyMatch(recipient.getEmail()::contains))) {
                        User recipientUser = getUserByEmail(recipient.getEmail());
                        if (recipientUser != null && (recipientUser.getUserType().equals(UserType.external) || globalProperties.getForcedExternalsDomainList().stream().anyMatch(recipient.getEmail()::contains))) {
                            tempUsers.add(recipientUser);
                            recipientUser.setUserType(UserType.external);
                        }
                    }
                }
            }
        }
        return tempUsers;
    }

    @Transactional
    public List<User> getTempUsers(SignBook signBook, List<RecipientWsDto> recipients) throws EsupSignatureRuntimeException {
        Set<User> users = new HashSet<>(getTempUsers(signBook));
        if(recipients != null) {
            users.addAll(getTempUsersFromRecipientList(recipients));
        }
        return new ArrayList<>(users);
    }

    @Transactional
    public List<User> getTempUsers(SignBook signBook) {
        Set<User> users = new HashSet<>();
        if(!signBook.getLiveWorkflow().getLiveWorkflowSteps().isEmpty()) {
            for (LiveWorkflowStep liveWorkflowStep : signBook.getLiveWorkflow().getLiveWorkflowSteps()) {
                for (Recipient recipient : liveWorkflowStep.getRecipients()) {
                    if (recipient.getUser().getUserType().equals(UserType.external) || (recipient.getUser().getEppn().equals(recipient.getUser().getEmail()) && !recipient.getUser().getUserType().equals(UserType.group) && !recipient.getUser().getUserType().equals(UserType.shib) && recipient.getUser().getEppn().equals(recipient.getUser().getName()))) {
                        users.add(recipient.getUser());
                    }
                }
            }
        } else if(signBook.getLiveWorkflow().getWorkflow() != null) {
            if (!signBook.getLiveWorkflow().getWorkflow().getWorkflowSteps().isEmpty()) {
                for (WorkflowStep workflowStep : signBook.getLiveWorkflow().getWorkflow().getWorkflowSteps()) {
                    for (User user : workflowStep.getUsers()) {
                        if (user.getUserType().equals(UserType.external) || (user.getEppn().equals(user.getEmail()) && !user.getUserType().equals(UserType.group) && user.getEppn().equals(user.getName()))) {
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
            signature.put("fileName", "sign_" + signImage.get().getFileName());
            signature.put("contentType", signImage.get().getContentType());
            return signature;
        } else {
            return null;
        }
    }

    @Transactional
    public void deleteSign(String authUserEppn, long id) {
        User authUser = getByEppn(authUserEppn);
        Document signDocument = documentService.getById(id);
        int test = authUser.getSignImages().indexOf(signDocument);
        if (authUser.getDefaultSignImageNumber().equals(test)) {
            authUser.setDefaultSignImageNumber(999998);
        } else {
            if(test < authUser.getDefaultSignImageNumber()) {
                authUser.setDefaultSignImageNumber(authUser.getDefaultSignImageNumber() - 1);
            }
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
        if(!StringUtils.hasText(globalProperties.getSealCertificatPin()) && globalProperties.getSealCertificatPin().isEmpty()) {
            userEmails.forEach(ue -> users.add(this.getUserByEmail(ue)));
            return users.stream().filter(u -> u.getKeystoreFileName() == null).collect(Collectors.toList());
        } else {
            return users;
        }
    }

    @Transactional
    public Map<UiParams, String> getUiParams(String authUserEppn) {
        User user = getByEppn(authUserEppn);
        return new HashMap<>(user.getUiParams()); //new HashMap to force lazy loading
    }

    @Transactional
    public void setUiParams(String authUserEppn, UiParams key, String value) {
        User user = getByEppn(authUserEppn);
        user.getUiParams().put(key, value);
    }

    @Transactional
    public void setDefaultSignImage(String authUserEppn, int signImageNumber) {
        User user = getByEppn(authUserEppn);
        user.setDefaultSignImageNumber(signImageNumber);
    }

    @Transactional
    public void updateRoles(String userEppn, List<String> roles) {
        User user = getByEppn(userEppn);
        user.getRoles().clear();
        user.getRoles().addAll(roles);
    }

    @Transactional
    public void updatePhone(String userEppn, String phone) {
        User user = getByEppn(userEppn);
        if(globalProperties.getFrenchPhoneNumberOnly()) {
            try {
                Phonenumber.PhoneNumber phoneNumber = PhoneNumberUtil.getInstance().parse(phone, "FR");
                if(phoneNumber.getCountryCode() != 33) {
                    throw new EsupSignatureRuntimeException("Le numéro de téléphone doit être un numéro français");
                }
            } catch (NumberParseException e) {
                throw new RuntimeException(e);
            }
        }
        String phoneNormalized = PhoneNumberUtil.normalizeDiallableCharsOnly(phone);
        User checkUser = getUserByPhone(phoneNormalized);
        if(checkUser == null) {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNormalized, "FR");
                String national = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
                String digitsOnly = national.replaceAll("\\s+", "");
                checkUser = getUserByPhone(digitsOnly);
            } catch (NumberParseException e) {
                throw new EsupSignatureRuntimeException(e.getMessage());
            }
        }
        if(checkUser != null && !user.equals(checkUser)) {
            throw new EsupSignatureRuntimeException("Le numéro de téléphone est déjà présent dans la base");
        } else {
            user.setPhone(phoneNormalized);
        }
    }

    public List<String> getAllRoles() {
        List<String> roles = new ArrayList<>(userRepository.getAllRoles());
        for(String role : webSecurityProperties.getMappingGroupsRoles().values()){
            if(!roles.contains(role)) {
                roles.add(role);
            }
        }
        if(ldapGroupService != null && StringUtils.hasText(webSecurityProperties.getGroupToRoleFilterPattern())) {
            List<String> groupsNames = ldapGroupService.getAllPrefixGroups(webSecurityProperties.getGroupToRoleFilterPattern());
            for (String groupName : groupsNames) {
                Pattern pattern = Pattern.compile(webSecurityProperties.getGroupToRoleFilterPattern());
                Matcher matcher = pattern.matcher(groupName);
                if(matcher.find()) {
                    String roleName = "ROLE_" + matcher.group(1).toUpperCase();
                    if (!roles.contains(roleName)) {
                        roles.add(roleName);
                    }
                }
            }
        }
        return roles.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    public List<User> getByManagersRoles(String role) {
        return userRepository.findByManagersRolesIn(Collections.singletonList(role));
    }

    public List<User> getByManagersRolesUsers() {
        return userRepository.findByManagersRolesNotNull();
    }

    @Transactional
    public void updateReplaceUserBy(String eppn, String[] byEmail, String beginDate, String endDate) {
        User user = getByEppn(eppn);
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

    @Transactional
    public String tryGetEppnFromLdap(Authentication auth) {
        String eppn = null;
        if(ldapPersonLightService != null) {
            List<PersonLightLdap> personLdaps = ldapPersonLightService.getPersonLdapLight(auth.getName());
            if(personLdaps.size() == 1) {
                eppn = personLdaps.get(0).getEduPersonPrincipalName();
            } else {
                if (personLdaps.size() == 0) {
                    logger.debug("no result on ldap search for " + auth.getName());
                } else {
                    logger.debug("more than one result on ldap search for " + auth.getName());
                }
            }
        }
        if (!StringUtils.hasText(eppn)) {
            User user = getByEppn(auth.getName());
            if(user != null) {
                eppn = user.getEppn();
            } else {
                logger.debug("eppn not found for " + auth.getName());
                eppn = buildEppn(auth.getName());
            }
        } else {
            logger.debug("eppn found " + eppn);
        }
        return eppn;
    }

    public String getFavoriteSignRequestParamsJson(String userEppn) throws JsonProcessingException {
        User user = getByEppn(userEppn);
        return objectMapper.writer().writeValueAsString(user.getFavoriteSignRequestParams());
    }

    @Transactional
    public InputStream getDefaultImage(String eppn) throws IOException {
        User user = getByEppn(eppn);
        return fileService.getDefaultImage(user.getName(), user.getFirstname(), user.getEmail(), false);
    }

    @Transactional
    public String getDefaultImage64(String eppn) throws IOException {
        return fileService.getBase64Image(getDefaultImage(eppn), "default");
    }

    @Transactional
    public InputStream getDefaultParaphe(String eppn) throws IOException {
        User user = getByEppn(eppn);
        return fileService.getDefaultParaphe(user.getName(), user.getFirstname(), user.getEmail(), false);
    }

    @Transactional
    public String getDefaultParaphe64(String eppn) throws IOException {
        return fileService.getBase64Image(getDefaultParaphe(eppn), "default");
    }

    @Transactional
    public List<User> getGroupUsers() {
        return userRepository.findByUserType(UserType.group);
    }

    @Transactional
    public List<String> getManagersRoles(String authUserEppn) {
        User user = getByEppn(authUserEppn);
        return user.getManagersRoles().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    @Transactional
    public void anonymize(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        List<User> users = userRepository.findByReplaceByUser(user);
        for(User user1 : users) {
            user1.setReplaceByUser(user.getCurrentReplaceByUser());
        }
        user.setEppn("");
        user.setName("");
        user.setFirstname("");
        user.setEmail("");
        user.getSignImages().clear();
        user.setKeystore(null);
        user.setPhone("");
        user.getRoles().clear();
    }

    @Transactional
    public void parseRoles(String userEppn, GlobalProperties myGlobalProperties) {
        User user = getByEppn(userEppn);
        try {
            Set<String> roles = user.getRoles();
            if(!Collections.disjoint(roles, globalProperties.getHideSendSignExceptRoles()))
                myGlobalProperties.setHideSendSignRequest(!globalProperties.getHideSendSignRequest());
            if(!Collections.disjoint(roles, globalProperties.getHideWizardExceptRoles()))
                myGlobalProperties.setHideWizard(!globalProperties.getHideWizard());
            if(!Collections.disjoint(roles, globalProperties.getHideAutoSignExceptRoles()))
                myGlobalProperties.setHideAutoSign(!globalProperties.getHideAutoSign());

            if(roles.contains("ROLE_CREATE_SIGNREQUEST")) {
                myGlobalProperties.setHideSendSignRequest(false);
            }
            if(roles.contains("ROLE_CREATE_WIZARD")) {
                myGlobalProperties.setHideWizard(false);
            }
            if(roles.contains("ROLE_CREATE_AUTOSIGN")) {
                myGlobalProperties.setHideAutoSign(false);
            }
            if(roles.contains("ROLE_NO_CREATE_SIGNREQUEST")) {
                myGlobalProperties.setHideSendSignRequest(true);
            }
            if(roles.contains("ROLE_NO_CREATE_WIZARD")) {
                myGlobalProperties.setHideWizard(true);
            }
            if(roles.contains("ROLE_NO_CREATE_AUTOSIGN")) {
                myGlobalProperties.setHideAutoSign(true);
            }
        } catch(LazyInitializationException e) {
            logger.error("enable to find roles", e);
        }
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        userRepository.delete(user);
    }

    @Transactional
    public Set<String> getRoles(String userEppn) {
        User user = getByEppn(userEppn);
        return user.getRoles();
    }

    @Transactional
    public void removeKeystore(String authUserEppn) {
        User authUser = getByEppn(authUserEppn);
        authUser.setKeystore(null);
    }

    @Transactional
    public void createRole(String role, List<String> rolesManagers) {
        if(rolesManagers != null) {
            for (String mail : rolesManagers) {
                User user = getUserByEmail(mail);
                user.getManagersRoles().add(role);
            }
        } else {
            for (User user : getByManagersRoles(role)) {
                user.getManagersRoles().remove(role);
            }
        }
    }

    @Transactional
    public void updateRole(String role, List<String> rolesManagers) {
        if(rolesManagers != null) {
            for (User user : getByManagersRoles(role)) {
                if (!rolesManagers.contains(user.getEmail())) {
                    user.getManagersRoles().remove(role);
                }
            }
            for (String mail : rolesManagers) {
                User user = getUserByEmail(mail);
                user.getManagersRoles().add(role);
            }
        } else {
            for (User user : getByManagersRoles(role)) {
                user.getManagersRoles().remove(role);
            }
        }
    }

    @Transactional
    public void renewToken(String userEppn) {
        User user = getByEppn(userEppn);
        user.setAccessToken(UUID.randomUUID().toString());
    }

    @Transactional
    public List<SignRequestParams> getSignRequestParamsesFromJson(String signRequestParamsJsonString, String userEppn) {
        User user = getByEppn(userEppn);
        List<SignRequestParams> signRequestParamses = new ArrayList<>();
        try {
            signRequestParamses = new LinkedList<>(
                    Arrays.asList(objectMapper.readValue(signRequestParamsJsonString, SignRequestParams[].class))
            );
            for (SignRequestParams signRequestParams : signRequestParamses) {
                if(signRequestParams.getImageBase64() != null) {
                    try {
                        user.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signRequestParams.getImageBase64()), user, user.getEppn() + "_sign.png", "image/png"));
                        signRequestParams.setSignImageNumber(user.getSignImages().size() - 1);
                    } catch (IOException e) {
                        logger.error("error on create sign image", e);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.warn("no signRequestParams returned", e);
        }
        return signRequestParamses;
    }

    @Transactional
    public List<SignRequestParamsWsDto> getSignRequestParamsWsDtosFromJson(String signRequestParamsJsonString, String userEppn) {
        User user = getByEppn(userEppn);
        List<SignRequestParamsWsDto> signRequestParamseWsDtos = new ArrayList<>();
        try {
            signRequestParamseWsDtos = Arrays.asList(objectMapper.readValue(signRequestParamsJsonString, SignRequestParamsWsDto[].class));
            for (SignRequestParamsWsDto signRequestParamsWsDto : signRequestParamseWsDtos) {
                if(signRequestParamsWsDto.getImageBase64() != null) {
                    try {
                        user.getSignImages().add(documentService.createDocument(fileService.base64Transparence(signRequestParamsWsDto.getImageBase64()), user, user.getEppn() + "_sign.png", "image/png"));
                        signRequestParamsWsDto.setSignImageNumber(user.getSignImages().size() - 1);
                    } catch (IOException e) {
                        logger.error("error on create sign image", e);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            logger.warn("no signRequestParams returned", e);
        }
        return signRequestParamseWsDtos;
    }

}
