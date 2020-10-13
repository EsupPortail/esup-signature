package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.OrganizationalUnitLdapRepository;
import org.esupportail.esupsignature.repository.ldap.PersonLdapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapOrganizationalUnitService {

    @Resource
    private OrganizationalUnitLdapRepository organizationalUnitLdapRepository;

    private Map<String, LdapTemplate> ldapTemplates;

    @Autowired
    public LdapOrganizationalUnitService(Map<String, LdapTemplate> ldapTemplates) {
        this.ldapTemplates = ldapTemplates;
    }

    public List<OrganizationalUnitLdap> getOrganizationalUnitLdaps(String supannCodeEntite) {
        return organizationalUnitLdapRepository.findBySupannCodeEntite(supannCodeEntite);
    }

    public OrganizationalUnitLdap getOrganizationalUnitLdap(String supannCodeEntite) {
        List<OrganizationalUnitLdap> organizationalUnitLdap = organizationalUnitLdapRepository.findBySupannCodeEntite(supannCodeEntite);
        if(organizationalUnitLdap.size() > 0) {
            return organizationalUnitLdap.get(0);
        }
        return null;
    }

}