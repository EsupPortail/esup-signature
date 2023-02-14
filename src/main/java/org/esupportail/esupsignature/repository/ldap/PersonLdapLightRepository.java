package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.PersonLdapLight;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonLdapLightRepository extends LdapRepository<PersonLdapLight> {

    @Query(value = "(&(|(displayName={0}*)(cn={0}*)(uid={0})(mail={0}*))(mail=*))", countLimit = 9)
    List<PersonLdapLight> fullTextSearch(String searchText);
}

