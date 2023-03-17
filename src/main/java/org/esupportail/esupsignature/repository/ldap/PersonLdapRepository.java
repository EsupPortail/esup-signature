package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.entry.PersonLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonLdapRepository extends LdapRepository<PersonLdap> {
    List<PersonLdap> findByEduPersonPrincipalName(String eppn);
    List<PersonLdap> findByMail(String mail);
}

