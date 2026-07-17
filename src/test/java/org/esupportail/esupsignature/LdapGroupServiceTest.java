package org.esupportail.esupsignature;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.ConfigRepository;
import org.esupportail.esupsignature.service.ldap.LdapGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.ldap.InvalidSearchFilterException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LdapGroupServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void getGroupsOfUserAddsConfigurationContextWhenMappingFilterIsInvalid() {
        LdapTemplate ldapTemplate = mock(LdapTemplate.class);
        when(ldapTemplate.search(any(LdapQuery.class), any(ContextMapper.class)))
                .thenReturn(List.of("uid=test,ou=people,dc=univ,dc=fr"))
                .thenReturn(List.of())
                .thenThrow(new InvalidSearchFilterException(
                        new javax.naming.directory.InvalidSearchFilterException("invalid attribute description")));

        LdapProperties ldapProperties = new LdapProperties();
        ldapProperties.setSearchBase("ou=people");
        ldapProperties.setGroupSearchBase("ou=groups");
        ldapProperties.setEppnLeftPartSearchFilter("(uid={0})");
        ldapProperties.setGroupSearchFilter("(member={0})");
        ldapProperties.setMemberSearchFilter("(&(uid={0})({1}))");

        LdapGroupService service = new LdapGroupService(
                ldapProperties,
                mock(GlobalProperties.class),
                mock(ConfigRepository.class));
        ReflectionTestUtils.setField(service, "ldapTemplate", ldapTemplate);
        service.getLdapFiltersGroups().put("for.esup-signature.role.admin", "admin");

        assertThatThrownBy(() -> service.getGroupsOfUser("test"))
                .isInstanceOf(EsupSignatureRuntimeException.class)
                .hasMessageContaining("ldap.member-search-filter / ldap.mapping-filters-groups")
                .hasMessageContaining("(&(uid=test)(for.esup-signature.role.admin))")
                .hasMessageContaining("admin")
                .hasMessageContaining("non un nom de groupe ou un role");
    }
}
