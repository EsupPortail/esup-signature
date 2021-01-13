package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.AliasLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AliasLdapRepository extends LdapRepository<AliasLdap> {

    List<AliasLdap> findByMailAliasStartingWithOrCnStartingWithAndMailAliasNotNull(String mailAlias, String cn);
}
