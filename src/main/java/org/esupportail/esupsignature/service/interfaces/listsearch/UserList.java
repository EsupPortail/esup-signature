package org.esupportail.esupsignature.service.interfaces.listsearch;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;

import java.util.List;
import java.util.Map;

public interface UserList {
    String getName();
    List<String> getUsersEmailFromList(String listName) throws EsupSignatureRuntimeException;
    List<Map.Entry<String, String>> getListOfLists(String search);
}
