package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import org.esupportail.esupsignature.service.interfaces.listsearch.UserList;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Primary
@Component
@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base"})
public class LdapUserList implements UserList {

    @Resource
    private LdapGroupService ldapGroupService;

    @Override
    public String getName() {
        return "ldap";
    }

    @Override
    public List<String> getUsersEmailFromList(String listName) throws DataAccessException {
        List<String> userEmails = ldapGroupService.getMembers(listName);
        return userEmails;
    }

    @Override
    public List<Map.Entry<String, String>> getListOfLists(String search) {
        List<Map.Entry<String, String>> listNames= ldapGroupService.getAllGroups(search);
        return listNames;
    }
}
