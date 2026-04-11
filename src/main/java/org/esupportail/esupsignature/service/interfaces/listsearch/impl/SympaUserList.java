package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import jakarta.annotation.PostConstruct;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.extdb.ExtDbService;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserList;
import org.esupportail.esupsignature.service.utils.database.LikePatternUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "extdb.datasources.userListDataSource.name", havingValue = "sympa")
public class SympaUserList implements UserList {

    private JdbcTemplate jdbcTemplate;

    private final ExtDbService extDbService;

    private final GlobalProperties globalProperties;

    public SympaUserList(ExtDbService extDbService, GlobalProperties globalProperties) {
        this.extDbService = extDbService;
        this.globalProperties = globalProperties;
    }

    @PostConstruct
    public void initJdbcTemplate() {
        this.jdbcTemplate = extDbService.getJdbcTemplateByName("userListDataSource");
    }

    @Override
    public String getName() {
        return "sympa";
    }

    @Override
    public List<String> getUsersEmailFromList(String listName) throws DataAccessException {
        List<String> userEmails = new ArrayList<>();
        if(listName.contains(globalProperties.getDomain())) {
            jdbcTemplate.query("select user_subscriber from subscriber_table where list_subscriber = ?", (ResultSet rs) -> {
                userEmails.add(rs.getString("user_subscriber"));
                while (rs.next()) {
                    userEmails.add(rs.getString("user_subscriber"));
                }
            }, listName.split("@")[0]);
            return userEmails;
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> getUsersEmailFromAliases(String listName) throws DataAccessException, EsupSignatureRuntimeException {
        return new ArrayList<>();
    }

    @Override
    public List<Map.Entry<String, String>> getListOfLists(String search) {
        List<Map.Entry<String, String>> listNames= new ArrayList<>();
        String pattern = LikePatternUtils.containsPattern(search);
        jdbcTemplate.query("select distinct concat(name_list, '@', robot_list ) as list_name, subject_list from list_table where searchkey_list like ? escape '\\' or name_list like ? escape '\\'", (ResultSet rs) -> {
            listNames.add(new AbstractMap.SimpleEntry<>(rs.getString("list_name"), rs.getString("subject_list")));
            while (rs.next()) {
                listNames.add(new AbstractMap.SimpleEntry<>(rs.getString("list_name"), rs.getString("subject_list")));
            }
        }, pattern, pattern);
        return listNames;
    }
}
