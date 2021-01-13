package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.repository.ldap.AliasLdapRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class LdapAliasService {

    @Resource
    private AliasLdapRepository aliasLdapRepository;


    public List<AliasLdap> searchAlias(String searchString) {
        return aliasLdapRepository.findByMailAliasStartingWithOrCnStartingWithAndMailAliasNotNull(searchString, searchString);
    }
}
