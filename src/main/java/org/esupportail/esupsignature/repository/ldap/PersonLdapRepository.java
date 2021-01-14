package org.esupportail.esupsignature.repository.ldap;

import org.esupportail.esupsignature.service.ldap.PersonLdap;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonLdapRepository extends LdapRepository<PersonLdap> {
    List<PersonLdap> findByEduPersonPrincipalName(String eppn);
    List<PersonLdap> findByMail(String mail);
    List<PersonLdap> findByUid(String uid);
    List<PersonLdap> findByUidOrEduPersonPrincipalNameOrSupannAliasLogin(String uid);
    List<PersonLdap> findByCnIgnoreCaseOrDisplayNameIgnoreCaseOrUidOrMail(String cn, String displayName, String uid, String mail);
    List<PersonLdap> findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrUidStartingWithOrMailStartingWith(String displayName, String cn, String uid, String mail);
}
