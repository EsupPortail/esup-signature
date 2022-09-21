package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserListService {

    private final List<UserList> userLists;

    public UserListService(@Autowired(required = false) List<UserList> userLists) {
        this.userLists = userLists;
    }

    public List<String> getUsersEmailFromList(String listName) throws DataAccessException {
        if(userLists != null && userLists.size() > 0) {
            if(listName.contains("*")) {
                listName = listName.split("\\*")[1];
            }
            List<String> emails = new ArrayList<>();
            for (UserList userList : userLists) {
                emails.addAll(userList.getUsersEmailFromList(listName));
            }
            return emails;
        } else {
            return new ArrayList<>();
        }

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
