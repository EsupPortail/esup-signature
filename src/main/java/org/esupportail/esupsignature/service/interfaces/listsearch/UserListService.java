package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserListService {

    private static final Logger logger = LoggerFactory.getLogger(UserListService.class);

    private final List<UserList> userLists;

    private final UserRepository userRepository;

    private final LdapPersonService ldapPersonService;

    private final GlobalProperties globalProperties;

    public UserListService(@Autowired(required = false) List<UserList> userLists, UserRepository userRepository, LdapPersonService ldapPersonService, GlobalProperties globalProperties) {
        this.userLists = userLists;
        this.userRepository = userRepository;
        this.ldapPersonService = ldapPersonService;
        this.globalProperties = globalProperties;
    }

    public List<String> getUsersEmailFromList(String listName) throws DataAccessException, EsupSignatureRuntimeException {
        if(userLists != null && !userLists.isEmpty()) {
            if(listName.contains("*")) {
                listName = listName.split("\\*")[1];
            }
            if(ldapPersonService.getPersonLdapByMail(listName).isEmpty()) {
                Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(listName);
                if (optionalUser.isEmpty() || optionalUser.get().getUserType().equals(UserType.group)) {
                    Set<String> emails = new HashSet<>();
                    for (UserList userList : userLists) {
                        emails.addAll(userList.getUsersEmailFromList(listName));
                        emails.addAll(userList.getUsersEmailFromAliases(listName));
                    }
                    if (!emails.isEmpty()) {
                        return emails.stream().toList();
                    } else if (listName.contains(globalProperties.getDomain())) {
                        throw new EsupSignatureRuntimeException("no users found");
                    }
                } else {
                    logger.debug("user founded as local user : " + optionalUser.get().getEppn() + " as " + optionalUser.get().getUserType().name());
                }
            } else {
                logger.info("user founded as ldap user : " + listName);
            }
        }
        return new ArrayList<>();
    }

    public Map<String, String> getListsNames(String search) throws DataAccessException {
        if(userLists != null && !userLists.isEmpty() && search.length() > 4) {
            Map<String, String> names = new HashMap<>();
            for (UserList userList : userLists) {
                List<Map.Entry<String, String>> entries = userList.getListOfLists(search);
                for(Map.Entry<String, String> entry : entries) {
                    names.put(entry.getKey(), entry.getValue());
                }
            }
            return names;
        } else {
            return new HashMap<>();
        }
    }

}
