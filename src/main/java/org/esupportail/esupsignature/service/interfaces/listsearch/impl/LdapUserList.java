package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import org.apache.commons.validator.routines.EmailValidator;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserList;
import org.esupportail.esupsignature.service.ldap.LdapAliasService;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Primary
@Component
@ConditionalOnProperty({"spring.ldap.base", "ldap.search-base"})
public class LdapUserList implements UserList {

    @Resource
    private LdapGroupService ldapGroupService;

    @Resource
    private LdapAliasService ldapAliasService;

    @Override
    public String getName() {
        return "ldap";
    }

    @Override
    public List<String> getUsersEmailFromList(String listName) throws DataAccessException, EsupSignatureRuntimeException {
        List<String> userEmailsOk = new ArrayList<>();
        List<String> userEmails = ldapGroupService.getMembers(listName);
        for(String userEmail : userEmails) {
            List<String> childsUserEmails = getUsersEmailFromList(userEmail);
            if(childsUserEmails.size() > 0) {
                userEmailsOk.addAll(childsUserEmails);
            } else {
                userEmailsOk.add(userEmail);
            }
        }
        return userEmailsOk;
    }

    @Override
    public List<String> getUsersEmailFromAliases(String listName) throws DataAccessException, EsupSignatureRuntimeException {
        List<String> userEmails = new ArrayList<>();
        if(StringUtils.hasText(listName)) {
            List<AliasLdap> aliasLdaps = ldapAliasService.searchByMail(listName, true);
            if (aliasLdaps.size() > 0) {
                for (AliasLdap userEmail : aliasLdaps) {
                    if (userEmail.getRfc822MailMember().size() > 0) {
                        for (String alias : userEmail.getRfc822MailMember()) {
                            userEmails.addAll(getUsersEmailFromAliases(alias));
                        }
                    }
                }
            } else {
                if(EmailValidator.getInstance().isValid(listName)) {
                    userEmails.add(listName);
                }
            }
        }
        return userEmails;
    }

    @Override
    public List<Map.Entry<String, String>> getListOfLists(String search) {
        List<Map.Entry<String, String>> listNames= ldapGroupService.getAllGroupsStartWith(search);
        return listNames;
    }
}
