package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.entry.PersonLightLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonLightLdapRepository extends LdapRepository<PersonLightLdap> {
    List<PersonLightLdap> findByEduPersonPrincipalName(String eppn);
    List<PersonLightLdap> findByMail(String mail);
}

