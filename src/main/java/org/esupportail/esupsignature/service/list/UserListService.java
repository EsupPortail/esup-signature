package org.esupportail.esupsignature.service.list;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserListService {

    @Resource
    UserList userList;

    public List<String> getUsersEmailFromList(String listName) {
        return userList.getUsersEmailFromList(listName);
    }


}
