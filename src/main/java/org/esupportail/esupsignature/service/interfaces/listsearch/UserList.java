package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.esupportail.esupsignature.exception.EsupSignatureException;

import java.util.List;
import java.util.Map;

public interface UserList {
    String getName();
    List<String> getUsersEmailFromList(String listName) throws EsupSignatureException;
    List<Map.Entry<String, String>> getListOfLists(String search);
}
