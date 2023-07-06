package org.esupportail.esupsignature.service.interfaces.listsearch.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.esupportail.esupsignature.service.extdb.ExtDbService;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserList;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.sql.ResultSet;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "extdb.datasources.userListDataSource.name", havingValue = "sympa")
public class SympaUserList implements UserList {

    private JdbcTemplate jdbcTemplate;

    @Resource
    private ExtDbService extDbService;

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
        jdbcTemplate.query("select user_subscriber from subscriber_table where list_subscriber=" + "'" + listName.split("@")[0] + "'", (ResultSet rs) -> {
            userEmails.add(rs.getString("user_subscriber"));
            while (rs.next()) {
                userEmails.add(rs.getString("user_subscriber"));
            }
        });
        return userEmails;
    }

    @Override
    public List<Map.Entry<String, String>> getListOfLists(String search) {
        search = StringEscapeUtils.escapeSql(search);
        List<Map.Entry<String, String>> listNames= new ArrayList<>();
        jdbcTemplate.query("select distinct concat(name_list, '@', robot_list ) as list_name, subject_list  from list_table where searchkey_list like '%" + search + "%' or name_list like '%" + search + "%'", (ResultSet rs) -> {
            listNames.add(new AbstractMap.SimpleEntry<>(rs.getString("list_name"), rs.getString("subject_list")));
            while (rs.next()) {
                listNames.add(new AbstractMap.SimpleEntry<>(rs.getString("list_name"), rs.getString("subject_list")));
            }
        });
        return listNames;
    }
}
