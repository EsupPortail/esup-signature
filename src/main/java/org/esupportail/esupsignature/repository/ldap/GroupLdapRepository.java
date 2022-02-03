package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.GroupLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupLdapRepository extends LdapRepository<GroupLdap> {

}
