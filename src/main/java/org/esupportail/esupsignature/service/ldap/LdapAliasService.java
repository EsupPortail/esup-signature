package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.repository.ldap.AliasLdapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LdapAliasService {

    @Autowired(required = false)
    private AliasLdapRepository aliasLdapRepository;


    public List<AliasLdap> searchAlias(String searchString) {
        return aliasLdapRepository.findByMailAliasStartingWithOrCnStartingWithAndMailAliasNotNull(searchString, searchString);
    }
}
