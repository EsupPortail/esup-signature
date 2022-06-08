package org.esupportail.esupsignature.service.interfaces.listsearch;

import java.util.List;

public interface UserList {
    String getName();
    List<String> getUsersEmailFromList(String listName);
    List<String> getListOfLists();
}
