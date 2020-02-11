package org.esupportail.esupsignature.ldap;

import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.naming.Name;
import java.util.List;

@Repository
public interface OrganizationalUnitLdapRepository extends LdapRepository<OrganizationalUnitLdap> {
    List<OrganizationalUnitLdap> findBySupannCodeEntite(String supannCodeEntite);
}
