package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserListService {

    private final List<UserList> userLists;

    private final UserRepository userRepository;

    private final GlobalProperties globalProperties;
    
    public UserListService(@Autowired(required = false) List<UserList> userLists, UserRepository userRepository, GlobalProperties globalProperties) {
        this.userLists = userLists;
        this.userRepository = userRepository;
        this.globalProperties = globalProperties;
    }

    public List<String> getUsersEmailFromList(String listName) throws DataAccessException, EsupSignatureRuntimeException {
        if(userLists != null && userLists.size() > 0) {
            if(listName.contains("*")) {
                listName = listName.split("\\*")[1];
            }
            Optional<User> optionalUser = userRepository.findByEmail(listName);
            if(optionalUser.isEmpty() || optionalUser.get().getUserType().equals(UserType.group)) {
                Set<String> emails = new HashSet<>();
                for (UserList userList : userLists) {
                    emails.addAll(userList.getUsersEmailFromList(listName));
                    emails.addAll(userList.getUsersEmailFromAliases(listName));
                }
                if(emails.size() >0 ) {
                    return emails.stream().toList();
                } else  if (listName.contains(globalProperties.getDomain())) {
                    throw new EsupSignatureRuntimeException("no users found");
                }
            }
        }
        return new ArrayList<>();
    }

    public Map<String, String> getListsNames(String search) throws DataAccessException {
        if(userLists != null && userLists.size() > 0 && search.length() > 4) {
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
