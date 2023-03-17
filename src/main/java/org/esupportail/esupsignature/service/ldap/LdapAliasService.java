package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.repository.ldap.AliasLdapRepository;
import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
@EnableConfigurationProperties(LdapProperties.class)
public class LdapAliasService {

    private static final Logger logger = LoggerFactory.getLogger(LdapAliasService.class);

    @Resource
    private AliasLdapRepository aliasLdapRepository;

    public List<AliasLdap> searchByMail(String mail) {
        return aliasLdapRepository.findByMailStartingWith(mail);
    }

}