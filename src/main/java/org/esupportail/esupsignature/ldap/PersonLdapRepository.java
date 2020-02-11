package org.esupportail.esupsignature.ldap;

import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.naming.Name;
import java.util.List;

@Repository
public interface PersonLdapRepository extends LdapRepository<PersonLdap> {
    List<PersonLdap> findByEduPersonPrincipalName(String eppn);
    List<PersonLdap> findByMail(String mail);
    List<PersonLdap> findByUid(String uid);
    List<PersonLdap> findByCnIgnoreCaseOrDisplayNameIgnoreCaseOrUidOrMail(String cn, String displayName, String uid, String mail);
}
