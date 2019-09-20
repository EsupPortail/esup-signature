package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.ldap.PersonAttributMapper;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.ldap.filter.OrFilter;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LdapPersonService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
    private LdapTemplate ldapTemplate;
    
    private Map<String, LdapTemplate> ldapTemplates = new HashMap<String, LdapTemplate>();
    
	public void setLdapTemplate(LdapTemplate ldapTemplate) {
		this.ldapTemplate = ldapTemplate;
	}

	public void setLdapTemplates(Map<String, LdapTemplate> ldapTemplates) {
		this.ldapTemplates = ldapTemplates;
	}
	
	public List<String> getLdapTemplatesNames() {
		return new ArrayList<String>(ldapTemplates.keySet());
	}

	public List<PersonLdap> search(String searchString, String ldapTemplateName) {
		LdapTemplate ldapTemplateSelected = ldapTemplate;
		if(ldapTemplateName != null && !ldapTemplateName.isEmpty() && ldapTemplates.containsKey(ldapTemplateName)) {
			ldapTemplateSelected = ldapTemplates.get(ldapTemplateName);
		}
		if(ldapTemplateSelected != null) {
	        AndFilter filter = new AndFilter();
	        filter.and(new EqualsFilter("objectclass", "person"));
	        OrFilter orFilter = new OrFilter();
	        orFilter.or(new LikeFilter("displayName", "*" + searchString + "*"));
	        orFilter.or(new LikeFilter("cn", "*" + searchString + "*"));
	        orFilter.or(new LikeFilter("uid", "*" + searchString + "*"));
	        filter.and(orFilter);
	        
	        List<PersonLdap> results = ldapTemplateSelected.search(LdapUtils.emptyLdapName(), filter.encode(), new PersonAttributMapper());       
	        return results;
		} else {
			log.warn("No ldapTemplate found -> LdapPersonService.searchByCommonName result is empty");
			return new ArrayList<PersonLdap>();
		}
	}
	
}