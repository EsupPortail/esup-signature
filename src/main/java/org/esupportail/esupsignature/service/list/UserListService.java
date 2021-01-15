package org.esupportail.esupsignature.service.list;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserListService {

    @Autowired(required = false)
    UserList userList;

    public List<String> getUsersEmailFromList(String listName) {
        if(userList != null) {
            return userList.getUsersEmailFromList(listName);
        } else {
            return new ArrayList<>();
        }

    }

}
