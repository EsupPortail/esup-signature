package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.OrganizationalUnitLdapRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapOrganizationalUnitService {

    @Resource
    private OrganizationalUnitLdapRepository organizationalUnitLdapRepository;

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