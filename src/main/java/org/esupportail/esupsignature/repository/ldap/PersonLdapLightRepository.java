package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.PersonLdapLight;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonLdapLightRepository extends LdapRepository<PersonLdapLight> {
    List<PersonLdapLight> findByEduPersonPrincipalName(String eppn);
    List<PersonLdapLight> findByMail(String mail);
}

