package org.esupportail.esupsignature.service.interfaces.listsearch;

import java.util.List;
import java.util.Map;

public interface UserList {
    String getName();
    List<String> getUsersEmailFromList(String listName);
    List<Map.Entry<String, String>> getListOfLists(String search);
}
