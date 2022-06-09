package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    public List<String> getListsNames(String search) throws DataAccessException {
        if(userLists != null && userLists.size() > 0) {
            List<String> names = new ArrayList<>();
            for (UserList userList : userLists) {
                names.addAll(userList.getListOfLists(search));
            }
            return names;
        } else {
            return new ArrayList<>();
        }

    }

}
