package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserListService {

    private final UserList userList;

    public UserListService(@Autowired(required = false) UserList userList) {
        this.userList = userList;
    }

    public List<String> getUsersEmailFromList(String listName) throws DataAccessException {
        if(userList != null) {
            if(listName.contains("*")) {
                listName = listName.split("\\*")[1];
            }
            return userList.getUsersEmailFromList(listName);
        } else {
            return new ArrayList<>();
        }

    }

}
