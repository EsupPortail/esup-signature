package org.esupportail.esupsignature.service.list;

import java.util.List;

public interface UserList {
    public String getName();
    public List<String> getUsersEmailFromList(String listName);
}
