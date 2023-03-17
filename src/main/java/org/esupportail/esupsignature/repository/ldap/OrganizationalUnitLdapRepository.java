package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.entry.OrganizationalUnitLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationalUnitLdapRepository extends LdapRepository<OrganizationalUnitLdap> {
    List<OrganizationalUnitLdap> findBySupannCodeEntite(String supannCodeEntite);
}
