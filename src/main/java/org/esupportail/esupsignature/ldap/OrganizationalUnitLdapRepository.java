package org.esupportail.esupsignature.ldap;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.naming.Name;
import java.util.List;

@Repository
public interface OrganizationalUnitLdapRepository extends CrudRepository<OrganizationalUnitLdap, Name> {
    List<OrganizationalUnitLdap> findBySupannCodeEntite(String supannCodeEntite);
}
